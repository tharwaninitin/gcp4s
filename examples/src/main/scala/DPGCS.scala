import com.google.cloud.dataproc.v1.Job
import gcp4zio.dp.DPJob
import gcp4zio.gcs.GCS
import zio.stream.ZPipeline
import zio.{Task, ZIO, ZIOAppDefault}
import java.net.URI

// Before running this application make sure dataproc cluster is running and below mentioned environment variables are set
// export GOOGLE_APPLICATION_CREDENTIALS=
// export GCP_PROJECT=
// export GCP_REGION=
// export DP_CLUSTER=
// export DP_ENDPOINT=

object DPGCS extends ZIOAppDefault with ApplicationLogger {
  val gcpProject: String = sys.env("GCP_PROJECT")
  val gcpRegion: String  = sys.env("GCP_REGION")
  val dpCluster: String  = sys.env("DP_CLUSTER")
  val dpEndpoint: String = sys.env("DP_ENDPOINT")

  def printGcsLogs(response: Job): ZIO[GCS, Throwable, Unit] = {
    val uri    = new URI(response.getDriverOutputResourceUri)
    val bucket = uri.getHost
    val path   = uri.getPath.substring(1)
    GCS
      .listObjects(bucket, Some(path), recursive = false, List.empty)
      .flatMap { blob =>
        logger.info(s"Reading logs from gs://$bucket/${blob.getName} with size ${blob.getSize} bytes")
        GCS
          .getObject(bucket, blob.getName, 4096)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .tap(line => ZIO.succeed(logger.info(line)))
      }
      .runDrain
  }

  private val libs = List("file:///usr/lib/spark/examples/jars/spark-examples.jar")
  private val conf = Map("spark.executor.memory" -> "1g", "spark.driver.memory" -> "1g")

  private val mainClass = "org.apache.spark.examples.SparkPi"

  private val program1 = DPJob
    .executeSparkJob(List("1000"), mainClass, libs, conf, dpCluster, gcpProject, gcpRegion)
    .flatMap(printGcsLogs)

  private val program2 = for {
    job <- DPJob.submitHiveJob("SELE 1 AS ONE", dpCluster, gcpProject, gcpRegion)
    _   <- DPJob.trackJobProgress(gcpProject, gcpRegion, job).tapError(_ => printGcsLogs(job))
    _   <- printGcsLogs(job)
  } yield ()

  val run: Task[Unit] = (program1 *> program2).provide(DPJob.live(dpEndpoint) ++ GCS.live())
}
