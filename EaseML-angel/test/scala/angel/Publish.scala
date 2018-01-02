package angel

import java.io.BufferedReader
import java.util.Properties

import easeml.common.queue.MessagePublisher
import easeml.common.queue.messages.Job

import scala.io.Source
import scala.collection.mutable
/**
  * Created by chris on 1/2/18.
  */
object Publish {
  private final val confFile:String = "config.properties"
  private final val properties = new Properties()
  private final val reader:BufferedReader = Source.fromURL(getClass.getClassLoader.getResource(confFile)).bufferedReader()
  properties.load(reader)
  //private final val numThreads:Int = properties.getProperty("threads", "1").toInt
  private final val consumeHost:String = properties.getProperty("consume_host")
  private final val consumePort:Int = Integer.parseInt(properties.getProperty("consume_port"))
  private final val consumeUser:String = properties.getProperty("consume_user")
  private final val consumePassword:String = properties.getProperty("consume_password")
  private final val consumeQueue:String = properties.getProperty("consume_queue")
  private final val confMap:mutable.Map[String,Any] = mutable.Map()

  def main(args: Array[String]): Unit = {
    // Feature number of train data
    val featureNum = 124
    // Total iteration number
    val epochNum = 10
    // Validation sample Ratio
    val vRatio = 0.1
    // Train batch number per epoch.
    val spRatio = 1.0
    // Batch number
    val batchNum = 10

    // Learning rate
    val learnRate = 1.0
    // Decay of learning rate
    val decay = 0.1
    // Regularization coefficient
    val reg = 0.02
    val rank = 5
    val vInit = 0.1

    confMap += (("ml.feature.num", featureNum))
    confMap += (("ml.epoch.num", epochNum))
    confMap += (("ml.batch.sample.ratio", spRatio))
    confMap += (("ml.validate.ratio", vRatio))
    confMap += (("ml.learn.rate", learnRate))
    confMap += (("ml.learn.decay", decay))
    confMap += (("ml.reg.l2",reg))
    confMap += (("ml.mlr.rank", rank))
    confMap += (("ml.mlr.v.init", vInit))
    confMap += (("ml.sgd.batch.num", batchNum))
    val job:Job = new Job(
      "debug",
      "MLR",
      confMap.toMap[String,Any]
    )
    val publisher = new MessagePublisher(
      consumeHost,
      consumePort,
      consumeUser,
      consumePassword,
      consumeQueue
    )
    publisher.publish(job)
    publisher.close()
  }
}
