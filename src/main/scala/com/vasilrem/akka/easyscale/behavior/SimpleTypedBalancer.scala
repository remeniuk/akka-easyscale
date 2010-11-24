/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale.behavior

import se.scalablesolutions.akka.actor.{Uuid, Actor, ActorRef, ActorRegistry}
import se.scalablesolutions.akka.dispatch.Futures

case class IsReady()
case class Ready(actorUuid: Uuid)

class SimpleTypedBalancer[T](implicit manifest: Manifest[T]) extends Actor{

  def receive = {
    case message :AnyRef =>
      forwardMessage(message,
                     self.getSenderFuture orElse self.getSender,
                     idleWorkerId)      
  }

  def idleWorkerId = Futures.awaitOne{
    ActorRegistry.filter{actor =>
      Class.forName(actor.actorClassName).isAssignableFrom(manifest.erasure)
    }.map(_ !!! IsReady()).toList
  }.result.flatMap(_ match {
      case Ready(actorUuid) =>        
        Option(actorUuid)
      case _ =>
        None        
    })

  def forwardMessage(message: AnyRef, originalSender: Option[AnyRef], workerId: Option[Uuid]) =
  {
    for{id <- workerId; worker <- ActorRegistry.actors.find(actor =>
        actor.id == id.toString ||
        actor.uuid == id )}{
      if(originalSender.isDefined)
        worker.forward(message)
      else worker.sendOneWay(message)
    }
  }

}
