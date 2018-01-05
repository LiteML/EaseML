package easeml.console.listener

import javax.inject.{Inject, Singleton}

import easeml.console.mq.MqClient
import easeml.console.tables.Metric
import easeml.dag.Status
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

/**
  * Created by takun on 05/01/2018.
  */
@Singleton
class MetricProcessor @Inject() (appLifecycle: ApplicationLifecycle,
                                 mq: MqClient,
                                 val dbConfigProvider: DatabaseConfigProvider)
                                (implicit ec: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile]{
  import dbConfig.profile.api._
  def startup(): Unit = {
    println("start metric consume .")
    val metrics = TableQuery[Metric]
    mq.metric_consumer().consume({
      metric =>
        if(metric.epoch == -1) {
          val state = metric.metrics.get("state") match {
            case Some(0) => Status.SUCCESSED
            case Some(_) => Status.FAILED
            case None => Status.SUCCESSED
          }
          Events.trigger(EventKeys.JOB_FINSHED, state)
        }else{
          metric.metrics.foreach{
            case (key, value) =>
              metrics.insertOrUpdate(metric.id.toInt, metric.epoch, key, value)
          }
        }
    })
  }
  startup()

}
