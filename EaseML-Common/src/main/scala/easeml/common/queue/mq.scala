package easeml.common.queue

import com.rabbitmq.client.ConnectionFactory
import easeml.common.job.Job

/**
  * Created by takun on 20/11/2017.
  */
abstract class MqBase(host:String,
             port:Int,
             user:String,
             password:String) {
  val factory = new ConnectionFactory()
  factory.setUsername(user)
  factory.setPassword(password)
  factory.setHost(host)
  factory.setPort(port)

  def conn = factory.newConnection

  def get:Job = {
    val job = new Job("test_job", "lr", Map[String,Any]("learningRate" -> 0.01))
    job
  }
}