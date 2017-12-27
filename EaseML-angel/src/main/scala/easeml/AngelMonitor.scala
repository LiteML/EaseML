package easeml

import java.io.BufferedReader

import com.tencent.angel.client.AngelClient
import com.tencent.angel.master.MasterProtocol
import com.tencent.angel.protobuf.generated.ClientMasterServiceProtos.{GetJobReportRequest, GetJobReportResponse, JobReportProto}
import java.lang.reflect.Method
import java.util.Properties

import AngelMonitor._
import easeml.common.metrics.Metrics
import easeml.common.queue.MetricsPublisher
import org.apache.commons.logging.{Log, LogFactory}

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.io.Source
/**
  * Created by chris on 12/26/17.
  */
object AngelMonitor {
  val LOG:Log = LogFactory.getLog(AngelMonitor.getClass)

  def getField(obj:AnyRef, fieldName: String): AnyRef= {
    val clazz = obj.getClass
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj)
  }

  def invokeMethod(obj:AnyRef, methodName:String, parameterTypes: Class[_]): (Any*) => Object = {
    val clazz = obj.getClass
    val method:Method = clazz.getDeclaredMethod(methodName, parameterTypes)
    method.setAccessible(true)
    (parameters:Any*) => method.invoke(obj, parameters)
  }
}

class AngelMonitor {
  private final var master:MasterProtocol = _
  private final var getJobReportReq: GetJobReportRequest = _
  private final var getJobReportReqBuilder: GetJobReportRequest.Builder = _
  private final var appId:String = _
  private final var client:AngelClient = _
  private final var updateMaster:(Any*) => Object = _
  private final var isFinished: Boolean = false
  private final var lastReport:GetJobReportResponse = _
  private final val confFile:String = "config.properties"
  private final val properties = new Properties()
  private final val reader:BufferedReader = Source.fromURL(getClass.getResource(confFile)).bufferedReader()
  properties.load(reader)
  private final val publishHost:String = properties.getProperty("publish_host")
  private final val publishPort:Int = Integer.parseInt(properties.getProperty("publish_port"))
  private final val publishUser:String = properties.getProperty("publish_user")
  private final val publishPassword:String = properties.getProperty("publish_password")
  private final val publishQueue:String = properties.getProperty("publish_queue")
  private final val metricPublish:MetricsPublisher = new MetricsPublisher(publishHost,publishPort,publishUser,publishPassword,publishQueue)


  def this(client2monitor:AngelClient){
    this()
    client = client2monitor
  }

  def call(): Unit = {

    val jobId = client.getConf.get("jobId")
    // AppId
    val getAppId:(Any*) => Object = invokeMethod(client,"getAppId",null)
    appId = getAppId().asInstanceOf[String]
    //updateMaster
    updateMaster = invokeMethod(client,"updateMaster",java.lang.Integer.TYPE)
    //get the master
    master = getField(client,"master").asInstanceOf[MasterProtocol]

    Future {
      try {
        while(!isFinished){
          publishJobReport(metricPublish.publish,jobId)
          this.synchronized {
            this.wait(1000)
          }
        }
      } catch {
        case e:Exception => e.printStackTrace()
      }
    }
  }

  private def publishJobReport(publish:Metrics => Unit, jobId:String): Unit = {
    val getJobRequest: GetJobReportRequest = getGetJobReportRequest()
    var response:GetJobReportResponse = null
    try{
      response = master.getJobReport(null, getJobRequest)
    }catch {
      case e: Exception =>
        LOG.error("getJobReport from master failed. " + e.getMessage)
        try {
          updateMaster(10*60)
          if (master != null) response = master.getJobReport(null, getJobRequest)
        } catch {
          case e1: Exception =>
            LOG.error("update master failed.", e1)
        }
    }
    isFinished = getField(client,"isFinished").asInstanceOf[Boolean]
    if(isFinished) {
      lastReport = null
      return
    }
    val report: JobReportProto = response.getJobReport
    if (lastReport == null || (report.hasCurIteration && report.getCurIteration != lastReport.getJobReport.getCurIteration)) {
      val epoch = report.getCurIteration
      var metricTable = report.getMetricsList.map{f =>
        f.getKey -> f.getValue.toDouble
      }.toMap[String,Double]
      if(report.hasLoss) {
        metricTable.+= ("loss" -> report.getLoss.toDouble)
        metricTable.+= ("success" -> report.getSuccess.toDouble)
      }
      val metrics = new Metrics(jobId,epoch,metricTable)
      publish(metrics)
    }
    lastReport = response
  }

  private def getGetJobReportRequest(): GetJobReportRequest= {
    if (getJobReportReq != null)
      return getJobReportReq
    if (getJobReportReqBuilder == null)
      getJobReportReqBuilder = GetJobReportRequest.newBuilder
    getJobReportReqBuilder.setAppId(appId)
    getJobReportReqBuilder.build
  }
}
