/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale

import se.scalablesolutions.akka.actor.{Uuid, LocalActorRef, ActorRegistry, ActorRegistered, ActorUnregistered, ActorRef}
import se.scalablesolutions.akka.remote.RemoteNode
import com.vasilrem.akka.easyscale.RegistryActorUtil._

/**
 * Publishes all registered local actor as a remote ref on all
 * linked remote registries
 */
trait InlifeActorRefsDistribution extends RegistryActor{

  override def specificMessageHandler = {
    case ActorRegistered(actor) =>
      log.debug("Actor [%s] is registered" format(actor))
      registerOnLinks(actor)
      
    case ActorUnregistered(actor) =>
      log.debug("Actor [%s] is unregistered" format(actor))
      if(isActorLocal(actor)) 
        actor.id match {
          case REGISTRY_ACTOR => ActorRegistry.foreach(act =>
              if(act.getClass.isAssignableFrom(classOf[LocalActorRef]))
                unregisterOnLinks(act))
              case _ => unregisterOnLinks(actor)
            }
        }


      /**
       * Makes the actor remote, and registers at remote nodes
       */
      private def registerOnLinks(actor: ActorRef) =
        if(isActorLocal(actor)){          
          log.debug("Registering %s remotely" format(actor.uuid.toString))
          RemoteNode.register(actor.uuid.toString, actor)
          val iterator = linkedRegistries.values.iterator
          while(iterator.hasNext) iterator.next ! RegisterActor(actor)
        }

      /**
       * Unregister the actor from remote nodes
       */
      private def unregisterOnLinks(actor: ActorRef) = {        
        val iterator = linkedRegistries.values.iterator
        while(iterator.hasNext) {
          val remoteRegistry = iterator.next
          log.debug("Unregistering [%s] from remote node %s" format(actor, remoteRegistry))
          remoteRegistry ! UnregisterActor(actor)
        }
      }

  }
