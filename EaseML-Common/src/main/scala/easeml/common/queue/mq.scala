package easeml.common.queue

import java.util.concurrent.Executors

import com.rabbitmq.client._
import easeml.common.job.Job
import easeml.common.metrics.Metrics

/**
  * Created by takun on 20/11/2017.
  */
private [queue] abstract class MqBase(host:String,
             port:Int,
             user:String,
             password:String,
             queue:String) {
  protected val factory = new ConnectionFactory()
  factory.setUsername(user)
  factory.setPassword(password)
  factory.setHost(host)
  factory.setPort(port)
  protected val conn = factory.newConnection
  protected val channel = conn.createChannel()

  def close() = {
    channel.close()
    conn.close()
  }

  protected def _consume(handler : Array[Byte] => Unit, parall:Int = 1) = {
    val pool = Executors.newFixedThreadPool(parall)
    0 until parall foreach {
      i =>
        val runnable = new Runnable {
          override def run(): Unit = {
            while(true){
              val msg = channel.basicGet(queue, true).getBody
              handler(msg)
            }
          }
        }
        pool.submit(runnable)
    }
  }

  protected def _publish(msg: Array[Byte]): Unit = {
    channel.basicPublish("", queue, null, msg)
  }
}

class JobConsumer(host:String,
                      port:Int,
                      user:String,
                      password:String,
                      queue:String) extends MqBase(host, port, user, password, queue) {
  def consume(handler : Job => Unit, parall:Int = 1) = {
    _consume({
      msg =>
        val msg_str = new String(msg, "utf-8")
        val job = Job.fromJSON(msg_str)
        handler(job)
    }, parall)
  }
}

class JobPublisher(host:String,
                       port:Int,
                       user:String,
                       password:String,
                       queue:String) extends MqBase(host, port, user, password, queue) {
  def publish(msg : Job) = _publish(msg.toJSON.getBytes("utf-8"))
}


class MetricsConsumer(host:String,
                  port:Int,
                  user:String,
                  password:String,
                  queue:String) extends MqBase(host, port, user, password, queue) {
  def consume(handler : Metrics => Unit, parall:Int = 1) = {
    _consume({
      msg =>
        val msg_str = new String(msg, "utf-8")
        val metrics = Metrics.fromJSON(msg_str)
        handler(metrics)
    }, parall)
  }
}

class MetricsPublisher(host:String,
                   port:Int,
                   user:String,
                   password:String,
                   queue:String) extends MqBase(host, port, user, password, queue) {
  def publish(msg : Metrics) = _publish(msg.toJSON.getBytes("utf-8"))
}