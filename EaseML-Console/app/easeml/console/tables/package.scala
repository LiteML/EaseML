package easeml.console

import slick.lifted.Tag
import slick.jdbc.MySQLProfile.api._

/**
  * Created by takun on 05/01/2018.
  */
package object tables {

  class Job(tag: Tag) extends Table[(Int, String, String, String, String, Int)](tag, "job") {
    def id = column[Int]("id")
    def name = column[String]("name")
    def algorithm = column[String]("algorithm")
    def server = column[String]("server")
    def params = column[String]("params")
    def status = column[Int]("status")
    def * = (id, name, algorithm, server, params, status)
  }

  class Metric(tag: Tag) extends Table[(Int, Int, String, Double)](tag, "metric") {
    def job_id = column[Int]("job_id")
    def epoch = column[Int]("epoch")
    def name = column[String]("name")
    def value = column[Double]("value")
    def * = (job_id, epoch, name, value)
  }

  class AlgorithmServer(tag: Tag) extends Table[(Int, String)](tag, "algo_server") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def * = (id, name)
  }

  class Algorithms(tag: Tag) extends Table[(Int, Int, String, String)](tag, "algorithms") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def serverId = column[Int]("id")
    def name = column[String]("name")
    def parameters = column[String]("parameters")
    def * = (id, serverId, name, parameters)
  }

}
