package controllers

import easeml.dag.{BaseStage, Stage}

/**
  * Created by takun on 02/01/2018.
  */
class TestStage(name:String) extends BaseStage{

  override def process(obj: scala.AnyRef*) = {
    println(s"[$name] pre: $obj" )
    Thread.sleep(1000)
    println(s"[$name] post: $obj" )
    ""
  }

  override def getName: String = name
}
