package easeml.console.controllers

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by takun on 28/12/2017.
  */
object Forms {
  val job_summit = Form(
    tuple(
      "id" -> text,
      "data" -> text
    )
  )
}
