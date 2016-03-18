package com.actor

import akka.actor.{Props, Actor}
import com.{RequestRejected, RealTimeStatsRequest, SystemTime, Request}


class SessionProxyActor extends Actor{

  var numOfRequestsPerSecond = 0
  private val sessionActor = createSessionActor()
  private[actor] val maxRequestsPerSecond = context.system.settings.config.getInt("session-proxy-actor.max-requests")

  override def receive: Receive = {

    case Request(session, timestamp, url) if numOfRequestsPerSecond >= maxRequestsPerSecond =>
      sender() ! RequestRejected(session)

    case r@Request(session, timestamp, url) =>
      numOfRequestsPerSecond += 1
      sessionActor forward r

    case t@SystemTime(timestamp) =>
      numOfRequestsPerSecond = 0
      sessionActor forward t

    case message =>
      sessionActor forward message

  }

  def createSessionActor() =
    context.actorOf(SessionActor.props)
}

object SessionProxyActor {
  def props = Props(new SessionProxyActor)
}



