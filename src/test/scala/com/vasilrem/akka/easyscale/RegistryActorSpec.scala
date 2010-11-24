/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale

import org.specs._
import se.scalablesolutions.akka.remote.RemoteServer
import se.scalablesolutions.akka.actor._
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.remote.{RemoteServer, RemoteNode, RemoteClient}
import se.scalablesolutions.akka.util.Logging
import com.vasilrem.akka.easyscale.boot._
import RegistryActorUtil._
import com.vasilrem.akka.easyscale.dummy._

class RegistryActorSpec extends Specification with Logging{

  doBeforeSpec{
    new Boot
  }
  
  "Registry actors" should{
    "be able to link to other registry actors and communicate with remote actors" in {
      (ActorRegistry.filter(actor =>
          Class.forName(actor.actorClassName)
          .isAssignableFrom(classOf[MyActor])
        ).head !! "dummy") must beSome("received message [dummy]")
    }

  }

  doAfterSpec{
    log.info("====SHUTTING DOWN ACTOR REGISTRY====")
    log.info("Local registry instance: " + RegistryActorInstance)
    for(instance <- RegistryActorInstance)
      instance !! ActorUnregistered(instance)
  }

}
