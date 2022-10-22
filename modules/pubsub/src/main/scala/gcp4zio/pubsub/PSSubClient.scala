package gcp4zio
package pubsub

import com.google.api.gax.core.{FixedCredentialsProvider, NoCredentialsProvider}
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.auth.oauth2.{GoogleCredentials, ServiceAccountCredentials}
import com.google.cloud.pubsub.v1.{SubscriptionAdminClient, SubscriptionAdminSettings}
import io.grpc.ManagedChannelBuilder
import zio.{Task, ZIO}
import java.io.FileInputStream

object PSSubClient {

  private def getSubscriptionClient(path: String): SubscriptionAdminClient = {
    val credentials: GoogleCredentials = ServiceAccountCredentials.fromStream(new FileInputStream(path))
    val subscriptionAdminSettings = SubscriptionAdminSettings
      .newBuilder()
      .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
      .build
    SubscriptionAdminClient.create(subscriptionAdminSettings)
  }

  val testClient: Task[SubscriptionAdminClient] = ZIO.attempt {
    val hostport            = System.getenv("PUBSUB_EMULATOR_HOST")
    val channel             = ManagedChannelBuilder.forTarget(hostport).usePlaintext().build()
    val channelProvider     = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
    val credentialsProvider = NoCredentialsProvider.create
    val subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder
      .setTransportChannelProvider(channelProvider)
      .setCredentialsProvider(credentialsProvider)
      .build
    SubscriptionAdminClient.create(subscriptionAdminSettings)
  }

  def apply(path: Option[String]): Task[SubscriptionAdminClient] = ZIO.attempt {
    val envPath: String = sys.env.getOrElse("GOOGLE_APPLICATION_CREDENTIALS", "NOT_SET_IN_ENV")

    path match {
      case Some(p) =>
        logger.info("Using GCP credentials from values passed in function")
        getSubscriptionClient(p)
      case None =>
        if (envPath == "NOT_SET_IN_ENV") {
          logger.info("Using GCP credentials from local sdk")
          SubscriptionAdminClient.create()
        } else {
          logger.info("Using GCP credentials from environment variable GOOGLE_APPLICATION_CREDENTIALS")
          getSubscriptionClient(envPath)
        }
    }
  }
}
