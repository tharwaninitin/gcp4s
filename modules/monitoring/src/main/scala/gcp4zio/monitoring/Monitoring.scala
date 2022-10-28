package gcp4zio
package monitoring

import com.google.monitoring.v3.{TimeInterval, TimeSeries}
import zio._

trait Monitoring {
  def getMetric(project: String, metric: String, interval: TimeInterval): Task[Iterable[TimeSeries]]
}

object Monitoring {
  def getMetric(project: String, metric: String, interval: TimeInterval): ZIO[Monitoring, Throwable, Iterable[TimeSeries]] =
    ZIO.environmentWithZIO(_.get.getMetric(project, metric, interval))
  def live(path: Option[String] = None): TaskLayer[Monitoring] =
    ZLayer.scoped(MonitoringClient(path).map(client => MonitoringImpl(client)))
}
