package easeml.common.queue

import easeml.common.job.Job
import org.junit.Test
import org.scalatest.junit.JUnitSuite

/**
  * Created by takun on 27/11/2017.
  */
object TestQueue extends JUnitSuite{


  def test_publish() {
    0 until 2 foreach {
      i =>
        val publisher = new JobPublisher("localhost", 5672, "platform", "platform", "job")
        val job = new Job("lr", "lr", Map())
        publisher.publish(job)
    }
  }


  def test_consume() {
    val consumer = new JobConsumer("172.30.113.253", 5672, "platform", "platform", "job", 2)
    consumer.consume{
      job =>
        println(job.toJSON)
    }
    println("xxxxxxxxxxxxxx")
  }

  def main(args: Array[String]): Unit = {
    //test_publish()
    test_consume()
  }
}
