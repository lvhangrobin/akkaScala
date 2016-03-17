package com.actor

import akka.actor.{Props, ActorRef, Actor}
import com.{InactiveSession, SystemTime, Request, Session, Stats}

class StatsActor extends Actor {

  var sessions: Map[Session, ActorRef] = Map.empty
  var currentSystemTime: SystemTime = _
  var stats: Stats = Stats(List.empty)

  override def receive: Receive = {
    case Request(session, timestamp, url) if sessions.contains(session) =>
      val sessionActor = sessions(session)
      sessionActor ! Request(session, timestamp, url)

    case Request(session, timestamp, url) =>
      val sessionActor = context.actorOf(SessionActor.props)
      sessions += session -> sessionActor

    case t@SystemTime(timestamp) =>
      currentSystemTime = t
      context.children.foreach(_ ! t)

    case InactiveSession(requests, sessionActorRef) =>
      sessions.find{case (_, actorRef) => actorRef == sessionActorRef} match {
        case Some((session, _)) =>
          stats = stats.copy(stats.userRequests ++ requests)
          sessions -= session
          context.stop(sessionActorRef)

        case None => ()
      }



  }
}

object StatsActor {
  def props = Props(new StatsActor)
}
