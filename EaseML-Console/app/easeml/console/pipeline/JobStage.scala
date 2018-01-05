package easeml.console.pipeline

import easeml.common.queue.messages.Job
import easeml.console.mq.MqClient
import easeml.dag.BaseStage

/**
  * Created by takun on 05/01/2018.
  */
class JobStage(name:String, mq: MqClient, job:Job, server:String) extends BaseStage{
  override def getName: String = name

  override def process(obj: AnyRef*): AnyRef = {
    val job_publisher = mq.new_publisher(mq.job_queue(server))
    job_publisher.publish(job)
    job_publisher.close()
    NONE
  }
}
