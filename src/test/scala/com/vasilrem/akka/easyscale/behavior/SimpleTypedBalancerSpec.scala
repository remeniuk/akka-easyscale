/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale.behavior

import com.vasilrem.akka.easyscale.behavior._
import org.specs.Specification
import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.actor.ActorRegistry
import se.scalablesolutions.akka.dispatch.Futures
import com.vasilrem.akka.easyscale.dummy._

class SimpleTypedBalancerSpec extends Specification{

  doBeforeSpec{
    (1 to 3).foreach(_ => actorOf[SimpleActor].start)
    actorOf(new SimpleTypedBalancer[SimpleActor]).start
  }

  "Messages sent to the balancer should be distributed across workers" in {
    val balancer = ActorRegistry.actorsFor(classOf[SimpleTypedBalancer[SimpleActor]]).head
    val futures = (1 to 3).map(i => balancer !!! "hi").toList
    Futures.awaitAll(futures)
    futures.map(future => future.result).flatten.toSet.size must be equalTo(3)
  }

  doAfterSpec{
    ActorRegistry.actorsFor(classOf[SimpleTypedBalancer[SimpleActor]]).foreach(_.stop)
    ActorRegistry.actorsFor(classOf[SimpleActor]).foreach(_.stop)
  }

}
