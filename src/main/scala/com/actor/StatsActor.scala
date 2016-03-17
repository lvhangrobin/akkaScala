package com.actor

import akka.actor.{Props, ActorRef, Actor}
import com.{Request, Session}

class StatsActor extends Actor {

  var sessions: Map[Session, ActorRef] = Map.empty

  override def receive: Receive = {
    case Request(session, timestamp, url) if sessions.contains(session) =>
      val sessionActor = sessions(session)
      sessionActor ! Request(session, timestamp, url)

    case Request(session, timestamp, url) =>
      val sessionActor = context.actorOf(SessionActor.props)
      sessions += session -> sessionActor
  }
}

object StatsActor {
  def props = Props(new StatsActor)
}
