package com.actor

import akka.actor.{Props, Actor}
import com.{RetrievedData, RetrieveData, Stats, StoreData}
import net.liftweb.json
import net.liftweb.json.{DefaultFormats, Extraction}

import java.io.{File, FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.collection.JavaConversions._


class DatabaseActor extends Actor{

  private[actor] val persistentFilePath = "./persistence.log"
  implicit val formats = DefaultFormats

  override def receive: Receive = {
    case StoreData(stats) =>
      writeToFile(stats)

    case RetrieveData =>
      sender() ! RetrievedData(recoverStats())
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
}

object DatabaseActor {
  def props = Props(new DatabaseActor)
}
