package sampler.math

class RangeException[N: Numeric](actual: N, lower: N, upper: N) 
	extends RuntimeException(s"$actual not in range [$lower, $upper]")

/** Object to allow  checking that a value is a valid probability or is within a given range*/

object RangeCheck {
  /** Checks that a given value is a valid probability
   *  
   *  @param p The value to be checked as a valid probability 
   */
	def probability[T: Fractional](p: T) {
		val f = implicitly[Fractional[T]]
		if(f.lt(p, f.zero) || f.gt(p, f.one)) 
			throw new RangeException(p, f.zero, f.one)
	}
	
	/** Used to check if a given value is close enough to an expected value (determined 
	 *  by a tolerance) to be considered equal 
	 *  
	 *  @param actual The value to be tested
	 *  @param expected The expected value, that equality is tested against
	 *  @param tolerance The acceptable tolerance for deviation from the expected value
	 */
	def within[T: Fractional](actual: T, expected: T, tolerance: T) {
		val f = implicitly[Fractional[T]]
		import f.mkNumericOps
		if(f.gt(f.abs(actual - expected), tolerance)) 
			throw new RangeException(actual, expected - tolerance, actual + tolerance)
	}
}