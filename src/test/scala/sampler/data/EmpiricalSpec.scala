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

package sampler.data;

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class FrequencyTableSpec extends Specification{
	val once = 1
	val twice = 2
	val thrice = 3
	
	//	--d1--		--d2--		---d3---
	//					6		    3
	//				  5,6		  2,3
	//	4,5,6		4,5,6		1,2,3,4
	val d1 = FrequencyTable[Int](IndexedSeq(4, 5, 6))
	val d2 = FrequencyTable[Int](IndexedSeq(4, 5,5, 6,6,6))
	val d3 = FrequencyTable[Int](IndexedSeq(1, 2,2, 3,3,3, 4))
	
	"Frequency tables" should {
		
		"return the number of items in the sequence used to create the table when asked for the size" in todo
		
		"calculate the probability of observing each sample" in todo
		
		"select a random element from the table when a sample is requested" in todo
		
		"produce a map of counts of each observation" in todo
		
		"map / flatmap??" in todo
		
		"be able to have an extra sample added once initial table has been created" in {
			val newTable = d1.+(1)
			newTable.samples mustEqual List(4,5,6,1)
		}
		
		"be able to combine into one big frequency table" in todo
		
		"have a working right tail" in todo
		
		"have a working quantile" in todo

		"add together, summing counts" in { todo			
//			val dSum: Empirical[Int] = d2 + d3
//			
//			val resultCounts = dSum.counts
//			
//			(resultCounts.size === 6) and
//			(resultCounts(1) === 1) and
//			(resultCounts(2) === 2) and
//			(resultCounts(3) === 3) and
//			(resultCounts(4) === 2) and
//			(resultCounts(5) === 2) and
//			(resultCounts(6) === 3)
		}
//		"use infinity-norm for distance" in {
//			def expectedDist(dA: Empirical[Int], dB: Empirical[Int], position: Int) =
//				math.abs(dA(position)-dB(position))
//		
//			(d1.distanceTo(d2) ===  expectedDist(d1, d2, 6)) and
//			(d1.distanceTo(d3) ===  expectedDist(d1, d3, 3)) and
//			(d2.distanceTo(d3) ===  expectedDist(d2, d3, 6)) // index 6, not 3
//		}
		"Observation counts" in { todo
//			d3(0) === None and
//			d3(1).get === 1 and
//			d3(2).get === 2 and
//			d3(3).get === 3 and
//			d3(4).get === 1 and
//			d3(5) === None
		}
		"Relative frequency" in {
			todo
		}
		"Override equals and hashcode" in {
			val instance1a = FrequencyTable[Int](IndexedSeq(4, 5))
			val instance1b = FrequencyTable[Int](IndexedSeq(4, 5))
			val instance2 = FrequencyTable[Int](IndexedSeq(4, 5,5))
			
			(instance1a mustEqual instance1b) and
			(instance1a mustNotEqual instance2) and
			(instance1a.hashCode mustEqual instance1b.hashCode) and
			(instance1a.hashCode mustNotEqual instance2.hashCode)
		}
	}
}