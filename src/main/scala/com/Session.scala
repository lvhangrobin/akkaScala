package com

import scala.util.Random
import Session._

// A session is created with a predefined duration, for simplicity
case class Session(duration: Int) {

  val id = Math.abs(Random.nextLong())
  val start = System.currentTimeMillis()
  val referrer = randomReferrer
  val browser = randomBrowser

  val requests: List[Request] =
    for {
      tick <- (0 to duration).toList
      ts = start + tick * 1000
    } yield Request(this, ts, randomUrl)

  override def toString = s"Session($id, $referrer, $browser)"
}

object Session {
  val urls      = distributedList(Map("/" -> 4, "/about" -> 2, "/store" -> 2, "/blog" -> 1, "/help" -> 1))
  val browsers  = distributedList(Map("chrome" -> 5, "firefox" -> 3, "ie" -> 2))
  val referrers = distributedList(Map("google" -> 8, "twitter" -> 1, "facebook" -> 2))

  def randomUrl      = urls(Random.nextInt(urls.length))
  def randomBrowser  = browsers(Random.nextInt(browsers.length))
  def randomReferrer =
    if(Random.nextInt(100) < 98)
      referrers(Random.nextInt(referrers.length))
    else
      Random.nextString(10)

  // Lazy way of creating a list with a skewed distribution - repeat elements
  def distributedList[A](map: Map[A,Int]): List[A] = {
    for {
      (value, prob) <- map.toList
      i <- 1 to prob
    } yield value
  }
}