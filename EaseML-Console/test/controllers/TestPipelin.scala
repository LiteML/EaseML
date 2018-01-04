package controllers



import java.util.EventObject

import org.apache.commons.pipeline.Pipeline
import org.apache.commons.pipeline.driver.{SynchronousStageDriverFactory, ThreadPoolStageDriverFactory}
import org.apache.commons.pipeline.stage.LogStage

import scala.concurrent.Future

/**
  * Created by takun on 29/12/2017.
  */
object TestPipeline {

  def main(args: Array[String]): Unit = {
    val pipeline = new Pipeline
    val b1 = new Pipeline
    val b2 = new Pipeline
    val sdf = new SynchronousStageDriverFactory
    b1.addStage(new TestStage("b1-1"), sdf)
    b1.addStage(new TestStage("b1-2"), sdf)
    b2.addStage(new TestStage("b2-1"), sdf)
    b2.addStage(new TestStage("b2-2"), sdf)
    pipeline.addBranch("b1", b1)
    pipeline.addBranch("b2", b2)
    import scala.concurrent.ExecutionContext.Implicits.global
//    Future[Unit] {
//      Thread.sleep(5000)
//      pipeline.getSourceFeeder.feed("xxxxx")
//      println("xxxxxxxxxxxxxxxxxxx")
//      pipeline.finish()
//    }
    b1.start()
    b1.getSourceFeeder.feed("xxxxx")
  }
}
