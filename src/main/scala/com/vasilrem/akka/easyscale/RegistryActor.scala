/**
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale

import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import se.scalablesolutions.akka.actor.{Uuid, Actor, ActorRef, ActorRegistry, LocalActorRef}
import se.scalablesolutions.akka.actor.Actor._
import se.scalablesolutions.akka.remote.{RemoteClient, RemoteNode, RemoteServer}
import se.scalablesolutions.akka.config.Config.config

object RegistryActorUtil {

  val REGISTRY_ACTOR = "reg-actor"

  /**
   * RegistryActor running on a curent node
   */
  def RegistryActorInstance = ActorRegistry.actorsFor(REGISTRY_ACTOR).lastOption

  /**
   * Makes registry actor accessible remotely, and adds it as a listener to 
   * local actor registry
   */
  def initialize = for(registryActor <- RegistryActorInstance){
    log.info("Adding RegistryActor as listener to local actor registry")
    ActorRegistry.addListener(registryActor)

    log.info("Making RegistryActor remote...")
    RemoteNode.register(registryActor)

    log.info("Adding link to a neighbouring host...")
    val neightbourHost = config.getString("akka.remote.neighbour.hostname", "localhost")
    val neightbourPort = config.getInt("akka.remote.neighbour.port", 10000)
    try{
      new Socket().connect(new InetSocketAddress(neightbourHost, neightbourPort))
      RegistryActorInstance.map(_ ! AddRegistryLink(RegistryLink(port = neightbourPort, host = neightbourHost)))
    } catch {
      case ex:ConnectException => log.error("Remote registry is down!" format())
    }
  }

}

import RegistryActorUtil._

/**
 * Link to the actor accessible remotely
 */
case class ActorLink(uuid: String, className: String, hostname: String, port: Int)
/**
 * Registers reference to the actor (RemoteActorRef) at remote registry,
 * so that the reference can be picked from the registry just like the other
 * local actors
 */
case class RegisterActor(actor: ActorLink)
/**
 * Unregisters reference to the actor from remote registry
 */
case class UnregisterActor(actor: ActorLink)
/**
 * Link to the registry actor
 */
case class RegistryLink(host: String = RemoteServer.HOSTNAME, port: Int = RemoteServer.PORT, name: String = REGISTRY_ACTOR)
/**
 * Adds registry actor to the list of links on a remote node
 */
case class AddRegistryLink(link: RegistryLink)
/**
 * Removes registry actor from the list of links on a remote node
 */
case class RemoveRegistryLink(link: RegistryLink)

/**
 * RegistryActor allows remote nodes to access registry (register remote
 * actors) on this node
 */
class RegistryActor extends Actor{

  if(RegistryActorInstance.isDefined)
    throw new InstantiationException("Registry actor already exists on this node." +
                                     " Only one instance per node is allowed.")

  self.id = REGISTRY_ACTOR

  /**
   * RegistryActors located on the other hosts
   */
  protected[easyscale] val linkedRegistries = new ConcurrentHashMap[RegistryLink, ActorRef]()

  /**
   * Implicitly creates actor link from the reference
   */
  implicit def actorToLink(actor: ActorRef) =
    ActorLink(actor.uuid.toString, actor.actorClassName, RemoteServer.HOSTNAME, RemoteServer.PORT)

  def receive = defaultMessageHandler orElse specificMessageHandler

  def defaultMessageHandler: PartialFunction[Any, Unit] = {
    case RegisterActor(actor) =>
      log.debug("Registering remote actor [%s]" format(actor))
      if(!isActorInRegistry(actor.uuid) && !isLinkToLocal(actor))
        ActorRegistry.register( // Hack for 0.10, 1.0-M1
          RemoteClient.actorFor(actor.uuid.toString, actor.className, actor.hostname, actor.port)
        ) // RemoteActorRefs will register themselves in 1.0-M1+

    case UnregisterActor(actor) => {
        log.debug("Unregistering remote actor [%s]" format(actor))
        ActorRegistry.foreach{act =>
          if(act.uuid == actor.uuid){
            ActorRegistry.unregister(act)            
          }}
        Option(linkedRegistries.get(RegistryLink(actor.hostname, actor.port))) match{
          case Some(_) => removeLinkToRegistry(RegistryLink(actor.hostname, actor.port))
          case None => log.debug("[%s] is not a registry link" format(actor.uuid))
        }
      }

    case AddRegistryLink(link) => 
      if(!linkedRegistries.containsKey(link))
        addRegistryLink(link)
      else
        log.debug("Link to registry [%s] is already present" format(link))
     
      
    case RemoveRegistryLink(link) => {
        log.debug("Unlinking from registry [%s]" format(link))
        linkedRegistries.remove(link)
      }
      
  }

  def specificMessageHandler: PartialFunction[Any, Unit] = {
    case message => log.error("Unknown message [%s] was received" format(message))
  }

  /**
   * Adds link to remote registry, and register all local actors at there
   */
  protected def addRegistryLink(link: RegistryLink) = {
    log.debug("Adding link to remote registry [%s]" format(link))
    val remoteRegistry = RemoteClient.actorFor(link.name.toString, link.host, link.port)
    log.debug("Letting remote registry know about the other linked registries...")
    val iterator = linkedRegistries.keySet.iterator
    while(iterator.hasNext) remoteRegistry ! iterator.next
    remoteRegistry ! AddRegistryLink(RegistryLink())
    linkedRegistries.put(link, remoteRegistry)
  }

  /**
   * Checks if link refers to local actor
   */
  protected def isLinkToLocal(link: ActorLink) = link.hostname == RemoteServer.HOSTNAME &&
  link.port == RemoteServer.PORT

  /**
   * Checks if the actor is local
   */
  protected def isActorLocal(actor: ActorRef) = 
    actor.getClass.isAssignableFrom(classOf[LocalActorRef])
  
  /**
   * Gets the actors from local registry by ID
   */
  protected[easyscale] def isActorInRegistry(actorUuid: String) =
    ActorRegistry.filter(actor => actor.id == actorUuid.toString).length > 0

  /**
   * Removes link to remote registry, and shuts down connection to
   * remote host
   */
  private def removeLinkToRegistry(link: RegistryLink) = {
    log.debug("Removing remote registry link: " + link)
    linkedRegistries.remove(link)
    log.debug("Shutting down remote client connection...")
    RemoteClient.shutdownClientFor(new InetSocketAddress(link.host, link.port))
  }
 

}

