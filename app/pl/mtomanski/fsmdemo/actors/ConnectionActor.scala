package pl.mtomanski.fsmdemo.actors

import java.util.UUID

import akka.actor.{Actor, Props}
import pl.mtomanski.fsmdemo.actors.ConnectionActor._
import pl.mtomanski.fsmdemo.domain.{Connection, Destination, Origin}

class ConnectionActor extends Actor {

  override def receive = {
    case FetchSoonestConnections(origin) =>
      println("Connection actor is fetching soonest connections")
      sender() ! getSoonestConnections(origin)
  }

  private def getSoonestConnections(origin: Origin) = {
    val soonestConnections = Seq(
      Connection("1", origin, destination1, departure1),
      Connection("2", origin, destination2, departure2)
    )
    SoonestConnectionsFromOrigin(soonestConnections)
  }
}

object ConnectionActor {

  def props(): Props = Props(new ConnectionActor)

  case class FetchSoonestConnections(origin: Origin)

  case class SoonestConnectionsFromOrigin(connections: Seq[Connection])

  // Mocked
  val destination1 = Destination(UUID.randomUUID().toString, "Wroclaw")
  val destination2 = Destination(UUID.randomUUID().toString, "Warsaw")
  val departure1 = "18:15"
  val departure2 = "18:30"
}
