package easeml

import java.io.BufferedReader
import easeml.common.queue.JobConsumer
import org.apache.commons.logging.{Log, LogFactory}
import java.util.Properties
import utils.AngelRunJar.submit
import org.apache.hadoop.conf.Configuration

import scala.io.Source

/**
  * Created by chris on 11/20/17.
  */
object AngelSubmit{

  private final val LOG:Log = LogFactory.getLog(AngelSubmit.getClass)

  /**
    * Basic Configurations
    */
  private final val confFile:String = "config.properties"
  private final val properties = new Properties()
  private final val reader:BufferedReader = Source.fromURL(getClass.getResource(confFile)).bufferedReader()
  properties.load(reader)
  private final val numThreads:Int = properties.getProperty("threads", "1").toInt
  private final val consumeHost:String = properties.getProperty("consume_host")
  private final val consumePort:Int = Integer.parseInt(properties.getProperty("consume_port"))
  private final val consumeUser:String = properties.getProperty("consume_user")
  private final val consumePassword:String = properties.getProperty("consume_password")
  private final val consumeQueue:String = properties.getProperty("consume_queue")
 /* private final val argsFile:String = "angel.properties"
  private final val argsMap:Map[String,String] = Source.fromFile(argsFile).getLines().map{f =>
    f.split("=")(0) -> f.split("=")(1)
  }.toMap[String,String]*/

  def main(args: Array[String]): Unit = {
    val jobConsumer = new JobConsumer(
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
        val confMap: Map[String, Any] = job.params
        confMap.foreach {
          case (key, value) =>
            try {
              jobConf.set(key, value.toString)
            } catch {
              case e: Exception => LOG.fatal(s"No Such Key $key")
            }
        }
        submit(jobConf)
    },numThreads)
  }
}
