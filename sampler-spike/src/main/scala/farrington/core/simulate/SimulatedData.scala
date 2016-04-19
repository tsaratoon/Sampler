package farrington.core.simulate

import java.nio.file.Path
import java.nio.charset.Charset
import java.nio.file.Files
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsValue

case class BaselineData(
    year: IndexedSeq[Int],
    month: IndexedSeq[Int],
    baseline: IndexedSeq[Int],
    mean: IndexedSeq[Double]
  )

case class SimulatedData(
    year: IndexedSeq[Int],
    month: IndexedSeq[Int],
    baseline: IndexedSeq[Int],
    counts: IndexedSeq[Int],   // Baseline counts + outbreak counts (baseline + hist)
    hist: List[(Int, Int)],
    start: Int,
    end: Int
  )
case object SimulatedData {
  
  def apply(json: JsValue): SimulatedData = {
    val counts = (json \ "counts" \ "total").as[List[Int]].toIndexedSeq
    val histDates = (json \ "hist" \ "date").as[List[Int]]
    val histCounts = (json \ "hist" \ "count").as[List[Int]]
    val histData = histDates.zip(histCounts)
    SimulatedData(
      (json \ "date" \ "year").as[List[Int]].toIndexedSeq,
      (json \ "date" \ "month").as[List[Int]].toIndexedSeq,
      (json \ "counts" \ "baseline").as[List[Int]].toIndexedSeq,
      counts,
      histData,
      (json \ "outbreak" \ "start").as[Int],
      (json \ "outbreak" \ "end").as[Int]
    )
  }
  
  implicit val simulatedDataWrites = new Writes[SimulatedData] {
    def writes(data: SimulatedData) = Json.obj(
      "date" -> Json.obj(
          "year" -> data.year,
          "month" -> data.month,
          "index" -> (1 to data.month.length)
      ),
      "counts" -> Json.obj(
          "baseline" -> data.baseline,
          "total" -> data.counts
      ),
      "outbreak" -> Json.obj(
          "start" -> data.start,
          "end" -> data.end
      ),
      "hist" -> Json.obj(
          "date" -> data.hist.map(_._1),
          "count" -> data.hist.map(_._2)
      )
    )
  }
  
  def writeToFile(data: SimulatedData, path: Path, filename: String) = {  
    val nData = data.baseline.length
    Files.createDirectories(path)
    val writer = Files.newBufferedWriter(path.resolve(filename), Charset.defaultCharset())
    writer.write("month, baseline, outbreak, start, end")
    writer.newLine
    writer.write(s"${1.toString}, ${data.baseline(0).toString}, ${data.counts(0).toString}, ${data.start.toString}, ${data.end.toString}")
    writer.newLine
    for (i <- 1 until nData) {
      writer.write(s"${(i+1).toString}, ${data.baseline(i).toString}, ${data.counts(i).toString}")
      writer.newLine
    }
    writer.close    
  }
}