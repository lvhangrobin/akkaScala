package com.actor

import akka.actor.{Props, Actor}
import com.actor.DatabaseActor.DatabaseFailureException
import com.{RetrievedData, RetrieveData, Stats, StoreData}
import net.liftweb.json
import net.liftweb.json.{DefaultFormats, Extraction}

import scala.util.Random
import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.collection.JavaConversions._


class DatabaseActor extends Actor{

  private[actor] val persistentFilePath = "./persistence.log"
  implicit val formats = DefaultFormats
  private[actor] val failureRate = 1

  override def receive: Receive = {
    case StoreData(stats) =>
      maybeFailed(writeToFile(stats))

    case RetrieveData =>
      maybeFailed(sender() ! RetrievedData(recoverStats()))
  }

  private[actor] def writeToFile(readyToSerialize: Stats): Unit = {
    val writer = new PrintWriter(new FileWriter(persistentFilePath))
    try{
      val out = json.compactRender(Extraction.decompose(readyToSerialize))
      writer.print(out)
    } finally writer.close()
  }

  private[actor] def recoverStats(): Stats = {

    val persistentFile = new File(persistentFilePath)
    val filePath = Paths.get(persistentFilePath)
    val input = Files.readAllLines(filePath).mkString("")
    if (persistentFile.exists()) {
      json.parse(input).extract[Stats]
    } else Stats(List.empty)
  }

  private[actor] def maybeFailed(f: => Unit, failureRate: Double = failureRate) = {
    val rand = new Random().nextInt(100)
    if (rand < failureRate) throw new DatabaseFailureException("Random failure happens!")
    else f
  }
}

object DatabaseActor {

  case class DatabaseFailureException(message: String) extends RuntimeException(message)
  def props = Props(new DatabaseActor)
}
