package easeml.common.queue

import easeml.common.queue.messages.{Algorithm, Job}
import org.scalatest.junit.JUnitSuite

/**
  * Created by takun on 27/11/2017.
  */
object TestQueue extends JUnitSuite{


  def test_publish() {
    val publisher = new MessagePublisher("localhost", 5672, "platform", "platform", "job")
    0 until 10 foreach {
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
    }, parall = 5)
  }

  def declare_queue() {
    val mq = new MqBase("localhost", 5672, "platform", "platform")
    mq.declare_queue("test_queue")
    mq.close()
  }

  def delete_queue() {
    val mq = new MqBase("localhost", 5672, "platform", "platform")
    println(mq.delete_queue("test_queue"))
    mq.close()
  }

  def main(args: Array[String]): Unit = {
//    delete_queue()
//    test_publish()
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
