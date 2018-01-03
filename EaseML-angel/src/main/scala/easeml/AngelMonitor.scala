package easeml

import java.io.BufferedReader

import com.tencent.angel.client.AngelClient
import com.tencent.angel.master.MasterProtocol
import com.tencent.angel.protobuf.generated.ClientMasterServiceProtos.{GetJobReportRequest, GetJobReportResponse, JobReportProto}
import java.lang.reflect.Method
import java.util.Properties

import AngelMonitor._
import com.tencent.angel.ml.Monitor
import easeml.common.queue.messages.Metrics
import easeml.common.queue.MessagePublisher
import org.apache.commons.logging.{Log, LogFactory}

import scala.collection.JavaConversions._
import scala.concurrent.Future
/**
  * Created by chris on 12/26/17.
  */
object AngelMonitor {
  val LOG:Log = LogFactory.getLog(AngelMonitor.getClass)


  def getField(obj:AnyRef, fieldName: String): AnyRef = {
    val clazz = obj.getClass
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj)
  }

  def getSuperField(obj:AnyRef, fieldName:String): AnyRef = {
    val clazz = obj.getClass.getSuperclass
    val field = clazz.getDeclaredField(fieldName)
    field.setAccessible(true)
    field.get(obj)
  }
  def invokeMethod(obj:AnyRef,methodName:String) = {
    val clazz = obj.getClass
    val method:Method = clazz.getDeclaredMethod(methodName)
    method.setAccessible(true)
    method.invoke(obj)
  }

  def invokeMethod(obj:AnyRef, methodName:String, parameterTypes: java.lang.Class[_]*)(parameters:AnyRef*) = {
    val clazz = obj.getClass
    val method:Method = clazz.getDeclaredMethod(methodName, parameterTypes:_*)
    method.setAccessible(true)
    method.invoke(obj, parameters:_*)
  }

}

class AngelMonitor extends Monitor{
  private final var master:MasterProtocol = _
  private final var getJobReportReq: GetJobReportRequest = _
  private final var getJobReportReqBuilder: GetJobReportRequest.Builder = _
  private final var appId:String = _
  private final var client:AngelClient = _
  //private final var updateMaster: (AnyRef) => Object = _
  private final var isFinished: Boolean = false
  private final var lastReport:GetJobReportResponse = _
  private final val confFile:String = "config.properties"
  private final var appFailedMessage:String = _
  private final val properties = {
    val p = new Properties()
    //    val reader = getClass.getClassLoader.getResourceAsStream(confFile)
    val reader = Thread.currentThread().getContextClassLoader.getResourceAsStream(confFile)
    p.load(reader)
    reader.close()
    p
  }
  private final val publishHost:String = properties.getProperty("publish_host")
  private final val publishPort:Int = Integer.parseInt(properties.getProperty("publish_port"))
  private final val publishUser:String = properties.getProperty("publish_user")
  private final val publishPassword:String = properties.getProperty("publish_password")
  private final val publishQueue:String = properties.getProperty("publish_queue")
  private final val metricPublish:MessagePublisher = new MessagePublisher(publishHost,publishPort,publishUser,publishPassword,publishQueue)

  def call(client2monitor:AngelClient): Unit = {
    client = client2monitor
    val jobId = client.getConf.get("jobId")
    // appId
    appId = invokeMethod(client,"getAppId").asInstanceOf[String]
    //updateMaster
    //updateMaster = (params:AnyRef) => invokeMethod(client,"updateMaster",java.lang.Integer.TYPE)(params)
    //get the master
    master = getSuperField(client,"master").asInstanceOf[MasterProtocol]

    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      try {
        while(!isFinished){
          publishJobReport(metricPublish.publish,jobId)
          this.synchronized {
            this.wait(100)
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
          // updateMaster(Integer.valueOf(10*60))
          if (master != null) response = master.getJobReport(null, getJobRequest)
        } catch {
          case e1: Exception => LOG.error("Another failed. " + e1.getMessage)
        }
    }

    isFinished = getSuperField(client,"isFinished").asInstanceOf[Boolean]
    appFailedMessage = getSuperField(client, "appFailedMessage").asInstanceOf[String]

    if(isFinished || appFailedMessage != null) {
      val epoch = -1
      val metricTable = Map[String,Double]()
      isFinished = true
      val metrics = new Metrics(jobId,epoch,metricTable)
      publish(metrics)
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
