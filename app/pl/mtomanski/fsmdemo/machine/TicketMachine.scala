package pl.mtomanski.fsmdemo.machine

import akka.actor.{ActorRef, FSM, Props}
import pl.mtomanski.fsmdemo.actors.ConnectionActor.{FetchSoonestConnections, SoonestConnectionsFromOrigin}
import pl.mtomanski.fsmdemo.actors.PrintoutActor.{PrintOutFinished, PrintOutTicket}
import pl.mtomanski.fsmdemo.actors.ReservationActor.{CancelReservation, MakeReservation}
import pl.mtomanski.fsmdemo.domain._

import scala.concurrent.duration._

class TicketMachine(connectionActor: ActorRef,
                    reservationActor: ActorRef, printOutActor: ActorRef) extends FSM[TicketMachineState, TicketMachineData] {

  val reservationTimeout = 20.seconds

  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateTicketMachine(origin), Empty) =>
      val id = TicketMachineIdGenerator.generate
      goto(FetchingSoonestConnections) using Empty.addIdAndOrigin(id, origin) replying id
  }

  when(FetchingSoonestConnections) {
    case Event(SoonestConnectionsFromOrigin(connections), data: DataWithOrigin) =>
      goto(WaitingForConnectionSelection) using data.addConnections(connections)
  }

  when(WaitingForConnectionSelection) {
    case Event(SelectConnection(connection), data: DataWithConnections) =>
      goto(WaitingForPayment) using data.selectConnection(connection) replying connection.id
  }

  when(WaitingForPayment, reservationTimeout) {
    case Event(PaymentSuccessful(paymentId), data: DataWithSelectedConnection) =>
      goto(PrintingOutTickets) using data.paymentDone(paymentId) replying paymentId
    case Event(StateTimeout, data: DataWithSelectedConnection) =>
      goto(FetchingSoonestConnections) using data.resetAfterTimeout()
  }

  when(PrintingOutTickets) {
    case Event(PrintOutFinished(ticketNumber, connection), data: DataWithPayment) =>
      println(s"Ticket $ticketNumber printed successfully")
      stop()
  }

  onTransition {
    case Idle -> FetchingSoonestConnections =>
      nextStateData match {
        case DataWithOrigin(id, origin) => {
          connectionActor ! FetchSoonestConnections(origin)
        }
      }
    case WaitingForConnectionSelection -> WaitingForPayment =>
      nextStateData match {
        case DataWithSelectedConnection(id, origin, selectedConnection) =>
          reservationActor ! MakeReservation(selectedConnection)
      }
    case WaitingForPayment -> PrintingOutTickets =>
      nextStateData match {
        case DataWithPayment(id, origin, selectedConnection, paymentId) =>
          printOutActor ! PrintOutTicket(selectedConnection)
      }
    case WaitingForPayment -> FetchingSoonestConnections =>
      stateData match {
        case DataWithSelectedConnection(id, origin, selectedConnection) =>
          println("Timeout received")
          reservationActor ! CancelReservation(selectedConnection)
          connectionActor ! FetchSoonestConnections(origin)
      }
  }

  onTransition {
    case (a: TicketMachineState) -> (b: TicketMachineState) =>
      println(s"Going from ${a.getClass.getSimpleName} to ${b.getClass.getSimpleName}")
  }

  initialize()

}

object TicketMachine {
  def props(connectionActor: ActorRef,
            reservationActor: ActorRef, printOutActor: ActorRef): Props = Props(new TicketMachine(connectionActor, reservationActor, printOutActor))
}