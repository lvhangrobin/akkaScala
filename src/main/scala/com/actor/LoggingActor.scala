package com.actor

import akka.actor.{Props, ActorLogging, Actor}
import com.Retry

class LoggingActor extends Actor with ActorLogging{

  private val threshold: Int = 5
  private[actor] var numOfRetries = 0

  override def receive: Receive = {
    case Retry(t) if numOfRetries >= threshold =>
      numOfRetries = 0
      log.error(t, "statsActor failed too many times! FIX IT!!!")
    case Retry(t) =>
      numOfRetries += 1
  }
}

object LoggingActor {
  def props = Props(new LoggingActor)
}
