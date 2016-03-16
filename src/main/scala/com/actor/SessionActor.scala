package com.actor

import akka.actor.{Props, Actor}
import com.Request


class SessionActor extends Actor {

  var requests: List[Request] = List.empty

  override def receive: Receive = {
    case r@Request(session, timestamp, url) =>
      requests = (requests :+ r)
  }
}

object SessionActor {

  def props = Props(new SessionActor)
}
