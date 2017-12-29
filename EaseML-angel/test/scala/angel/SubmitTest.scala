package angel
import org.junit._
import java.io.BufferedReader

import easeml.common.queue.MessageConsumer
import org.apache.commons.logging.{Log, LogFactory}
import java.util.Properties

import easeml.common.queue.messages.Job
import easeml.utils.AngelRunJar._
import org.apache.hadoop.conf.Configuration

import scala.io.Source
/**
  * Created by chris on 12/29/17.
  */
object SubmitTest{

}
class SubmitTest {
  private val LOG:Log = LogFactory.getLog(SubmitTest.getClass)
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

  private final val algorithmFile:String = "angel.properties"
  private final val algorithmMap:Map[String,String] = Source.fromFile(algorithmFile).getLines().map{f =>
    f.split("=")(0) -> f.split("=")(1)
  }.toMap[String,String]

  @Test
  def testSubmit():Unit = {
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
}
