package easeml.console.listener

import javax.inject.{Inject, Singleton}

import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

/**
  * Created by takun on 04/01/2018.
  */
@Singleton
class ServiceRegister @Inject() (appLifecycle: ApplicationLifecycle) {

  def startup(): Unit = {
    println("start .......")
  }

  def shutdown(): Unit = {
    println("shutdown .......")
  }

  appLifecycle.addStopHook { () =>
    shutdown()
    Future.successful(())
  }

  startup()

}
