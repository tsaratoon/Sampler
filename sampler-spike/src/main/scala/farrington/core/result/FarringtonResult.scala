package farrington.core.result

// TODO Make these case classes clearer

case class FarringtonResult(
    results: ResultVector,
    flags: IndexedSeq[Int]
)

case class FarringtonResult2(
    results: IndexedSeq[Result],
    flags: IndexedSeq[Int]
)