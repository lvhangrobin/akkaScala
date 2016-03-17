package com.actor

import akka.testkit.{TestActorRef, TestProbe}
import com.{EventProducer, Request, BaseAkkaSpec}

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
  }
}
