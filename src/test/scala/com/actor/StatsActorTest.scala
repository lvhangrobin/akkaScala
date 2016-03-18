package com.actor

import akka.actor.Status
import akka.testkit.{TestActorRef, TestProbe}
import com._
import com.actor.DatabaseActor.DatabaseFailureException

class StatsActorTest extends BaseAkkaSpec{


  "StatsActor" should {
    "create a new session actor if not seen before" in {
      val statsActor = system.actorOf(StatsActor.props, "stats-actor")
      val request = (new EventProducer(1)).tick.head
      statsActor ! request

      TestProbe().expectActor("/user/stats-actor/$*")
    }

    "send the event to the same session proxy actor" in {
      val statsActor = TestActorRef[StatsActor](new StatsActor)
      val request = (new EventProducer(1)).tick.head
      val sessionProxyActor = TestProbe()
      statsActor.underlyingActor.sessions = Map(request.session -> sessionProxyActor.ref)

      statsActor ! RetrievedData(Stats(List.empty)) //switch context
      statsActor ! request
      sessionProxyActor.expectMsg(request)
    }

    "terminate inactive session actor" in {
      val sessionActor = TestActorRef[SessionActor](new SessionActor)
      val session = Session(20)
      val requests = session.requests
      sessionActor.underlyingActor.requests = requests

      val statsActor = TestActorRef[StatsActor](new StatsActor)
      statsActor.underlyingActor.sessions = Map(session -> sessionActor)
      val randomRequests = Session(20).requests
      statsActor ! RetrievedData(Stats(randomRequests)) //switch context

      val statsTestProbe = TestProbe()
      statsTestProbe.watch(sessionActor)

      statsActor ! InactiveSession(requests, sessionActor)

      statsActor.underlyingActor.sessions should not contain key (session)
      statsActor.underlyingActor.stats shouldEqual Stats(randomRequests ++ requests)
      statsTestProbe.expectTerminated(sessionActor)
    }

    "restart if it throws an exception" in {
      val loggingActor = TestProbe()
      val sessionActor = TestProbe()

      val statsActor = TestActorRef[StatsActor](new StatsActor {
        override def createLoggingActor() = loggingActor.ref
      })

      val randomRequests = Session(20).requests

      statsActor ! RetrievedData(Stats(randomRequests)) //switch context
      statsActor ! InactiveSession(randomRequests, sessionActor.ref)

      loggingActor.expectMsgClass[Retry](classOf[Retry])
    }

    "store data periodically" in {
      val databaseActor = TestProbe()

      val statsActor = TestActorRef[StatsActor](new StatsActor {
        override def createSessionProxyActor() = TestProbe().ref
        override def createDatabaseActor() = databaseActor.ref
      })

      val randomRequests = Session(20).requests
      statsActor ! RetrievedData(Stats(randomRequests)) //switch context

      val systemTimes = randomRequests.take(5).map(r => SystemTime(r.timestamp))
      systemTimes.foreach(statsActor ! _)

      databaseActor.expectMsg(StoreData(Stats(randomRequests)))
    }

    "retrieve stats on postRestart" in {
      val databaseActor = TestProbe()

      val statsActor = TestActorRef[StatsActor](new StatsActor {
        override def createDatabaseActor() = databaseActor.ref
      })

      statsActor.underlyingActor.postRestart(new Exception)

      databaseActor.expectMsg(RetrieveData)
    }

    "handle reading from database failure by using default empty stats" in {
//      lazy val databaseActor = TestActorRef[DatabaseActor](new DatabaseActor {
//        override val failureRate = 100
//      })
//
//      val testProbe = TestProbe()
//      val statsActor = TestActorRef[StatsActor](new StatsActor {
//        override def createDatabaseActor() = databaseActor
//
//        override def dealWithError(t: Throwable) = {
//          testProbe.ref ! Status.Failure(t)
//        }
//      })
//
//
//      implicit val _ = statsActor
//      val randomRequests = Session(20).requests
//      val stats = Stats(randomRequests)
//
//      databaseActor ! StoreData(stats)

    }

    "handle writing to database failure by logging ignoring the exception" in {

    }
  }
}
