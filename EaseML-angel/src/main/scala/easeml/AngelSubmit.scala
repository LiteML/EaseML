package easeml

import java.io.BufferedReader
import org.json4s._
import org.json4s.native.JsonMethods._
import easeml.common.queue.{MessageConsumer, MessagePublisher}
import org.apache.commons.logging.{Log, LogFactory}
import java.util.Properties
import easeml.common.queue.messages.Algorithm.HyperParam
import utils.json._
import easeml.common.queue.messages.{Algorithm, Job, RegisterAlgorithmService}
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
  private final val registerJson:String = "algorithms.json"
  private final val registerHost:String = properties.getProperty("register_host")
  private final val registerPort:Int = Integer.parseInt(properties.getProperty("register_port"))
  private final val registerUser:String = properties.getProperty("register_user")
  private final val registerPassword:String = properties.getProperty("register_password")
  private final val registerQueue:String = properties.getProperty("register_queue")

 private final val algorithmFile:String = "angel.properties"
  private final val algorithmMap:Map[String,String] = Source.fromFile(algorithmFile).getLines().map{f =>
    f.split("=")(0) -> f.split("=")(1)
  }.toMap[String,String]

  def main(args: Array[String]): Unit = {

    registerAlgos()

    val jobConsumer = new MessageConsumer[Job](
      consumeHost,
      consumePort,
      consumeUser,
      consumePassword,
      consumeQueue
    )

    jobConsumer.consume({
      job =>
        try{
          val algorithm: String = job.algorithm
          val jobId: String = job.id
          val jobConf = new Configuration(false)
          jobConf.set("jobId", jobId)
          jobConf.set("angel.app.submit.class", algorithmMap(algorithm))
          val confMap: Map[String, Any] = job.params
          confMap.foreach {
            case (key, value) =>
              jobConf.set(key, value.toString)
          }
          submit(jobConf)
        } catch {
          case e: Exception => e.printStackTrace()
        }
    },numThreads)
  }

  private def registerAlgos():Unit = {
    val json = Source.fromFile(registerJson).getLines().mkString
    val algosProfile = parse(json)
    val algorithms = array(algosProfile).map {algo =>
      val name = string(algo \\ "name")
      val hyperParams = array(algo \\ "hyperParams").map{obj =>
        val algoName = string(obj \\ "name")
        val tpe = string(obj \\ "tpe")
        val default = obj \\ "default" match {
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
    val registerPublish = new MessagePublisher(registerHost,
      registerPort,
      registerUser,
      registerPassword,
      registerQueue)
    registerPublish.publish(registerMessage)
    registerPublish.close()
  }
}
