package farrington.core.result

import java.time.YearMonth
import play.api.libs.json.JsValue

case class Date(yearMonth: YearMonth, idx: Long)

case class Result(date: Date, actual: Int, expected: Double, threshold: Double, trend: Int, exceed: Double, weights: IndexedSeq[Double]){
  lazy val isAlert = actual > threshold
}
case object Result {
  def apply(date: Date, actual: Int, json: JsValue): Result = Result(
      date,     
      actual,
      (json \ "expected").as[Double],
      (json \ "threshold").as[Double],
      (json \ "trend").as[Int],    
      (json \ "exceed").as[Double],
      (json \ "weights").as[List[Double]].toIndexedSeq
    )
}