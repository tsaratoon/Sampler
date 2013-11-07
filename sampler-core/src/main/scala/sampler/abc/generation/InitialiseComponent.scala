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

package sampler.abc.generation

import sampler.abc.ABCParameters
import sampler.abc.EncapsulatedPopulation
import sampler.abc.ABCModel
import sampler.abc.Particle

protected[abc] trait InitialiseComponent {
	val initialise: Initialise
	
	trait Initialise {
		def apply[M <: ABCModel](
				model: M, 
				abcParams: ABCParameters
		): EncapsulatedPopulation[M] = {
			val numParticles = abcParams.numParticles
			val pop0 = (1 to numParticles).par.map(i => Particle(model.prior.sample(), 1.0, Double.MaxValue)).seq
			EncapsulatedPopulation(model)(pop0)
		}
	}
}