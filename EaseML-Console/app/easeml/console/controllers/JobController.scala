package easeml.console.controllers

import javax.inject._

import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.BodyParser


@Singleton
class JobController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

//  @BodyParser.Of(classOf[BodyParser.Json])
  def summit() = Action { implicit request: Request[AnyContent] =>
    val result = Json.toJson(Map("status" -> 1))
    Ok(result)
  }
}
