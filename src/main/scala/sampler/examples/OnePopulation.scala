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

package sampler.examples

import sampler.math.Random
import sampler.data.Distance
import sampler.math.Probability
import sampler.data.Distribution
import sampler.data.FrequencyTableBuilder
import sampler.data.FrequencyTable
import scala.collection.mutable.ListBuffer
import sampler.io.CSVTableWriter
import java.nio.file.Paths
import sampler.data.Types.Column

object AnotherOnePopulation extends App{
	/*
	 * In a population of a given size, and sampling with replacement,
	 * how many samples should be taken to be % confident of observing
	 * the correct population prevalence (within specific precision)?
	 */
	
	val start = System.currentTimeMillis
	
	//Domain parameters
	val populationSize = 100
	val truePrevalence = 0.1
	val precision = 0.09
	val requiredConfidence = Probability(0.95)
	
	//Meta-parameters
	val chunkSize = 2000
	val convergenceCriterion = 0.001
	
	implicit val random = new Random
	val numInfected = (populationSize*truePrevalence).round.toInt
	val population = (1 to populationSize).map(_ < numInfected)
		
	def empiricalObjective(numSampled: Int) = {
		val model = 
			Distribution.withoutReplacement(population, numSampled)	// Start with base model
			.map(_.count(identity) / numSampled.toDouble)			// Transform to model of sample prevalance
		
		// Sample the model until convergence
		FrequencyTableBuilder.parallel(model, chunkSize){samples =>			
			val distance = Distance.max(FrequencyTable(samples.seq.take(samples.size - chunkSize)), FrequencyTable(samples.seq))
			(distance < convergenceCriterion) || (samples.size > 1e8)
		}
		.map(samplePrev => math.abs(samplePrev - truePrevalence) < precision)		// Transform samples to in/out of tolerance
	}
	
	val sampleSizeList = ListBuffer[Int]()
	val confidenceList = ListBuffer[Probability]()
	
	val result = {
		(1 to populationSize)
			.view
			.map{n => 
				val eo = empiricalObjective(n)
				val confidence = eo.probabilityMap.get(true).getOrElse(Probability.zero)
				println("Sample size = %d, empirical size = %d, confidence = %s".format(n, eo.size, confidence.toString))
				sampleSizeList.+=(n)
				confidenceList.+=(confidence)
				(n, confidence)
			}
			.find(_._2 > requiredConfidence)
			.get._1
	}
	
	new CSVTableWriter(Paths.get("OnePopulation.csv"))(
		Column(sampleSizeList.toList, Some("SampleSize")),
		Column(confidenceList.toList, Some("Confidence"))
	)
	
	
	println("Estimated minimum sample size required to detect a prevalence of " + truePrevalence + 
				" (precision " + precision + ") with confidence of " + requiredConfidence + "% is " + result)
	
	println("took " + (System.currentTimeMillis - start) + "ms.")
}