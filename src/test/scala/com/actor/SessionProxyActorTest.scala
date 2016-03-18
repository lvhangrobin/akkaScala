package com.actor

import akka.testkit.{TestProbe, TestActorRef}
import com.{SystemTime, RequestRejected, Session, BaseAkkaSpec}


class SessionProxyActorTest extends BaseAkkaSpec{

  "SessionProxyActor" should {

    "forward requests if not reaching the limit" in {

      val sessionActor = TestProbe()
      val sessionProxyActor = TestActorRef[SessionProxyActor](new SessionProxyActor{
        override def createSessionActor() = sessionActor.ref
      })

      val statsActor = TestProbe()
      implicit val _ = statsActor.ref

      val session = Session(5)
      val requestsInFirstSecond = List.fill(5)(session.requests.head)
      val requestForNextSecond = session.requests.drop(1).head
      val systemTime = SystemTime(requestsInFirstSecond.last.timestamp)

      requestsInFirstSecond.foreach(sessionProxyActor ! _)
      statsActor.expectNoMsg()
      requestsInFirstSecond.foreach(m => sessionActor.expectMsg(m))

      sessionProxyActor ! systemTime
      sessionProxyActor ! requestForNextSecond

      sessionProxyActor.underlyingActor.numOfRequestsPerSecond shouldEqual 1
      sessionActor.expectMsg(systemTime)
      sessionActor.expectMsg(requestForNextSecond)
    }

    "reject the incoming requests if it reaches the limit" in {
      val sessionProxyActor = TestActorRef[SessionProxyActor](new SessionProxyActor{
        override def createSessionActor() = TestProbe().ref
      })
      val statsActor = TestProbe()
      implicit val _ = statsActor.ref

      val session = Session(5)
      val randomRequests = List.fill(6)(session.requests.head)
      randomRequests.foreach(sessionProxyActor ! _)

      statsActor.expectMsg(RequestRejected(session))
    }
  }
}
