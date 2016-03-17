package com.actor

import akka.actor.{Props, ActorRef, Actor}
import akka.dispatch.sysmsg.Terminate
import com._

class StatsActor extends Actor {

  var sessions: Map[Session, ActorRef] = Map.empty
  var currentSystemTime: SystemTime = _
  var stats: Stats = Stats(List.empty)

  private val loggingActor = createLoggingActor()

  private[actor] def createLoggingActor() = {
    context.actorOf(LoggingActor.props, "logging-actor")
  }

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

        case None => throw new IllegalStateException(s"$sessionActorRef cannot be found")
      }
  }

  override def preRestart(t: Throwable, message: Option[Any]) = {
    loggingActor ! Retry(t)
  }

}

object StatsActor {
  def props = Props(new StatsActor)
}
