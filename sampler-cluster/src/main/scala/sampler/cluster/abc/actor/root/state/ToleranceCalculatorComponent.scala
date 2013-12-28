/*
 * Copyright (c) 2012-13 Crown Copyright 
 *                       Animal Health and Veterinary Laboratories Agency
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

package sampler.cluster.abc.actor.root.state

import sampler.math.StatisticsComponent
import sampler.math.Probability
import sampler.Implicits._
import sampler.abc.Weighted
import sampler.io.Logging

trait ToleranceCalculatorComponent {
	self: StatisticsComponent with Logging=>
		
	val toleranceCalculator: ToleranceCalculator
	
	trait ToleranceCalculator {
		def apply[P](weighedParameters: Seq[Weighted[P]], fallBackTolerance: Double): Double = {
			val medianMeanScore = statistics.quantile(weighedParameters.map{_.meanRepScore}.toEmpiricalSeq, Probability(0.5))
			if(medianMeanScore == 0) {
				log.warn("Median of mean scores from last generation evaluated to 0, using fallback tolerance: {}", fallBackTolerance)
				fallBackTolerance
			}
			else math.min(medianMeanScore, fallBackTolerance)
		}
	}
}