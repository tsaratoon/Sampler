package farrington.core.result

import play.api.libs.json.JsValue
import play.api.libs.json.JsValue.jsValueToJsLookup

case class ResultVector(date: IndexedSeq[Date], actual: IndexedSeq[Int], expected: IndexedSeq[Double], threshold: IndexedSeq[Double], trend: IndexedSeq[Int], exceed: IndexedSeq[Double], weights: IndexedSeq[Double]){
  lazy val isAlert = (0 until threshold.length).map(i => (actual(i) > threshold(i)))
}
object ResultVector{
  def apply(date: IndexedSeq[Date], actual: IndexedSeq[Int], json: JsValue): ResultVector = ResultVector(
      date,     
      actual,
      (json \ "expected").as[List[Double]].toIndexedSeq,
      (json \ "threshold").as[List[Double]].toIndexedSeq,
      (json \ "trend").as[List[Int]].toIndexedSeq,    
      (json \ "exceed").as[List[Double]].toIndexedSeq,
      (json \ "weights").as[List[Double]].toIndexedSeq
  )
}