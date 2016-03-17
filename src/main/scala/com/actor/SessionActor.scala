package com.actor

import akka.actor.{Props, Actor}
import com.{InitiateChat, InactiveSession, SystemTime, Request}


class SessionActor extends Actor {

  var requests: List[Request] = List.empty

  override def receive: Receive = {
    case r@Request(session, timestamp, url) =>
      requests = (requests :+ r)

    case SystemTime(timestamp) =>
      val diff = timestamp - getLastVisitTime()
      if (diff >= 2 * 60 * 1000 && requests.last.url == "/help" && context.children.size == 0){
        createChatActor() ! InitiateChat(requests.last.session, timestamp)
      }

      if (diff >= 5 * 60 * 1000){
        sender() ! InactiveSession(requests, self)
      }
  }

  private[actor] def createChatActor() =
    context.actorOf(ChatActor.props, "chat-actor")

  def getLastVisitTime() = requests.last.timestamp
}

object SessionActor {

  def props = Props(new SessionActor)
}
