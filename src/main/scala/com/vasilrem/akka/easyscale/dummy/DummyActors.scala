/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.vasilrem.akka.easyscale.dummy

import com.vasilrem.akka.easyscale.behavior._
import se.scalablesolutions.akka.actor.Actor

class MyActor extends Actor {
  def receive = {    
    case message: String => self.reply_?("received message [%s]" format(message))
    case IsReady() => self.reply_?(Ready(self.uuid))
  }
}

class SimpleActor extends Actor {
  def receive = {
    case message: String =>
      Thread.sleep(500)      
      self.reply_?("[%s] received the message" format(self.uuid))
    case IsReady() =>
      self.reply(Ready(self.uuid))
  }
}