package easeml.common.queue

import easeml.common.job.Job

/**
  * Created by takun on 20/11/2017.
  */
class RabbitMq {
  def get:Job = {
    val job = new Job("test_job", "lr", Map[String,Any]("learningRate", 0.01))
    job
  }
}
