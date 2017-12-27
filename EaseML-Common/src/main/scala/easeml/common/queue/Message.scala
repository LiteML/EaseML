package easeml.common.queue

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._


/**
  * Created by takun on 27/12/2017.
  */
trait Message {
  def toJSON: String = Message.toJSON(this)
}

object Message {
  private implicit val formats = Serialization.formats(NoTypeHints)
  def fromJSON[M <: Message : Manifest](json:String) = read[M](json)
  def toJSON(msg:Message) = {
    write(msg)
  }
}

package object messages {

  class Job(val id: String,
            val algorithm: String,
            val params: Map[String,Any]) extends Message

  class Metrics(val id: String,
                val epoch: Int,
                val metrics: Map[String,Double]) extends Message


  class Algorithm(
                 val name: String,
                 val hyperParams: List[Algorithm.HyperParam]
                 ) extends Message

  object Algorithm {
    case class HyperParam(key: String, tpe: String, default:Any)
  }
}