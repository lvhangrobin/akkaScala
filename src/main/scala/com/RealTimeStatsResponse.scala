package com

import com.actor.StatsActor._

case class RealTimeStatsResponse(
  totalNumOfUsers: Int,
  numOfUsersPerUrl: Map[Url, Int],
  numOfUsersPerBrowser: Map[Browser, Int]
)