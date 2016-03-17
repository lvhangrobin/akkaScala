package com.actor

import akka.actor.{Actor, ActorRef, Props}
import com._
import net.liftweb.json.{DefaultFormats, Extraction}
import net.liftweb.json

import scala.collection.JavaConversions._
import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}

class StatsActor extends Actor {

  var sessions: Map[Session, ActorRef] = Map.empty
  var currentSystemTime: SystemTime = _
  var stats: Stats = Stats(List.empty)

  implicit val formats = DefaultFormats

  private val loggingActor = createLoggingActor()

  private[actor] val persistentFilePath = "./persistence.log"

  private[actor] def createLoggingActor() = {
    context.actorOf(LoggingActor.props, "logging-actor")
  }

  override def receive: Receive = {
    case Request(session, timestamp, url) if sessions.contains(session) =>
      val sessionActor = sessions(session)
      sessionActor ! Request(session, timestamp, url)

    case r@Request(session, timestamp, url) =>
      val sessionActor = context.actorOf(SessionActor.props)
      sessions += session -> sessionActor
      sessionActor ! r

    case t@SystemTime(timestamp) =>
      currentSystemTime = t
      context.children.foreach(_ ! t)
      if (currentSystemTime.timestamp / 1000 % 5 == 0)
        writeToFile()

    case InactiveSession(requests, sessionActorRef) =>
      sessions.find{case (_, actorRef) => actorRef == sessionActorRef} match {
        case Some((session, _)) =>
          stats = stats.copy(stats.userRequests ++ requests)
          sessions -= session
          context.stop(sessionActorRef)

        case None => throw new IllegalStateException(s"$sessionActorRef cannot be found")
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
  def props = Props(new StatsActor)
}
