package easeml.console.controllers

import easeml.common.queue.{Message, MessageConsumer}
import easeml.common.queue.messages.Job
import play.api.libs.json.Json

/**
  * Created by takun on 26/12/2017.
  */
object TestMq {
  def main(args: Array[String]): Unit = {
    val job2 = Message.fromJSON[Job]("""{"id":"lr","algorithm":"lr","params":{"i":4}}""")
    println(job2)
  }
}