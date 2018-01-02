package easeml

import java.io.BufferedReader
import java.util.Properties

import com.tencent.angel.conf.AngelConf
import com.tencent.angel.ml.conf.MLConf
import easeml.common.queue.messages.Algorithm.HyperParam
import easeml.common.queue.messages.{Algorithm, Job, RegisterAlgorithmService}
import easeml.common.queue.{MessageConsumer, MessagePublisher}
import easeml.utils.AngelRunJar._
import easeml.utils.json.{array, string}
import org.apache.commons.logging.{Log, LogFactory}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.mapreduce.lib.input.CombineTextInputFormat
import org.json4s.JsonAST._
import org.json4s.native.JsonMethods.parse

import scala.io.Source
/**
  * Created by chris on 12/29/17.
  */
object SubmitTest {
  private val LOG:Log = LogFactory.getLog(SubmitTest.getClass)
  /**
    * Basic Configurations
    */
  private final val confFile:String = "config.properties"
  private final val properties = new Properties()
  private final val reader:BufferedReader = Source.fromURL(getClass.getClassLoader.getResource(confFile)).bufferedReader()
  properties.load(reader)
  private final val numThreads:Int = properties.getProperty("threads", "1").toInt
  private final val consumeHost:String = properties.getProperty("consume_host")
  private final val consumePort:Int = Integer.parseInt(properties.getProperty("consume_port"))
  private final val consumeUser:String = properties.getProperty("consume_user")
  private final val consumePassword:String = properties.getProperty("consume_password")
  private final val consumeQueue:String = properties.getProperty("consume_queue")
  private final val registerJson:String = "algorithms.json"
  private final val registerHost:String = properties.getProperty("register_host")
  private final val registerPort:Int = Integer.parseInt(properties.getProperty("register_port"))
  private final val registerUser:String = properties.getProperty("register_user")
  private final val registerPassword:String = properties.getProperty("register_password")
  private final val registerQueue:String = properties.getProperty("register_queue")


  private final val algorithmFile:String = "angel.properties"
  private final val algorithmMap:Map[String,String] = Source.fromURL(getClass.getClassLoader.getResource(algorithmFile)).getLines().map{f =>
    f.split("=")(0) -> f.split("=")(1)
  }.toMap[String,String]

  private val LOCAL_FS = FileSystem.DEFAULT_FS
  private val TMP_PATH = System.getProperty("java.io.tmpdir", "/tmp")
  def main(args: Array[String]): Unit = {
    // registerAlgos()

    val jobConsumer = new MessageConsumer[Job](
      consumeHost,
      consumePort,
      consumeUser,
      consumePassword,
      consumeQueue
    )

    jobConsumer.consume({
      job =>
        val algorithm: String = job.algorithm
        val jobId: String = job.id
        val jobConf = new Configuration(false)
        jobConf.set("jobId", jobId)
        jobConf.set("angel.app.submit.class", algorithmMap(algorithm))
        jobConf.set("angel.deploy.mode","LOCAL")
        jobConf.setBoolean("mapred.mapper.new-api", true)
        jobConf.set(AngelConf.ANGEL_INPUTFORMAT_CLASS, classOf[CombineTextInputFormat].getName)
        jobConf.setBoolean(AngelConf.ANGEL_JOB_OUTPUT_PATH_DELETEONEXIST, true)
        jobConf.setInt(AngelConf.ANGEL_WORKERGROUP_NUMBER, 1)
        jobConf.setInt(AngelConf.ANGEL_WORKER_TASK_NUMBER, 1)
        jobConf.setInt(AngelConf.ANGEL_PS_NUMBER, 1)
        jobConf.set(MLConf.ML_DATA_FORMAT, "libsvm")
        val inputPath = "./EaseML-angel/src/test/data/lr/a9a.train"
        val savePath = LOCAL_FS + TMP_PATH + "/model"
        val logPath = LOCAL_FS + TMP_PATH + "/MLRlog"
        // Set trainning data path
        jobConf.set(AngelConf.ANGEL_TRAIN_DATA_PATH, inputPath)
        // Set save model path
        jobConf.set(AngelConf.ANGEL_SAVE_MODEL_PATH, savePath)
        // Set log path
        jobConf.set(AngelConf.ANGEL_LOG_PATH, logPath)
        // Set actionType train
        jobConf.set(AngelConf.ANGEL_ACTION_TYPE, MLConf.ANGEL_ML_TRAIN)

        val confMap: Map[String, Any] = job.params
        confMap.foreach {
          case (key, value) =>
            jobConf.set(key, value.toString)
        }
        submit(jobConf)
    },numThreads)
  }


  private def registerAlgos():Unit = {
    val json = Source.fromURL(getClass.getClassLoader.getResource(registerJson)).getLines().mkString
    val algosProfile = parse(json)
    val algorithms = array(algosProfile).map {algo =>
      val name = string(algo \ "name")
      val hyperParams = array(algo \ "hyperParams").map{obj =>
        val algoName = string(obj \ "name")
        val tpe = string(obj \ "tpe")
        val default = obj \ "default" match {
          case JArray(arr) => arr
          case JDouble(v) => v
          case JInt(v) => v.toInt
          case JBool(v) => v
          case JString(v) => v
        }
        HyperParam(algoName,tpe,default)
      }
      new Algorithm(name, hyperParams)
    }

    val registerMessage = new RegisterAlgorithmService("angel", algorithms)
    val registerPublish = new MessagePublisher(
      registerHost,
      registerPort,
      registerUser,
      registerPassword,
      registerQueue)
    registerPublish.publish(registerMessage)
    // registerPublish.close()
  }

}

