/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale

import se.scalablesolutions.akka.actor.{ActorRef, ActorRegistry}
import com.vasilrem.akka.easyscale.RegistryActorUtil._

/**
 * Publishes all local actors as remote references to the linked registry
 * when the registry link is added
 */
trait StartupActorRefsDistribution extends RegistryActor{

  /**
   * Adds link to remote registry, and register all local actors at there
   */
  protected override def addRegistryLink(link: RegistryLink) = {
    super.addRegistryLink(link)
    registerActorsAt(linkedRegistries.get(link))
  }


  /**
   * Registers all local actors at the remote node
   */
  private def registerActorsAt(remoteRegistry: ActorRef) = {
    ActorRegistry.filter(actor =>
      actor.id != REGISTRY_ACTOR &&
      isActorLocal(actor))
    .foreach{actor => remoteRegistry ! RegisterActor(actor)}
    remoteRegistry
  }

}
