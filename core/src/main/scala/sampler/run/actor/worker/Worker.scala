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

package sampler.run.actor.worker

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.Future
import scala.util.Try
import akka.actor.actorRef2Scala
import sampler.run.actor.client.WorkAvailable
import sampler.run.actor.client.Request
import sampler.run.actor.client.dispatch.Job
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import akka.actor.ActorRef

case class Abort()
case class StatusRequest()
case class WorkerBusy()
case class WorkerIdle()
case class WorkConfirmed()
case class WorkRejected()

//object WorkerApp extends App with HostnameSetup{
//	if(args.size == 1){
//		System.setProperty("akka.remote.netty.port", args(0))
//		ConfigFactory.invalidateCaches()
//	}
//	val system = ActorSystem.withPortFallback("ClusterSystem")
//	system.actorOf(Props(new WorkerBase(new TestDomainWorker)), name = "workerroot")
//}

/*
 * Keep the worker simple.  It just responds to status requests
 * and switches between idle and busy states, with the option 
 * of aborting work
 */
class Worker(runner: Executor) extends Actor with ActorLogging{
	case class Done()
	
	val cluster = Cluster(context.system)
	override def preStart(): Unit = cluster.subscribe(self, classOf[ClusterEvent.UnreachableMember])
	override def postStop(): Unit = cluster.unsubscribe(self)
	
	def receive = idle
	
	def idle: Receive = {
		case StatusRequest => sender ! WorkerIdle
		case WorkAvailable => sender ! WorkerIdle
		case request: Request =>
			log.debug("Got request "+request.job)
			doWork(request.job)
	}
	
	def doWork(job: Job[_]){
		val sndr = sender
		val me = self
		context.become(busy(sndr))
		import context._
		Future{
			val result = Try(runner(job))
			me ! Done
			sndr ! result
		}
		sndr ! WorkConfirmed
	}
	
	def busy(requestor: ActorRef): Receive = {
		case ClusterEvent.UnreachableMember(m) =>
			if(requestor.path.address == m.address) {
				log.warning(s"Detected requester unreachable "+m.address)
				self ! Abort
			}
		case StatusRequest => sender ! WorkerBusy
		case Abort => 
			log.info("Aborting")
			runner.abort
			//Let the aborted future complete, then status will become idle
		case Done => 
			log.info("Becoming idle")
			context.become(idle)
		case Request => log.warning("Received request when busy, ignoring")
		case WorkAvailable => //Ignore
		case msg => log.warning("Unexpected msg: "+msg.toString)
	}
}