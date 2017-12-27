package easeml.console.controllers

import easeml.common.queue.MessageConsumer
import easeml.common.queue.messages.Job

/**
  * Created by takun on 26/12/2017.
  */
object TestMq {
  def main(args: Array[String]): Unit = {
    val consumer = new MessageConsumer[Job]("localhost", 5672, "platform", "platform", "job")
    consumer.consume(handler = {
      job =>
        println(job.toJSON)
        Thread.sleep(5000)
    }, parall = 2)
    println("xxxxxxxxxxxxxx")
  }
}