package easeml.common.job

import org.json4s._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization

/**
  * Created by takun on 20/11/2017.
  */
class Job(val id:String,
          val algorithm:String,
          val params:Map[String,Any]) {

  def toJSON = {
    implicit val formats = Serialization.formats(NoTypeHints)
    //
  }
}
