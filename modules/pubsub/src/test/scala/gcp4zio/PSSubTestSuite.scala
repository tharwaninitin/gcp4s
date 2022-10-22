package gcp4zio

import gcp4zio.Global._
import gcp4zio.pubsub.{PSSubApi, PSSubEnv}
import zio.ZIO
import zio.test.Assertion.{containsString, equalTo}
import zio.test.{assertZIO, suite, test, Spec, TestAspect}

@SuppressWarnings(Array("org.wartremover.warts.AutoUnboxing"))
object PSSubTestSuite {
  val spec: Spec[PSSubEnv, Any] =
    suite("PubSubSubscription APIs")(
      test("Create Subscription with existing topic") {
        val step = PSSubApi.createPullSubscription(gcsProject, subscription1, topic)
        assertZIO(step.foldZIO(ex => ZIO.fail(ex.toString), _ => ZIO.succeed("ok")))(equalTo("ok"))
      },
      test("Create Subscription with non-existing topic") {
        val step  = PSSubApi.createPullSubscription(gcsProject, subscription2, nonExistingTopic)
        val error = "NOT_FOUND"
        assertZIO(step.foldZIO(ex => ZIO.succeed(ex.getMessage), _ => ZIO.fail("ok")))(containsString(error))
      },
      test("Create Subscription with existing Subscription Name") {
        val step  = PSSubApi.createPullSubscription(gcsProject, subscription1, topic)
        val error = "ALREADY_EXISTS"
        assertZIO(step.foldZIO(ex => ZIO.succeed(ex.getMessage), _ => ZIO.fail("ok")))(containsString(error))
      },
      test("Delete existing Subscription") {
        val step = PSSubApi.deleteSubscription(gcsProject, subscription1)
        assertZIO(step.foldZIO(ex => ZIO.fail(ex.toString), _ => ZIO.succeed("ok")))(equalTo("ok"))
      },
      test("Delete non-existing Subscription") {
        val step  = PSSubApi.deleteSubscription(gcsProject, subscription1)
        val error = "NOT_FOUND"
        assertZIO(step.foldZIO(ex => ZIO.succeed(ex.getMessage), _ => ZIO.fail("ok")))(containsString(error))
      }
    ) @@ TestAspect.sequential
}
