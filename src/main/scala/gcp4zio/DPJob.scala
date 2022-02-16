package gcp4zio

import com.google.cloud.dataproc.v1._
import zio.{Managed, Task, TaskLayer}
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

case class DPJob(client: JobControllerClient) extends DPJobApi.Service {

  private def submitAndWait(projectId: String, region: String, job: Job): Job = {
    val response = client.submitJob(projectId, region, job)
    val jobId    = response.getReference.getJobId
    logger.info(s"Submitted job $jobId")
    var continue = true
    var jobInfo  = client.getJob(projectId, region, jobId)
    var jobState = jobInfo.getStatus.getState.toString
    while (continue) {
      jobInfo = client.getJob(projectId, region, jobId)
      jobState = jobInfo.getStatus.getState.toString
      logger.info(s"Job $jobId Status $jobState")
      jobInfo.getStatus.getState.toString match {
        case "DONE" =>
          logger.info(s"Job $jobId completed successfully with state $jobState")
          continue = false
        case "CANCELLED" | "ERROR" =>
          val error = jobInfo.getStatus.getDetails
          logger.error(s"Job $jobId failed with error $error")
          throw new RuntimeException(s"Job failed with error $error")
        case _ =>
          TimeUnit.SECONDS.sleep(10)
      }
    }
    response
  }

  def executeSparkJob(
      args: List[String],
      mainClass: String,
      libs: List[String],
      conf: Map[String, String],
      clusterName: String,
      project: String,
      region: String
  ): Task[Job] = Task {
    logger.info(s"""Trying to submit spark job on Dataproc with Configurations:
                   |region => $region
                   |project => $project
                   |clusterName => $clusterName
                   |mainClass => $mainClass
                   |args => $args
                   |conf => $conf""".stripMargin)
    logger.info("libs")
    libs.foreach(logger.info)

    val jobPlacement = JobPlacement.newBuilder().setClusterName(clusterName).build()
    val sparkJob = SparkJob
      .newBuilder()
      .addAllJarFileUris(libs.asJava)
      .putAllProperties(conf.asJava)
      .setMainClass(mainClass)
      .addAllArgs(args.asJava)
      .build()
    val job: Job = Job.newBuilder().setPlacement(jobPlacement).setSparkJob(sparkJob).build()
    submitAndWait(project, region, job)
  }

  def executeHiveJob(query: String, clusterName: String, project: String, region: String): Task[Job] = Task {
    logger.info(s"""Trying to submit hive job on Dataproc with Configurations:
                   |region => $region
                   |project => $project
                   |clusterName => $clusterName
                   |query => $query""".stripMargin)
    val jobPlacement = JobPlacement.newBuilder().setClusterName(clusterName).build()
    val queryList    = QueryList.newBuilder().addQueries(query)
    val hiveJob      = HiveJob.newBuilder().setQueryList(queryList).build()
    val job          = Job.newBuilder().setPlacement(jobPlacement).setHiveJob(hiveJob).build()
    submitAndWait(project, region, job)
  }
}

object DPJob {
  def live(endpoint: String): TaskLayer[DPJobEnv] = Managed.fromAutoCloseable(DPJobClient(endpoint)).map(dp => DPJob(dp)).toLayer
}
