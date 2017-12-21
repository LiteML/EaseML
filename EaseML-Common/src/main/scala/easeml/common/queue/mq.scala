package easeml.common.queue

import java.util.concurrent.Executors

import com.rabbitmq.client._
import easeml.common.job.Job

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
  channel.queueDeclare(queue, true, false, false, null)

  def close() = {
    channel.close()
    conn.close()
  }

  protected def _consume(handler : Array[Byte] => Unit) = {
    channel.basicConsume(queue, true, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String, envelope: Envelope,
                                  properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        handler(body)
      }
    })
  }

  protected def _publish(msg: Array[Byte]): Unit = {
    channel.basicPublish("", queue, null, msg)
  }
}

class JobConsumer(host:String,
                      port:Int,
                      user:String,
                      password:String,
                      queue:String,
                    parall:Int = 1) extends MqBase(host, port, user, password, queue) {
  def consume(handler : Job => Unit) = {
    val pool = Executors.newFixedThreadPool(parall)
    println(parall)
    0 until parall foreach{
      i =>
        val runnable = new Runnable {
          override def run(): Unit = {
            val self = this
            _consume{
              msg =>
                val msg_str = new String(msg, "utf-8")
                val job = Job.fromJSON(msg_str)
                println( Thread.currentThread().getName -> self)
                handler(job)
            }
          }
        }
        pool.submit(runnable)
    }
  }
}

class JobPublisher(host:String,
                       port:Int,
                       user:String,
                       password:String,
                       queue:String) extends MqBase(host, port, user, password, queue) {
  def publish(msg : Job) = _publish(msg.toJSON.getBytes("utf-8"))
}