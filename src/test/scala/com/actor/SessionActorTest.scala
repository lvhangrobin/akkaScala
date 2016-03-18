package com.actor

import akka.testkit.{TestProbe, TestActorRef, TestActor}
import com.{InitiateChat, SystemTime, InactiveSession, Session, BaseAkkaSpec}
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

    "initiate chat to chatActor" in {
      val chatActor = TestProbe()
      val sessionActor = TestActorRef[SessionActor](new SessionActor {
        override def createChatActor() = chatActor.ref
      })

      val session = new Session(20)
      val requests = session.requests
      val onHelpPage = requests.last.copy(url = "/help")
      val newRequests = requests :+ onHelpPage
      val after2Mins = onHelpPage.timestamp + 2 * 60 * 1000

      sessionActor.underlyingActor.requests = newRequests

      sessionActor ! SystemTime(after2Mins)
      chatActor.expectMsg(InitiateChat(session, after2Mins))
    }
  }
}
