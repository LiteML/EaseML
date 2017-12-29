package easeml.common.queue

import java.util.concurrent.Executors

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

  protected def _consume(queue:String, handler : Array[Byte] => Unit, parall:Int = 1) = {
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

  protected def _publish(queue:String, msg: Array[Byte]): Unit = {
    channel.basicPublish("", queue, null, msg)
  }
}

class MessageConsumer[M <: Message : Manifest](host:String,
                                               port:Int,
                                               user:String,
                                               password:String,
                                               queue:String) extends MqBase(host, port, user, password) {
  def consume(handler : M => Unit, parall:Int = 1) = {
    _consume(queue, {
      msg =>
        val msg_str = new String(msg, "utf-8")
        val job = Message.fromJSON[M](msg_str)
        handler(job)
    }, parall)
  }
}

class MessagePublisher(host:String,
                       port:Int,
                       user:String,
                       password:String,
                       queue:String) extends MqBase(host, port, user, password) {
  def publish(msg : Message) = _publish(queue, msg.toJSON.getBytes("utf-8"))
}