package easeml

import com.tencent.angel.client.AngelClient
import com.tencent.angel.master.MasterProtocol
import com.tencent.angel.protobuf.generated.ClientMasterServiceProtos.{GetJobReportRequest, GetJobReportResponse, JobReportProto}
import java.lang.reflect.Method

import AngelMonitor._
import easeml.common.metrics.Metrics
import org.apache.commons.logging.{Log, LogFactory}
import scala.collection.JavaConversions._
/**
  * Created by chris on 12/26/17.
  */
object AngelMonitor {
  val LOG:Log = LogFactory.getLog(AngelMonitor.getClass)
}

class AngelMonitor {
  private var master:MasterProtocol = _
  private var getJobReportReq: GetJobReportRequest = _
  private var getJobReportReqBuilder: GetJobReportRequest.Builder = _
  private var appId:String = _
  private var client:AngelClient = _
  private var updateMaster:(Any*) => Object = _
  private var isFinished: Boolean = false
  private var lastReport:GetJobReportResponse = null

  def call(publish: Metrics => Unit, client2monitor:AngelClient):Unit = {
    client = client2monitor
    val jobId = client.getConf.get("jobId")
    // AppId
    val getAppId = invokeMethod(client,"getAppId",null)
    appId = getAppId.toString
    //updateMaster
    updateMaster = invokeMethod(client,"updateMaster",java.lang.Integer.TYPE)
    //get the master
    master = getField(client,"master").asInstanceOf[MasterProtocol]

    while(!isFinished) {
      publishJobReport(publish,jobId)
      Thread.sleep(1000)
    }
  }


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
    if(response == null) {
      isFinished = true
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
