package easeml.common.queue

import java.util.concurrent.{Callable, Executors, LinkedBlockingDeque}

import com.rabbitmq.client._

/**
  * Created by takun on 20/11/2017.
  */
class MqBase(host:String,
             port:Int,
             user:String,
             password:String) {
  protected val factory = new ConnectionFactory()
  factory.setUsername(user)
  factory.setPassword(password)
  factory.setHost(host)
  factory.setPort(port)
  protected val conn = factory.newConnection
  protected val channel = conn.createChannel()

  def declare_queue(queue: String) = {
    channel.queueDeclare(queue, true, false, false, null).getMessageCount
  }

  def delete_queue(queue: String) = {
    channel.queueDelete(queue).getMessageCount
  }

  def close() = {
    channel.close()
    conn.close()
  }

  protected def _consume(queue:String, handler : Array[Byte] => Unit, parall: Int, requeue:Boolean) = {
    val pool = Executors.newFixedThreadPool(parall)
    val messages = new LinkedBlockingDeque[Array[Byte]](1)
    val ack = new LinkedBlockingDeque[Int](1)
    channel.basicQos(0, 1, false)
    // consume
    channel.basicConsume(queue, false, new DefaultConsumer(channel) {
      override def handleDelivery(consumerTag: String,
                                  envelope: Envelope,
                                  properties: AMQP.BasicProperties,
                                  body: Array[Byte]): Unit = {
        if(messages.offer(body)) {
          ack.take()
          channel.basicAck(envelope.getDeliveryTag, false)
        }else{
          channel.basicNack(envelope.getDeliveryTag, false, true)
        }
      }
    })
    0 until parall foreach {
      i =>
        val task = new Runnable {
          override def run(): Unit = {
            while (true) {
              val message = messages.take()
              ack.offer(1)
              try{
                handler(message)
              }catch {
                case e : Exception =>
                  e.printStackTrace()
                  if(requeue) {
                    println("requeue message")
                    _publish(queue, message)
                  }
              }
            }
          }
        }
        pool.submit(task)
    }
  }

  protected def _publish(queue:String, msg: Array[Byte]): Unit = {
    channel.basicPublish("", queue, null, msg)
  }
}

class MessageConsumer[M <: Message : Manifest](host:String,
                                               port:Int,
                                               user:String,
                                               password:String,
                                               queue:String) extends MqBase(host, port, user, password) {
  def consume(handler : M => Unit, parall:Int = 1, requeue:Boolean = true): Unit = {
    _consume(queue, {
      msg =>
        val msg_str = new String(msg, "utf-8")
        val job = Message.fromJSON[M](msg_str)
        handler(job)
    }, parall, requeue)
  }
}

class MessagePublisher(host:String,
                       port:Int,
                       user:String,
                       password:String,
                       queue:String) extends MqBase(host, port, user, password) {
  def publish(msg : Message) : Unit = _publish(queue, msg.toJSON.getBytes("utf-8"))
}