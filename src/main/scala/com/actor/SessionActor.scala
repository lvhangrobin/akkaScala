package com.actor

import akka.actor.{Props, Actor}
import com.{InactiveSession, SystemTime, Request}


class SessionActor extends Actor {

  var requests: List[Request] = List.empty

  override def receive: Receive = {
    case r@Request(session, timestamp, url) =>
      requests = (requests :+ r)

    case SystemTime(timestamp) =>
      val diff = timestamp - getLastVisitTime()
      if (diff >= 5 * 60 * 1000)
        sender() ! InactiveSession(requests, self)

  }

  def getLastVisitTime() = requests.last.timestamp
}

object SessionActor {

  def props = Props(new SessionActor)
}
