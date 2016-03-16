package com

import scala.util.Random
import EventProducer._

// Use this to simulate a stream of user requests.
// Use the concurrentUsers to make the stream try to keep the users around that number
class EventProducer(concurrentUsers: Int) {

  // Create initial sessions
  var sessions: Map[Long, List[Request]] = (
    for{
      i <- (1 to concurrentUsers).toList
      session = new Session(randomVisitTime)
    } yield (session.id, session.requests)
  ).toMap

  // Call this every time unit to get the requests for that time.
  // Note that this will not try to simulate time. 
  // Every time you call this method, you'll get the events for the next simulated second.
  def tick: List[Request] = {

    // Take the head of each list of requests on the map
    val currentRequests = sessions.map{ case (id,requests) => requests.head }

    // Remove the head of each list of requests on the map, filter out those with size 1 
    // (sessions that are ending on this tick)
    sessions =
      for {
        (id,requests) <- sessions if(requests.size > 1)
      } yield (id,requests.tail)

    // Decide if we should start a new session
    if(concurrentUsers > sessions.size && Random.nextBoolean()){
      val session = new Session(randomVisitTime)
      sessions += session.id -> session.requests
    }

    currentRequests.toList
  }
}

object EventProducer {
  val longVisit  = 300 // secs
  val shortVisit = 10 // sec

  // For more interesting behaviour, we insert some randomness in our numbers
  def deviate(n: Int):Int = (n * (1.75 - Random.nextDouble())).toInt

  // 80% of users stay for a long visit, the rest bounce off
  def randomVisitTime =
    if(Random.nextInt(100) < 80) deviate(longVisit)
    else deviate(shortVisit)

}
