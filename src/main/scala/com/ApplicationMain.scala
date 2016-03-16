package com

object ApplicationMain extends App {
  val stream = new EventProducer(5)

  for {
    i <- 1 to 20
    requests = stream.tick
  } println(requests)
}