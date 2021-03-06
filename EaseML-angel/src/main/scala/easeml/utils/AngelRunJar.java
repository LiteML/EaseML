package easeml.utils;

import com.tencent.angel.utils.UGITools;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.tencent.angel.AppSubmitter;
import com.tencent.angel.conf.AngelConf;
import com.tencent.angel.exception.InvalidParameterException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Created by chris on 12/21/17.
 */
public class AngelRunJar {
    private static final Log LOG = LogFactory.getLog(AngelRunJar.class);
    private static String angelSysConfFile = "angel-site.xml";

    public static void submit(Configuration jobConf) throws Exception {
        if(jobConf.get(AngelConf.ANGEL_DEPLOY_MODE, "local").equalsIgnoreCase("YARN")) {
            final Configuration conf = new Configuration();
            // load hadoop configuration
            String hadoopHomePath = System.getenv("HADOOP_HOME");
            if (hadoopHomePath == null) {
                LOG.warn("HADOOP_HOME is empty.");
            } else {
                conf.addResource(new Path(hadoopHomePath + "/etc/hadoop/yarn-site.xml"));
                conf.addResource(new Path(hadoopHomePath + "/etc/hadoop/hdfs-site.xml"));
            }

            // load angel system configuration
            String angelHomePath = System.getenv("ANGEL_HOME");
            if (angelHomePath == null) {
                LOG.fatal("ANGEL_HOME is empty, please set it first");
                throw new InvalidParameterException("ANGEL_HOME is empty, please set it first");
            }
            LOG.info("angelHomePath conf path=" + angelHomePath + "/conf/" + angelSysConfFile);
            conf.addResource(new Path(angelHomePath + "/conf/" + angelSysConfFile));
            LOG.info("load system config file success");

            // load user configuration:
            // load user config file
            String jobConfFile = jobConf.get(AngelConf.ANGEL_APP_CONFIG_FILE);
            if(jobConfFile != null) {
                LOG.info("user app config file " + jobConfFile);
                conf.addResource(new Path(jobConfFile));
            } else {
                jobConfFile = conf.get(AngelConf.ANGEL_APP_CONFIG_FILE);
                if(jobConfFile != null) {
                    LOG.info("user app config file " + jobConfFile);
                    conf.addResource(new Path(jobConfFile));
                }
            }

            // load command line parameters
            Iterator<Entry<String, String>> iter = jobConf.iterator();
            Entry<String, String> entry = null;
            while(iter.hasNext()) {
                entry = iter.next();
                conf.set(entry.getKey(), entry.getValue());
            }

            // load user job resource files
            String userResourceFiles = conf.get(AngelConf.ANGEL_APP_USER_RESOURCE_FILES);
            if(userResourceFiles != null) {
                addResourceFiles(conf, userResourceFiles);
            }

            // load user job jar if it exist
            String jobJar = conf.get(AngelConf.ANGEL_JOB_JAR);
            if (jobJar != null) {
                loadJar(jobJar);
                addResourceFiles(conf, jobJar);
            }

            // Expand the environment variable
            try {
                expandEnv(conf);
            } catch (Exception x) {
                LOG.warn("expand env in configuration failed.", x);
            }

            // instance submitter class
            final String submitClassName =
                    conf.get(AngelConf.ANGEL_APP_SUBMIT_CLASS, AngelConf.DEFAULT_ANGEL_APP_SUBMIT_CLASS);
            UserGroupInformation ugi = UGITools.getCurrentUser(conf);
            ugi.doAs(new PrivilegedExceptionAction<String>() {
                @Override public String run() throws Exception {
                    AppSubmitter submmiter = null;
                    try {
                        Class<?> submitClass = Class.forName(submitClassName);
                        submmiter = (AppSubmitter) submitClass.newInstance();
                    } catch (Exception x) {
                        String message = "load submit class failed " + x.getMessage();
                        LOG.fatal(message, x);
                        throw new InvalidParameterException(message);
                    }

                    submmiter.submit(conf);
                    return "OK";
                }
            });
        } else if(jobConf.get(AngelConf.ANGEL_DEPLOY_MODE, "local").equalsIgnoreCase("LOCAL")) {
            AppSubmitter submmiter = null;
            try {
                final String submitClassName =
                        jobConf.get(AngelConf.ANGEL_APP_SUBMIT_CLASS, AngelConf.DEFAULT_ANGEL_APP_SUBMIT_CLASS);
                Class<?> submitClass = Class.forName(submitClassName);
                submmiter = (AppSubmitter) submitClass.newInstance();
            } catch (Exception x) {
                String message = "load submit class failed " + x.getMessage();
                LOG.fatal(message, x);
                throw new InvalidParameterException(message);
            }
            submmiter.submit(jobConf);
        } else {
            LOG.fatal("No Such Deploy Method");
        }
;
    }

    private static void expandEnv(Configuration conf) {
        Map<String, String> kvs = conf.getValByRegex("angel.*");
        Pattern pattern = Pattern.compile("\\$\\{[\\p{Alnum}\\p{Punct}]+?\\}");

        for (Entry<String, String> kv : kvs.entrySet()) {
            String value = kv.getValue();
            Matcher matcher = pattern.matcher(value);
            List<String> keys = new ArrayList<String>();

            while (matcher.find()) {
                String matchedStr = matcher.group();
                keys.add(matchedStr.substring(2, matchedStr.length() - 1));
            }

            int size = keys.size();
            for (int i = 0; i < size; i++) {
                String envValue = System.getenv(keys.get(i));
                if (envValue == null) {
                    LOG.warn("env " + keys.get(i) + " is null, please check.");
                    continue;
                }
                value = value.replaceAll("\\$\\{" + keys.get(i) + "\\}", envValue);
            }

            conf.set(kv.getKey(), value);
        }

        // Add default fs(local fs) for lib jars.
        String libJars = conf.get(AngelConf.ANGEL_JOB_LIBJARS);
        if (libJars != null) {
            StringBuilder sb = new StringBuilder();
            String[] jars = libJars.split(",");
            for (int i = 0; i < jars.length; i++) {
                if (new Path(jars[i]).isAbsoluteAndSchemeAuthorityNull()) {
                    sb.append("file://").append(jars[i]);
                    if (i != jars.length - 1) {
                        sb.append(",");
                    }
                } else {
                    sb.append(jars[i]);
                    if (i != jars.length - 1) {
                        sb.append(",");
                    }
                }
            }
            conf.set(AngelConf.ANGEL_JOB_LIBJARS, sb.toString());
        }
    }

    private static void addResourceFiles(Configuration conf, String fileNames)
            throws MalformedURLException {
        String[] fileNameArray = fileNames.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fileNameArray.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            URL url = new File(fileNameArray[i]).toURI().toURL();
            sb.append(url.toString());
        }

        String addJars = conf.get(AngelConf.ANGEL_JOB_LIBJARS);

        if (addJars == null || addJars.trim().isEmpty()) {
            conf.set(AngelConf.ANGEL_JOB_LIBJARS, sb.toString());
        } else {
            conf.set(AngelConf.ANGEL_JOB_LIBJARS, sb.toString() + "," + addJars);
        }
    }

    private static void loadJar(String jarFile) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<? extends URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);

            method.invoke(sysloader, new File(jarFile).toURI().toURL());

        } catch (Throwable t) {
            throw new IOException("Error, could not add URL to system classloader", t);
        }
    }





}
