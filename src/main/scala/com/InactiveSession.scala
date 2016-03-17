package com

import akka.actor.ActorRef

case class InactiveSession(requests: List[Request], session: ActorRef)
