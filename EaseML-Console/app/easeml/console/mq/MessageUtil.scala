package easeml.console.mq


import java.math.BigDecimal

import easeml.common.queue.messages.Algorithm.HyperParam
import play.api.libs.json._
import easeml.common.queue.messages.{Algorithm, Job, Metrics, RegisterAlgorithmService}



/**
  * Created by takun on 28/12/2017.
  */
object MessageUtil {

  val MaxPlain: BigDecimal = new BigDecimal(1e20)
  // Minimum magnitude of BigDecimal to write out as a plain string
  val MinPlain: BigDecimal = new BigDecimal(1e-10)

  def get[T](value: JsValue):T = {
    (value match {
      case JsNumber(v) =>
        // Workaround #3784: Same behaviour as if JsonGenerator were
        // configured with WRITE_BIGDECIMAL_AS_PLAIN, but forced as this
        // configuration is ignored when called from ObjectMapper.valueToTree
        val shouldWritePlain = {
          val va = v.abs
          va < MaxPlain && va > MinPlain
        }
        val stripped = v.bigDecimal.stripTrailingZeros
        val raw = if (shouldWritePlain) stripped.toPlainString else stripped.toString

        if (raw.indexOf('E') < 0 && raw.indexOf('.') < 0)
          if(v.isValidInt) v.toIntExact else v.toLongExact
        else v.toDouble
      case JsString(v) => v
      case JsBoolean(v) => v
      case JsArray(elements) => elements.map(v => get[Any](v)).toList
      case JsObject(values) => values.map {
        case (k, jv) => k -> get[Any](jv)
      }
      case JsNull => null
    }).asInstanceOf[T]
  }

  def parse_job(root:JsValue) = {
    val id = get[String]((root \ "id").get)
    val algorithm = get[String]((root \ "algorithm").get)
    val params = get[Map[String, Any]]((root \ "params").get)
    new Job(id, algorithm, params)
  }

  def parse_metric(root:JsValue) = {
    val id = get[String]((root \ "id").get)
    val epoch = get[Int]((root \ "epoch").get)
    val metrics = get[Map[String, Double]]((root \ "metric").get)
    new Metrics(id, epoch, metrics)
  }

  def parse_algorithm(root:JsValue) = {
    val name = get[String]((root \ "name").get)
    val algorithms = get[Seq[Map[String,Any]]]((root \ "hyperParams").get).map{
      algo => HyperParam(
        algo.get("name").asInstanceOf[String],
        algo.get("tpe").asInstanceOf[String],
        algo.get("default").asInstanceOf[Any]
      )
    }
    new Algorithm(name, algorithms.toList)
  }

  def parse_register(root:JsValue) = {
    val name = get[String]((root \ "name").get)
    val algorithms = (root \ "algorithms").as[JsArray].value.map(v => parse_algorithm(v))
    new RegisterAlgorithmService(name, algorithms.toList)
  }
}
