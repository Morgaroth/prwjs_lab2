package io.github.morgaroth.reactive.lab2.app

import akka.actor.{ActorSystem, Props}
import io.github.morgaroth.reactive.lab2.actors.SoulsReaper.WatchHim
import io.github.morgaroth.reactive.lab2.actors.{AuctionSearch, Buyer, Seller, SoulsReaper}
import io.github.morgaroth.reactive.lab2.app.Application.Reaper

import scala.concurrent.duration._

object Application {
  class Reaper extends SoulsReaper {
    override def allSoulsReaped(): Unit = context.system.shutdown()
  }
}

trait Application {
  this: Configuration =>

  def run() {
    val system = ActorSystem("Lydie")
    import system.dispatcher

    val reaper = system.actorOf(Props[Reaper], "reaper")

    sellers.map { sellerConf =>
      val seller = system.actorOf(Props(classOf[Seller], sellerConf.items), s"seller_${sellerConf.name}")
      reaper ! WatchHim(seller)
    }

    system.actorOf(Props[AuctionSearch], "AuctionSearch")

    system.scheduler.scheduleOnce(1 seconds) {
      buyers.map { buyerCfg =>
        val buyer = system.actorOf(Props(classOf[Buyer], buyerCfg.target, buyerCfg.maxPrice), buyerCfg.name)
        reaper ! WatchHim(buyer)
      }
    }

    system.scheduler.scheduleOnce(10 minutes)(system.shutdown())
  }
}
