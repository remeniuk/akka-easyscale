/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale.behavior

import com.vasilrem.akka.easyscale.behavior._
import org.specs.Specification
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.ActorRegistry
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.dispatch.Futures
import se.scalablesolutions.akka.remote.RemoteNode
import com.vasilrem.akka.easyscale._
import com.vasilrem.akka.easyscale.dummy._
import com.vasilrem.akka.easyscale.boot._
import RegistryActorUtil._

class RemoteTypedBalancerSpec extends Specification{

  doBeforeSpec{
    new Boot
    (1 to 3).foreach(_ => actorOf[SimpleActor].start)
    actorOf(new SimpleTypedBalancer[SimpleActor]).start
  }

  "Messages sent to the balancer should be distributed across local and remote workers" in {

    val balancer = ActorRegistry.filter(actor =>
      Class.forName(actor.actorClassName)
      .isAssignableFrom(classOf[SimpleTypedBalancer[SimpleActor]])
    ).head

    log.info("========SENDING MESSAGES TO BALANCER=========")
    val start = System.currentTimeMillis
    val futures = (1 to 30).map(i => balancer !!! "" + i).toList
    log.info("All messages are disaptched...")
    Futures.awaitAll(futures)
    val processedByWorkers = futures.flatMap(future => future.result).toSet.size
    log.info("Process time by %s workers: %s" format(processedByWorkers, System.currentTimeMillis - start))
    processedByWorkers must beGreaterThan(3)
  }

  doAfterSpec{
    log.info("====SHUTTING DOWN ACTOR REGISTRY====")
    for(instance <- RegistryActorInstance) {      
      instance !! ActorUnregistered(instance)
      RemoteNode.shutdown
    }
  }

}
