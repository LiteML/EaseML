package easeml.common.queue

import easeml.common.job.Job
import org.scalatest.junit.JUnitSuite

/**
  * Created by takun on 27/11/2017.
  */
class TestQueue extends JUnitSuite{
  def test_consume() = {
    val consumer = new JobConsumer("localhost", 9999, "guest", "guest", "job")
    consumer.consume{
      job => println(job)
    }
  }

  def test_publish() = {
    val publisher = new JobPublisher("localhost", 9999, "guest", "guest", "job")
    val job = new Job("lr", "lr", null)
    publisher.publish(job)
  }
}
