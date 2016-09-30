package pl.mtomanski.fsmdemo.actors

import akka.actor.{Actor, Props}
import pl.mtomanski.fsmdemo.actors.PrintoutActor.{PrintOutFinished, PrintOutTicket}
import pl.mtomanski.fsmdemo.domain.{Connection, Origin, TicketNumber, TicketNumberGenerator}

class PrintoutActor extends Actor {

  override def receive = {
    case PrintOutTicket(connection) =>
      val ticketNumber = TicketNumberGenerator.generate
      println(s"Printing out ticket no. $ticketNumber from ${connection.origin.name} to ${connection.destination.name} departing at ${connection.departure}")
      sender() ! PrintOutFinished(ticketNumber, connection)
  }

}

object PrintoutActor {
  def props(): Props = Props(new PrintoutActor)

  case class PrintOutTicket(connection: Connection)
  case class PrintOutFinished(ticketNumber: TicketNumber, connection: Connection)
}
