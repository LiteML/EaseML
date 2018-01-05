package easeml.console.listener

import java.util.concurrent.{Executors, LinkedBlockingDeque}

import scala.collection.mutable

/**
  * Created by takun on 05/01/2018.
  */
trait CallBack {
  def name: String
  def once: Boolean
  def apply(data:Any): Unit
}

object EventKeys {
  val JOB_FINSHED = "JOB_FINSHED"
}
object Events {

  private val listeners = mutable.Map[String, mutable.Map[String, CallBack]]()
  private val events = new LinkedBlockingDeque[(String, Any)]()
  def add_listener(key:String, callback:CallBack): Unit = {
    listeners.synchronized {
      listeners.get(key) match {
        case Some(callbacks) => callbacks += callback.name -> callback
        case None => listeners += key -> mutable.Map(callback.name -> callback)
      }
    }
  }

  def remove_listener(key:String, name: String): Unit = {
    listeners.synchronized {
      listeners.get(key) match {
        case Some(callbacks) =>
          callbacks -= name
          if(callbacks.isEmpty) listeners -= key
        case None =>
      }
    }
  }

  def trigger(key: String, data:Any) = {
    events.offer(key -> data)
  }

  def start(thread_num: Int): Unit = {
    val pool = Executors.newFixedThreadPool(thread_num)
    pool.submit(new Runnable {
      override def run(): Unit = {
        val (key, data) = events.take()
        listeners.get(key) match {
          case Some(callbacks) => callbacks.foreach{
            case (_, callback) =>
              val task = new Runnable {
                override def run(): Unit = {
                  callback(data)
                  if(callback.once) remove_listener(key, callback.name)
                }
              }
              pool.submit(task)
          }
          case None =>
        }
      }
    })
  }
}
