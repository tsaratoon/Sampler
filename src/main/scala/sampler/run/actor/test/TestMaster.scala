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

package sampler.run.actor.test

import sampler.run.actor.PortFallbackSystem
import sampler.run.actor.dispatch.Job
import sampler.run.actor.dispatch.FailFastDispatcher

case class TestJob(i: Int) extends Job[String]

object TestMaster extends App{
	val system = PortFallbackSystem("ClusterSystem")
	
	//Run 100 jobs ...
	val jobs = (1 to 100).map{i => TestJob(i)}
	
	// ... but one of them failed, causing the rest to abort
	val result = new FailFastDispatcher(system).apply(jobs)
	
	println("*********************")
	println("Result is ..."+result)
	println("*********************")
	
}