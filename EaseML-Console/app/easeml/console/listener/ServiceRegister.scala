package easeml.console.listener

import javax.inject.{Inject, Singleton}

import easeml.console.mq.MqClient
import easeml.console.tables.{AlgorithmServer, Algorithms}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.inject.ApplicationLifecycle
import play.api.libs.json.Json
import slick.dbio.{FailedAction, FailureAction, SuccessAction}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by takun on 04/01/2018.
  */
@Singleton
class ServiceRegister @Inject() (appLifecycle: ApplicationLifecycle,
                                 mq: MqClient,
                                 val dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{

  def startup(): Unit = {
    println("start retister server consume .")
    import dbConfig.profile.api._
    val servers = TableQuery[AlgorithmServer]
    val algorithms = TableQuery[Algorithms]
    mq.new_register_consumer().consume({
      server =>
        servers += (0, server.name) match {
          case SuccessAction(result) =>
            val id = result.asInstanceOf[Int]
            algorithms ++= server.algorithms.map{
              algo =>
                val params = Json.stringify(Json.toJson(algo.hyperParams))
                (0, id, algo.name, params)
            } match {
              case SuccessAction(results) =>
                println("add algorithms results: " + results)
              case FailureAction(e) =>
                e.printStackTrace()
                println("add algorithms error.")
            }
          case FailureAction(e) =>
            e.printStackTrace()
        }
    })
  }

  def shutdown(): Unit = {}

  appLifecycle.addStopHook { () =>
    shutdown()
    Future.successful(())
  }

  startup()

}
