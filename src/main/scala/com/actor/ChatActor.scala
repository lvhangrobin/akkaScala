package com.actor

import akka.actor.{Props, Actor, ActorLogging}
import com.InitiateChat


class ChatActor extends Actor with ActorLogging{

  override def receive: Receive = {
    case InitiateChat(session, timestamp) =>
      log.info(s"start chat with ${session.id} at $timestamp")
  }
}

object ChatActor{
  def props = Props(new ChatActor)
}
