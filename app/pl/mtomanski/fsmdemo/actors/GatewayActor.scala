package pl.mtomanski.fsmdemo.actors

import akka.actor.{Actor, Props}
import pl.mtomanski.fsmdemo.actors.GatewayActor.{PaymentFailed, SoonestConnections}
import pl.mtomanski.fsmdemo.domain._

class GatewayActor extends Actor {

  override def receive = {
    case SoonestConnections(connections) => println(s"Soonest connections received: $connections")
    case PaymentFailed(connection) => println("Payment failed")

  }

}

object GatewayActor {
  def props(): Props = Props(new GatewayActor)

  case class SoonestConnections(connections: Seq[Connection])
  case class PaymentFailed(connection: Connection)
}
