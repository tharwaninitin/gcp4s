package gcp4zio

import com.google.cloud.dataproc.v1.Job
import gcp4zio.dp.{DPJobApi, DPJobEnv}
import gcp4zio.gcs.{GCSApi, GCSEnv}
import zio.ZIO
import zio.stream.ZPipeline
import zio.test.Assertion.equalTo
import zio.test._
import java.net.URI

object DPStepsTestSuite extends TestHelper {

  def printGcsLogs(response: Job): ZIO[GCSEnv, Throwable, Unit] = {
    val uri    = new URI(response.getDriverOutputResourceUri)
    val bucket = uri.getHost
    val path   = uri.getPath.substring(1)
    GCSApi
      .listObjects(bucket, Some(path), recursive = false, List.empty)
      .flatMap { blob =>
        logger.info(s"Reading logs from gs://$bucket/${blob.getName}")
        GCSApi
          .getObject(bucket, blob.getName, 4096)
          .via(ZPipeline.utf8Decode >>> ZPipeline.splitLines)
          .tap(line => ZIO.succeed(logger.info(line)))
      }
      .runDrain
  }

  val spec: Spec[TestEnvironment with DPJobEnv with GCSEnv, Any] =
    suite("Dataproc Job APIs")(
      test("executeHiveJob") {
        val step = DPJobApi
          .executeHiveJob("SELECT 1 AS ONE", dpCluster, gcpProjectId.getOrElse("NA"), gcpRegion.getOrElse("NA"))
          .flatMap(printGcsLogs)
        assertZIO(step.foldZIO(ex => ZIO.fail(ex.getMessage), _ => ZIO.succeed("ok")))(equalTo("ok"))
      },
      test("executeSparkJob") {
        val libs      = List("file:///usr/lib/spark/examples/jars/spark-examples.jar")
        val conf      = Map("spark.executor.memory" -> "1g", "spark.driver.memory" -> "1g")
        val mainClass = "org.apache.spark.examples.SparkPi"
        val step = DPJobApi
          .executeSparkJob(
            List("1000"),
            mainClass,
            libs,
            conf,
            dpCluster,
            gcpProjectId.getOrElse("NA"),
            gcpRegion.getOrElse("NA")
          )
          .flatMap(printGcsLogs)
        assertZIO(step.foldZIO(ex => ZIO.fail(ex.getMessage), _ => ZIO.succeed("ok")))(equalTo("ok"))
      }
    ) @@ TestAspect.sequential
}
