package easeml.console.controllers

import javax.inject.{Inject, Singleton}

import easeml.console.mq.{MessageUtil, MqClient}
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request}
import play.api.libs.json.Json

@Singleton
class JobController @Inject()(cc: ControllerComponents, mq:MqClient) extends AbstractController(cc) {

  def summit() = Action { implicit request: Request[AnyContent] =>
    val (id, data) = Forms.job_summit.bindFromRequest().get
    val job_publisher = mq.new_publisher(mq.job_queue(id))
    val json = Json.parse(data)
    val job = MessageUtil.parse_job(json)
    job_publisher.publish(job)
    job_publisher.close()
    val result = Json.toJson(Map("status" -> 1))
    Ok(result)
  }
}
