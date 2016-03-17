package com.actor

import akka.testkit.{TestProbe, TestActorRef, TestActor}
import com.{SystemTime, InactiveSession, Session, BaseAkkaSpec}
import scala.concurrent.duration._

class SessionActorTest extends BaseAkkaSpec{

  "SessionActor" should {
    "detect inactive user" in {
      val statsActor = TestProbe()
      implicit val _ = statsActor.ref
      val sessionActor = TestActorRef[SessionActor](new SessionActor)
      val requests = new Session(20).requests
      val lastRequest = requests.last
      sessionActor.underlyingActor.requests = requests

      sessionActor ! SystemTime(lastRequest.timestamp + 5 * 60 * 1000)
      statsActor.expectMsg(InactiveSession(requests, sessionActor))
    }
  }
}
