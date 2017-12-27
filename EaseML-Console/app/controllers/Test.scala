import easeml.common.queue.JobConsumer

/**
  * Created by takun on 26/12/2017.
  */
object TestMq {
  def main(args: Array[String]): Unit = {
    val consumer = new JobConsumer("localhost", 5672, "platform", "platform", "job")
    consumer.consume(handler = {
      job =>
        println(job.toJSON)
        Thread.sleep(5000)
    }, parall = 2)
    println("xxxxxxxxxxxxxx")
  }
}