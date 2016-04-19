package farrington.core.json

import scala.collection.SortedMap
import org.json4s.JObject
import org.json4s.native.JsonMethods.compact
import org.json4s.native.JsonMethods.parse
import org.json4s.native.JsonMethods.pretty
import org.json4s.native.JsonMethods.render
import org.json4s.JsonDSL.int2jvalue
import org.json4s.JsonDSL.jobject2assoc
import org.json4s.JsonDSL.long2jvalue
import org.json4s.JsonDSL._
import org.json4s.JsonDSL.seq2jvalue
import farrington.core.result.Date
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import farrington.core.outbreak.OutbreakData
import org.json4s.JsonAST.JValue
import org.json4s.DefaultFormats
import farrington.core.simulate.SimulatedData
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsValue
import play.api.libs.json.Writes

object JSON {
  
  implicit val timeSeriesWrites = new Writes[SortedMap[Date, Int]] {
    def writes(timeSeries: SortedMap[Date, Int]) = Json.obj(
      "current" -> Json.obj(
          "month" -> timeSeries.last._1.idx,
          "incidents" -> timeSeries.last._2
      ),
      "baseline" -> Json.obj(
          "month" -> timeSeries.keySet.map(_.idx),
          "count" -> timeSeries.values
      ),
      "startDate" -> Json.obj(
          "year" -> timeSeries.head._1.yearMonth.getYear,
          "month" -> timeSeries.head._1.yearMonth.getMonthValue
      )
    )
  }
  
  // Read json file as string
  def readJSON(path: Path): String = {
    val br = Files.newBufferedReader(path, Charset.defaultCharset())
    Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
  }
  
  def writeJSON(json: JsValue, path: Path) = {    
    val jsonAsString = Json.prettyPrint(json)
    val writer = Files.newBufferedWriter(path, Charset.defaultCharset())
    writer.write(jsonAsString)
    writer.close()
  }

}