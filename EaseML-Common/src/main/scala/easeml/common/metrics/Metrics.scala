package easeml.common.metrics

import org.json4s.NoTypeHints
import org.json4s.native.Serialization
import org.json4s.native.Serialization._

/**
  * Created by takun on 26/12/2017.
  */
class Metrics(val id:String,
              val epoch:Int,
              val metrics:Map[String,Double]) {
  def toJSON = Metrics.toJSON(this)
}

object Metrics {
  private implicit val formats = Serialization.formats(NoTypeHints)

  def fromJSON(json:String) = {
    read[Metrics](json)
  }

  def toJSON(metrics:Metrics) = {
    write(metrics)
  }
}