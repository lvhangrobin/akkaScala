package com.actor

import akka.testkit.{TestActorRef, TestProbe}
import com._

class StatsActorTest extends BaseAkkaSpec{


  "StatsActor" should {
    "create a new session actor if not seen before" in {
      val statsActor = system.actorOf(StatsActor.props, "stats-actor")
      val request = (new EventProducer(1)).tick.head
      statsActor ! request

      TestProbe().expectActor("/user/stats-actor/$*")
    }

    "send the event to the same session actor" in {
      val statsActor = TestActorRef[StatsActor](new StatsActor)
      val request = (new EventProducer(1)).tick.head
      val sessionActor = TestProbe()
      statsActor.underlyingActor.sessions = Map(request.session -> sessionActor.ref)

      statsActor ! request
      sessionActor.expectMsg(request)
    }

    "terminate inactive session actor" in {
      val sessionActor = TestActorRef[SessionActor](new SessionActor)
      val session = new Session(20)
      val requests = session.requests
      sessionActor.underlyingActor.requests = requests

      val statsActor = TestActorRef[StatsActor](new StatsActor)
      statsActor.underlyingActor.sessions = Map(session -> sessionActor)
      val randomRequests = new Session(20).requests
      statsActor.underlyingActor.stats = Stats(randomRequests)
      val statsTestProbe = TestProbe()
      statsTestProbe.watch(sessionActor)

      statsActor ! InactiveSession(requests, sessionActor)

      statsActor.underlyingActor.sessions should not contain key (session)
      statsActor.underlyingActor.stats shouldEqual Stats(randomRequests ++ requests)
      statsTestProbe.expectTerminated(sessionActor)
    }

    "it will restart if it throws an exception" in {
      val loggingActor = TestProbe()
      val sessionActor = TestProbe()

      val statsActor = TestActorRef[StatsActor](new StatsActor {
        override def createLoggingActor() = loggingActor.ref
      })

      val randomRequests = new Session(20).requests

      statsActor ! InactiveSession(randomRequests, sessionActor.ref)

      loggingActor.expectMsgClass[Retry](classOf[Retry])
    }
  }
}
