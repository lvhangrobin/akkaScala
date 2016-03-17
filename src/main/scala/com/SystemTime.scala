package com

import java.time.{ZoneOffset, Instant, LocalDateTime}

case class SystemTime(timestamp: Long) {
  val minute = {
    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
    time.getHour * 60 + time.getMinute
  }
}
