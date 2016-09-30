package pl.mtomanski.fsmdemo.actors

import akka.actor.{Actor, Props}
import pl.mtomanski.fsmdemo.actors.ReservationActor.MakeReservation
import pl.mtomanski.fsmdemo.domain.{Connection, Origin}

class ReservationActor extends Actor {
  override def receive: Receive = {
    case MakeReservation(connection) => println(s"Reservation made for connection ${connection.id}")
  }
}

object ReservationActor {
  def props(): Props = Props(new ReservationActor)

  case class MakeReservation(connection: Connection)
  case class CancelReservation(connection: Connection)
}
