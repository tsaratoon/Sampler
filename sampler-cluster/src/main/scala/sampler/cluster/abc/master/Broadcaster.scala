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

package sampler.cluster.abc.master

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.cluster.Cluster
import akka.actor.ActorRef
import akka.cluster.ClusterEvent._
import akka.cluster.Member
import akka.actor.RootActorPath
import akka.cluster.MemberStatus
import akka.actor.actorRef2Scala
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.actor.Props
import akka.cluster.ClusterEvent.ClusterDomainEvent
import scala.concurrent.duration.DurationInt
import sampler.cluster.abc.StatusRequest
import sampler.cluster.abc.WorkerBusy
import sampler.cluster.abc.WorkerIdle
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await

case class Broadcast(msg: Any)

class Broadcaster extends Actor with ActorLogging{
	case class NumWorkers(n: Int)
	
	val reportingActor = context.actorOf(Props(new Actor with ActorLogging{
		val Tick = "timeToReport"
		import context.dispatcher
		import scala.concurrent.duration._
		
		var numWorkers: Option[Int] = None
		context.system.scheduler.schedule(1.second, 10.second, self, Tick)
		def receive = {
			case Tick => 
				numWorkers.foreach(n => log.info("{} workers in the cluster", n))
				numWorkers = None
			case NumWorkers(n) => numWorkers = Some(n)
		}
	}))
	
	val cluster = Cluster(context.system)
  	override def preStart(): Unit = cluster.subscribe(self, classOf[ClusterDomainEvent])
	override def postStop(): Unit = {
		cluster.unsubscribe(self)
		log.info("Post stop")
	}
	
	val workers = collection.mutable.Set.empty[ActorRef]
	val workerPath = Seq("user", "workerroot")
	
	def attemptWorkerHandshake(m: Member){
//		implicit val timeout = Timeout(5.seconds)
//		val workerCandidate = Await.result(
//				context.system
//				.actorSelection(RootActorPath(m.address) / workerPath)
//				.resolveOne,
//				timeout.duration
//		)
		
		val path = RootActorPath(m.address) / workerPath
		context.system.actorSelection(path) ! StatusRequest
				
//		workerCandidate 
		log.info("Attempting handshake with potential worker {}", path)
	}
	
	def receive = {
		case state: CurrentClusterState => 
  		  	state.members.filter(_.status == MemberStatus.Up).foreach(attemptWorkerHandshake)
  		case MemberUp(m) => 
  		  	log.debug("Member {} is up", m)
  		  	attemptWorkerHandshake(m)
  		case MemberRemoved(member, previousStatus) =>
  			val down = workers.filter(_.path.address == member.address)
  			workers --= down
  		  	log.info(s"Worker down {}, previous status", down, previousStatus)
  		  	reportingActor ! NumWorkers(workers.size)
  		case WorkerBusy =>
  		  	workers += sender 
  		  	reportingActor ! NumWorkers(workers.size)
  		case WorkerIdle =>
  			log.info("{} is IDLE", sender)
  			workers += sender
  			reportingActor ! NumWorkers(workers.size)
  			context.parent.forward(WorkerIdle)
		
  		case Broadcast(msg) => 
			log.info("Broadcasting ({}) to {} known workers", msg, workers.size)
			workers.foreach(_.forward(msg))
	}
}