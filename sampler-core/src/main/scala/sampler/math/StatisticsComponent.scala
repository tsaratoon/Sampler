/*
 * Copyright (c) 2012 Crown Copyright 
 *                    Animal Health and Veterinary Laboratories Agency
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sampler.math

import sampler.data.Empirical

/** Provides methods for calculation of statistical properties of an Empirical object
 *  
 *  
 *  Mix in the StatisticsComponent to enable easy calculation of stats on Empirical. 
 */
trait StatisticsComponent{
  
  /** Returns the proportion (probability) of items in Empirical which are greater than or equal to supplied value
   *  
   *  @param e
   *  @param itemInclusive The value of interest, return value is inclusive of this value
   *  @return a new Probability giving the right tail
   *  */
	def rightTail[A](e: Empirical[A], itemInclusive: A)(implicit o: Ordering[A]): Probability = {
    import e._
		val value = probabilityTable.keys.toList.sorted(o).dropWhile(i => o.lt(i,itemInclusive)).foldLeft(0.0){
			case (acc, i) => acc + probabilityTable(i).value
		}
		Probability(value)
	}
	
	//TODO take seq of probability and give seq of results
	/** Returns quantile values from an Empirical given a specified probability
	 *  
	 *  @param e
	 *  @param prob The required quantile value
	 *  @return The value at the requested quantile
	 */
	def quantile[A](e: Empirical[A], prob: Probability)(implicit f: Fractional[A]): A = {
		import e._

		val probabilities = e.probabilityTable
		
		val ordered = probabilities.keys.toIndexedSeq.sorted
		
		assert(ordered.length > 0, "Cannot work out quantiles of an Empirical object with zero values")

		val cumulativeProbability = ordered.map(value => probabilities(value).value).scanLeft(0.0)(_ + _).tail
		
		// Added tolerance in condition to account for rounding error and ensure consistency with R Type 1
		val index = cumulativeProbability.zipWithIndex.find(_._1 >= (prob.value - 1e-6)).get._2		
		
		ordered(index)
	}
	
	/** Returns the mean value of an Empirical
	 *  
	 *  @param e
	 *  @return The mean value
	 */
	def mean[A](e: Empirical[A])(implicit num: Fractional[A]) = {
		import num._
		e.probabilityTable.foldLeft(0.0){case (acc, (v,p)) => {
			acc + v.toDouble * p.value
		}}
	}
	
	/** Returns the difference between the mean of two Empiricals
	 *  
	 *  A metric for calculating the difference between two Empiricals, using the mean value of the 
	 *  two Empiricals
	 *  
	 *  @param a
	 *  @param b
	 *  @return The difference between means
	 */
	def meanDistance[A: Fractional](a: Empirical[A], b: Empirical[A]) = {
		math.abs(mean(a)-mean(b))
	}
  
	/** Returns to maximum difference between two Empiricals
	 *  
	 *  An alternative metric for calculating the distance between two Empiricals. The distance is defined
	 *  as the greatest distance between the probabilities of each individual value in two distributions.
	 *  
	 *  E.g. In Empirical(1, 2) the probabilities are 1 -> 0.5, 2 -> 0.5. In Empirical(1,2,2,2,3) 
	 *  the probabilities are 1 -> 0.2, 2 -> 0.6, 3 -> 0.2. The differences are therefore 1 -> 0.3, 
	 *  2 -> 0.1 and 3 -> 0.2 and thus the max distance is 0.3 
	 *  
	 *  @param a
	 *  @param b
	 *  @return The max difference
	 */
	def maxDistance[A](a: Empirical[A], b: Empirical[A]): Double = {
		val indexes = a.probabilityTable.keySet ++ b.probabilityTable.keySet
		def distAtIndex(i: A) = math.abs(
			a.probabilityTable.get(i).map(_.value).getOrElse(0.0) -
			b.probabilityTable.get(i).map(_.value).getOrElse(0.0)
		)
		indexes.map(distAtIndex(_)).max
  }
}

object Statistics extends StatisticsComponent with Serializable

