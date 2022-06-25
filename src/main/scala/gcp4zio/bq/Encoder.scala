package gcp4zio.bq

import com.google.cloud.bigquery.{Field, LegacySQLTypeName, Schema}
import gcp4zio.utils.{ApplicationLogger, LoggedTry}
import java.util
import scala.reflect.ClassTag
import scala.jdk.CollectionConverters._

object Encoder extends ApplicationLogger {

  private def getBQType(spType: String): LegacySQLTypeName = spType match {
    case "string"         => LegacySQLTypeName.STRING
    case "int"            => LegacySQLTypeName.INTEGER
    case "long"           => LegacySQLTypeName.INTEGER
    case "double"         => LegacySQLTypeName.FLOAT
    case "java.sql.Date"  => LegacySQLTypeName.DATE
    case "java.util.Date" => LegacySQLTypeName.DATE
    case "boolean"        => LegacySQLTypeName.BOOLEAN
    case _                => LegacySQLTypeName.STRING
  }

  private def getFields[T: ClassTag]: Array[(String, String)] =
    implicitly[ClassTag[T]].runtimeClass.getDeclaredFields.map(f => (f.getName, f.getType.getName))

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def apply[T: ClassTag]: Option[Schema] =
    LoggedTry {
      val fields   = new util.ArrayList[Field]
      val ccFields = getFields[T]
      if (ccFields.isEmpty)
        throw new RuntimeException("Schema not provided")
      ccFields.foreach(x => fields.add(Field.of(x._1, getBQType(x._2))))
      val s = Schema.of(fields)
      logger.info(s"Schema provided: ${s.getFields.asScala.map(x => (x.getName, x.getType))}")
      s
    }.toOption
}
