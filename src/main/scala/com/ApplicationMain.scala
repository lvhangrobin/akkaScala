package com

import akka.actor.ActorSystem
import com.actor.StatsActor

object ApplicationMain extends App {
  val stream = new EventProducer(5)

  val system = ActorSystem("akkaScala")

  val statsActor = system.actorOf(StatsActor.props, "stats-actor")
  
  for {
    i <- 1 to 20
    requests = stream.tick
  } {
    requests.foreach(statsActor ! _)
    statsActor ! SystemTime(requests.head.timestamp)
  }
}