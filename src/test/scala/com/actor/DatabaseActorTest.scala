package com.actor

import akka.testkit.TestActorRef
import com.{Stats, Session, BaseAkkaSpec}

import java.io.File


class DatabaseActorTest extends BaseAkkaSpec{

  "DatabaseActor" should {
    "it will serialize and deserialize stats correctly" in {
      val randomRequests = new Session(20).requests
      val stats = Stats(randomRequests)

      val databaseActor = TestActorRef[DatabaseActor](new DatabaseActor {
        override val persistentFilePath = "./target/persistence.log"
      })

      databaseActor.underlyingActor.writeToFile(stats)

      databaseActor.underlyingActor.recoverStats() shouldEqual stats
    }
  }

  override protected def afterAll(): Unit = {
    val file = new File("./target/persistence.log")
    if (file.exists()) file.delete()
    system.shutdown()
  }
}
