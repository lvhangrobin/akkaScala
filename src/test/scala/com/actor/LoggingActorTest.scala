package com.actor

import akka.testkit.{TestProbe, EventFilter, TestActorRef}
import com.{Retry, BaseAkkaSpec}


class LoggingActorTest extends BaseAkkaSpec{


  "LoggingActor" should {
    "log to error after five retries" in {
      val loggingActor = TestActorRef[LoggingActor](new LoggingActor)
      loggingActor.underlyingActor.numOfRetries = 5

      loggingActor ! Retry(new Exception)

      loggingActor.underlyingActor.numOfRetries shouldEqual 0
    }

    "increment retries if they are less than 5" in {
      val loggingActor = TestActorRef[LoggingActor](new LoggingActor)
      loggingActor.underlyingActor.numOfRetries = 3

      loggingActor ! Retry(new Exception)

      loggingActor.underlyingActor.numOfRetries shouldEqual 4
    }
  }
}
