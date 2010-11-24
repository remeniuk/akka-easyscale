/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale.boot

import com.vasilrem.akka.easyscale.behavior.{IsReady, Ready}
import com.vasilrem.akka.easyscale.{RegistryActor, RegistryActorUtil, StartupActorRefsDistribution, InlifeActorRefsDistribution}
import se.scalablesolutions.akka.actor.Actor
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.remote.{RemoteServer, RemoteNode}
import se.scalablesolutions.akka.util.Logging
import com.vasilrem.akka.easyscale.dummy._

class Boot extends Logging{

  println("====STARTING REMOTE NODE====")
  RemoteNode.start

  log.info("Starting registry actor at %s:%s" format(RemoteServer.HOSTNAME, RemoteServer.PORT))
  val registryActor = actorOf(new RegistryActor 
              with StartupActorRefsDistribution
              with InlifeActorRefsDistribution).start

  RegistryActorUtil.initialize

}

object Run extends Application{
  override def main(args : Array[java.lang.String]):Unit = {
    new Boot
    actorOf[MyActor].start
    (1 to 3).foreach(_ => actorOf[SimpleActor].start)
  }
}


