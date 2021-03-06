package easeml.console.mq

import javax.inject.{Inject, Singleton}

import easeml.common.queue.{Message, MessageConsumer, MessagePublisher}
import easeml.common.queue.messages.{RegisterAlgorithmService, Metrics}
import play.api.Configuration

/**
  * Created by takun on 28/12/2017.
  */
@Singleton
class MqClient @Inject()(conf: Configuration) {

  private val MQ_HOST = "mq.host"
  private val MQ_PORT = "mq.port"
  private val MQ_USER = "mq.user"
  private val MQ_PASSWORD = "mq.password"
  private val MQ_REGISTER = "mq.queue.register"
  private val MQ_METRIC = "mq.queue.metric"

  private val JOB_PREFIX = "job_"

  def new_publisher(queue:String) = {
    new MessagePublisher(
      conf.get[String](MQ_HOST),
      conf.get[Int](MQ_PORT),
      conf.get[String](MQ_USER),
      conf.get[String](MQ_PASSWORD),
      queue
    )
  }

  def new_consumer[M <: Message: Manifest](queue:String) = {
    new MessageConsumer[M](
      conf.get[String](MQ_HOST),
      conf.get[Int](MQ_PORT),
      conf.get[String](MQ_USER),
      conf.get[String](MQ_PASSWORD),
      queue
    )
  }

  def new_register_consumer() = new_consumer[RegisterAlgorithmService](conf.get[String](MQ_REGISTER))
  def metric_consumer() = new_consumer[Metrics](conf.get[String](MQ_METRIC))

  def job_queue(name:String) = JOB_PREFIX + name
  def metric_queue = conf.get[String](MQ_METRIC)
}
