package easeml.dag

import scala.util.Random

/**
  * Created by takun on 03/01/2018.
  */
case class TestStage(name:String) extends BaseStage() {
  override def preprocess(): Unit = {
    println(s"[$name] preprocess")
  }

  val random = new Random(1L)
  override def process(obj: AnyRef*): AnyRef = {
    println(name + " : " + obj.mkString(","))
    Thread.sleep(1000)
    if(random.nextDouble() > .8) throw new RuntimeException("eeeee")
    NONE
  }

  override def postprocess(): Unit = {
    println(s"[$name] postprocess")
  }

  override def getName: String = name
}

object Test {

  def main(args: Array[String]): Unit = {
    val s0 = TestStage("s0")
    val s1 = TestStage("s11")
    val s2 = TestStage("s12")
    val s21 = TestStage("s21")
    s21.dependOn(s0)
    s2.dependOn(s1)
    s1.dependOn(s0)
    val dag = new Dag(new ThreadPoolStageContext(10))
    dag.addStage(s21)
    dag.addStage(s2)
    0 until 100 foreach {
      i =>
        println(dag.run(Integer.valueOf(i)))
    }
  }
}
