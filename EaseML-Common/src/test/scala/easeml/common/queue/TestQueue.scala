package easeml.common.queue

import easeml.common.job.Job
import org.junit.Test
import org.scalatest.junit.JUnitSuite

/**
  * Created by takun on 27/11/2017.
  */
object TestQueue extends JUnitSuite{


  def test_publish() {
    val publisher = new JobPublisher("localhost", 5672, "platform", "platform", "job")
    0 until 100 foreach {
      i =>
        println(i)
        val job = new Job("lr", "lr", Map("i" -> i))
        publisher.publish(job)
        Thread.sleep(100)
    }
    publisher.close()
  }


  def test_consume() {
    val consumer = new JobConsumer("localhost", 5672, "platform", "platform", "job")
    consumer.consume(handler = {
      job =>
        println(job.toJSON)
        Thread.sleep(5000)
    }, parall = 2)
    println("xxxxxxxxxxxxxx")
  }

  def main(args: Array[String]): Unit = {
    test_publish()
    test_consume()
  }
}
