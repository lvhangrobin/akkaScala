package com.actor

import akka.actor._
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com._
import net.liftweb.json.{DefaultFormats, Extraction}
import net.liftweb.json

import scala.collection.JavaConversions._
import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}
import java.util.concurrent.TimeUnit

class StatsActor extends Actor with Stash with ActorLogging{

  import StatsActor._
  import context.dispatcher

  var sessions: Map[Session, ActorRef] = Map.empty
  var currentSystemTime: SystemTime = _
  var stats: Stats = Stats(List.empty)

  var realTimeStats: Map[Session, (Url, Browser)] = Map.empty

  implicit val formats = DefaultFormats
  implicit val askTimeout = Timeout(5, TimeUnit.SECONDS)

  private val loggingActor = createLoggingActor()

  private[actor] val persistentFilePath = "./persistence.log"

  private[actor] def createLoggingActor() = {
    context.actorOf(LoggingActor.props, "logging-actor")
  }

  override def receive: Receive = {
    case Request(session, timestamp, url) if sessions.contains(session) =>
      val sessionProxyActor = sessions(session)
      sessionProxyActor ! Request(session, timestamp, url)

    case r@Request(session, timestamp, url) =>
      val sessionProxyActor = creatSessionProxyActor()
      sessions += session -> sessionProxyActor
      sessionProxyActor ! r

    case t@SystemTime(timestamp) =>
      currentSystemTime = t
      context.children.foreach(_ ! t)
      if (currentSystemTime.timestamp / 1000 % 5 == 0)
        writeToFile()

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
  }

  def creatSessionProxyActor() = {
    context.actorOf(SessionProxyActor.props)
  }

  def processingRealTimeStats(originalSender: ActorRef): Receive = {
    case RealTimeResponse(session, url, browser) =>
      realTimeStats += session -> (url, browser)
      if (maybeRealTimeStatsReady(originalSender))
        unstashAll()
        context.become(receive)

    case Status.Failure(e) =>
      self ! RealTimeResponse(Session(0), "", "")

    case _ =>
      stash()
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

  private[actor] def writeToFile(readyToSerialize: Stats = stats) = {
    val writer = new PrintWriter(new FileWriter(persistentFilePath))
    try{
      val out = json.compactRender(Extraction.decompose(readyToSerialize))
      writer.print(out)
    } finally writer.close()
  }

  private[actor] def recoverStats() = {

    val persistentFile = new File(persistentFilePath)
    val filePath = Paths.get(persistentFilePath)
    val input = Files.readAllLines(filePath).mkString("")
    if (persistentFile.exists()) {
      stats = json.parse(input).extract[Stats]
    }
  }

  override def preRestart(t: Throwable, message: Option[Any]) = {
    loggingActor ! Retry(t)
  }

  override def postRestart(t: Throwable) = {
    recoverStats()
  }
}

object StatsActor {

  type Url = String

  type Browser = String

  def props = Props(new StatsActor)
}
