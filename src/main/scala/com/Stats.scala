package com

/**
 * Created by hangl on 3/17/16.
 */
case class Stats(userRequests: List[Request]) {

  lazy val requestsPerBrowser: Map[String, Int] =
    userRequests.map(request => request.session.browser).groupBy(b => b).mapValues(_.size)

  lazy val busiestMinute: (Int, Int) = // minute of the day -> number of requests
    userRequests
      .map(r => SystemTime(r.timestamp).minute)
      .groupBy(m => m)
      .mapValues(_.size)
      .maxBy(_._2)

  lazy val averagePageViews: Double = {
    val uniqueSessions = userRequests.groupBy(_.session).size
    userRequests.size.toDouble / uniqueSessions
  }

  lazy val averageVisitTime: Double = {
    val sessionToVisitTime = userRequests
      .groupBy(_.session)
      .mapValues{requests =>
        val timestamps = requests.map(_.timestamp)
        timestamps.max - timestamps.min
      }
    sessionToVisitTime.values.sum.toDouble / sessionToVisitTime.size
  }

  lazy val top5LandingPages: List[(String, Int)] =
    userRequests
      .groupBy(_.session)
      .mapValues{_.sortBy(_.timestamp).head.url}.toList
      .groupBy(_._2)
      .mapValues(_.size).toList
      .sortBy(_._2).reverse.take(5)

  lazy val top5SinkPages: List[(String, Int)] =
    userRequests
      .groupBy(_.session)
      .mapValues{_.sortBy(_.timestamp).last.url}.toList
      .groupBy(_._2)
      .mapValues(_.size).toList
      .sortBy(_._2).reverse.take(5)

  lazy val top5Browsers: List[(String, Int)] =
    userRequests
      .groupBy(_.session)
      .mapValues{_.head.session.browser}.toList
      .groupBy(_._2)
      .mapValues(_.size).toList
      .sortBy(_._2).reverse.take(5)

  lazy val top5Referrers: List[(String, Int)] =
    userRequests
      .groupBy(_.session)
      .mapValues{_.head.session.referrer}.toList
      .groupBy(_._2)
      .mapValues(_.size).toList
      .sortBy(_._2).reverse.take(5)
}
