package easeml.common.queue

import easeml.common.queue.messages.{Algorithm, Job}
import org.scalatest.junit.JUnitSuite

/**
  * Created by takun on 27/11/2017.
  */
object TestQueue extends JUnitSuite{


  def test_publish() {
    val publisher = new MessagePublisher("localhost", 5672, "platform", "platform", "job")
    0 until 100 foreach {
      i =>
        println(i)
        val job = new Job("lr", "lr", Map("i" -> (i + 100)))
        publisher.publish(job)
        Thread.sleep(100)
    }
    publisher.close()
  }


  def test_consume() {
    val consumer = new MessageConsumer[Job]("localhost", 5672, "platform", "platform", "job")
    consumer.consume(handler = {
      job =>
        println(job.toJSON)
        Thread.sleep(5000)
    }, parall = 1)
  }

  def main(args: Array[String]): Unit = {
    test_publish()
    test_consume()
    val algorithm = new Algorithm("lr", List(
      Algorithm.HyperParam("epoch", "int", 10),
      Algorithm.HyperParam("learning_rate", "double", 0.1),
      Algorithm.HyperParam("loss", "option", List("loss1", "loss2"))
    ))
    val json = algorithm.toJSON
    println(json)
    val algo2 = Message.fromJSON[Algorithm](json)
    println(algo2.name)
    println(algo2.hyperParams.mkString(","))
  }
}
