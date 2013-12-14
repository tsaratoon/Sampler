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

package sampler.cluster.abc.actor.worker

import java.util.concurrent.atomic.AtomicBoolean
import scala.annotation.tailrec
import scala.util.Try
import sampler.abc.MaxRetryException
import sampler.abc.ABCModel
import sampler.data.Distribution
import sampler.data.SerialSampler
import sampler.io.Logging
import sampler.math.Partition
import sampler.math.Random
import sampler.run.DetectedAbortionException
import sampler.cluster.abc.actor.Job
import sampler.abc.Scored
import sampler.abc.Weighted

trait AbortableModelRunner extends Logging{
	val model: ABCModel
	import model._
	implicit val random: Random
	
	val aborted: AtomicBoolean = new AtomicBoolean(false)
	
	val parallelism: Int
	lazy val parallelTaskSupport = 
		if(parallelism == 0) None
		else Some(new scala.collection.parallel.ForkJoinTaskSupport(new scala.concurrent.forkjoin.ForkJoinPool(parallelism)))

	def abort() { aborted.set(true) }
	def isAborted = aborted.get
	def reset() { aborted.set(false) }
	
	def run(job: Job): Try[Seq[Scored[ParameterSet]]] = Try{
		val prevPopulation = job.population.asInstanceOf[Seq[Weighted[ParameterSet]]]
		val weightsTable = prevPopulation.groupBy(_.value).map{case (k,v) => (k, v.map(_.weight).sum)}.toIndexedSeq
		val (parameterSets, weights) = weightsTable.unzip
		val samplablePopulation = Distribution.fromPartition(parameterSets, Partition.fromWeights(weights))
		
		@tailrec
		def getScoredParameter(failures: Int = 0): Scored[ParameterSet] = {
			if(isAborted) throw new DetectedAbortionException("Abort flag was set")
			else if(failures >= job.abcParams.particleRetries) throw new MaxRetryException(s"Aborted after the maximum of $failures trials")
			else{
				def getScores(params: ParameterSet): IndexedSeq[Double] = {
					val modelWithMetric = modelDistribution(params).map(_.distanceToObserved)
					//TODO in parallel?
					SerialSampler(modelWithMetric)(_.size == job.abcParams.numReplicates)
				}
				
				val res: Option[Scored[ParameterSet]] = for{
					params <- Some(samplablePopulation.sample().perturb()) if prior.density(params) > 0
					fitScores <- Some(getScores(params))
				} yield Scored(params, fitScores)
				
				res match {
					case Some(p) => p
					case None => getScoredParameter(failures + 1)
				}
			}
		}
		
		val pc = (1 to job.abcParams.particleChunkSize).par
		parallelTaskSupport.foreach{pc.tasksupport = _}
		pc.map(i => getScoredParameter()).seq
	}
}

object AbortableModelRunner{
	def apply(model0: ABCModel, parallelism0: Int) = new AbortableModelRunner{
		val model = model0
		val random = Random
		val parallelism = parallelism0
	}
}

