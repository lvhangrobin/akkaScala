package com.actor

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com._
import com.actor.DatabaseActor.DatabaseFailureException
import java.util.concurrent.TimeUnit

class StatsActor extends Actor with Stash with ActorLogging{

  import StatsActor._
  import context.dispatcher

  var sessions: Map[Session, ActorRef] = Map.empty
  var currentSystemTime: SystemTime = _
  var stats: Stats = Stats(List.empty)

  var realTimeStats: Map[Session, (Url, Browser)] = Map.empty
  
  implicit val askTimeout = Timeout(5, TimeUnit.SECONDS)

  private val loggingActor = createLoggingActor()
  private val databaseActor = createDatabaseActor()

  override val supervisorStrategy: SupervisorStrategy = {
    val decider: Decider = {
      case e@DatabaseFailureException(m) =>
        self ! Status.Failure(e)
        Restart
    }

    OneForOneStrategy() { decider orElse super.supervisorStrategy.decider }
  }

  private[actor] def createLoggingActor() = {
    context.actorOf(LoggingActor.props, "logging-actor")
  }

  private[actor] def createSessionProxyActor() = {
    context.actorOf(SessionProxyActor.props)
  }

  private[actor] def createDatabaseActor() = {
    context.actorOf(DatabaseActor.props)
  }

  override def receive: Receive = recoverStats

  def recoverStats: Receive = {
    case RetrievedData(retrievedStats) =>
      stats = retrievedStats
      context.become(processingRequests)
      unstashAll()

    case Status.Failure(e) =>
      stats = Stats(List.empty)
      context.become(processingRequests)
      unstashAll()

    case _ =>
      stash()
  }

  def processingRequests: Receive = {
    case Request(session, timestamp, url) if sessions.contains(session) =>
      val sessionProxyActor = sessions(session)
      sessionProxyActor ! Request(session, timestamp, url)

    case r@Request(session, timestamp, url) =>
      val sessionProxyActor = createSessionProxyActor()
      sessions += session -> sessionProxyActor
      sessionProxyActor ! r

    case t@SystemTime(timestamp) =>
      currentSystemTime = t
      context.children.foreach(_ ! t)
      if (currentSystemTime.timestamp / 1000 % 5 == 0)
        databaseActor ! StoreData(stats)

    case RealTimeStatsRequest =>
      sessions.values.foreach(r => (r ? RealTimeStats) pipeTo self)
      context.become(processingRealTimeStats(sender()))
      realTimeStats = Map.empty

    case InactiveSession(requests, sessionActorRef) =>
      sessions.find{case (_, actorRef) => actorRef == sessionActorRef} match {
        case Some((session, _)) =>
          stats = stats.copy(stats.userRequests ++ requests)
          sessions -= session
          context.stop(sessionActorRef)

        case None => throw new IllegalStateException(s"$sessionActorRef cannot be found")
      }

    case RequestRejected(session) =>
      log.warning(s"Cannot forward request to ${session.id} for the moment. ${session.id} is having too many requests!")

    case Status.Failure(t: DatabaseFailureException) =>
      dealWithError(t)
  }

  def processingRealTimeStats(originalSender: ActorRef): Receive = {
    case RealTimeResponse(session, url, browser) =>
      realTimeStats += session -> (url, browser)
      if (maybeRealTimeStatsReady(originalSender)){
        unstashAll()
        context.become(receive)
      }

    case Status.Failure(e) =>
      self ! RealTimeResponse(Session(0), "", "")

    case _ =>
      stash()
  }

  def dealWithError(t: Throwable) = {
    log.error(t, "Receive failure from database actor!")
  }

  def maybeRealTimeStatsReady(originalSender: ActorRef): Boolean = {
    if (realTimeStats.size == sessions.size) {
      val totalNumberOfUsers =
        realTimeStats.size
      val numOfUsersPerUrl: Map[Url, Int] =
        realTimeStats.toList.groupBy(_._2._1).mapValues(_.size).filterNot(_._1 == "")
      val numOfUsersPerBrowser: Map[Browser, Int] =
        realTimeStats.toList.groupBy(_._2._2).mapValues(_.size).filterNot(_._1 == "")

      originalSender ! RealTimeStatsResponse(totalNumberOfUsers, numOfUsersPerUrl, numOfUsersPerBrowser)
      true

    } else {
      false
    }
  }

  override def preRestart(t: Throwable, message: Option[Any]) = {
    loggingActor ! Retry(t)
  }

  override def postRestart(t: Throwable) = {
    databaseActor ? RetrieveData pipeTo self
  }
}

object StatsActor {

  type Url = String

  type Browser = String

  def props = Props(new StatsActor)
}
