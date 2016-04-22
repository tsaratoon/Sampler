package farrington.core.outbreak

import java.nio.file.Files
import java.nio.charset.Charset
import java.nio.file.Path
import play.api.libs.json._

case class OutbreakData(
  year: IndexedSeq[Int],
  month: IndexedSeq[Int],
  counts: IndexedSeq[Int]
)
case object OutbreakData {
  
  def apply(json: JsValue): OutbreakData = OutbreakData(
    (json \ "year").as[List[Int]].toIndexedSeq,
    (json \ "month").as[List[Int]].toIndexedSeq,
    (json \ "count").as[List[Int]].toIndexedSeq
  )
  
  implicit val outbreakDataWrites = new Writes[OutbreakData] {
    def writes(data: OutbreakData) = Json.obj(
      "year" -> data.year,
      "month" -> data.month,
      "index" -> (1 to data.month.length),
      "count" -> data.counts
    )
  }
  
  def writeToFile(data: OutbreakData, path: Path, filename: String) = {  
    val nData = data.counts.length
    Files.createDirectories(path)
    val writer = Files.newBufferedWriter(path.resolve(filename), Charset.defaultCharset())
    writer.write("dateIndex, year, month, count")
    writer.newLine
    for (i <- 1 until nData) {
      writer.write(s"${(i+1).toString}, ${data.year(i).toString}, ${data.month(i).toString}, ${data.counts(i).toString}")
      writer.newLine
    }
    writer.close    
  }
}