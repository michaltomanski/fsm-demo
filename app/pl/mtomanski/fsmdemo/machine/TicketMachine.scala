package pl.mtomanski.fsmdemo.machine

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import pl.mtomanski.fsmdemo.actors.ConnectionActor.{FetchSoonestConnections, SoonestConnectionsFromOrigin}
import pl.mtomanski.fsmdemo.actors.GatewayActor.{PaymentFailed, SoonestConnections}
import pl.mtomanski.fsmdemo.actors.PrintoutActor.{PrintOutFinished, PrintOutTicket}
import pl.mtomanski.fsmdemo.actors.ReservationActor.{CancelReservation, MakeReservation}
import pl.mtomanski.fsmdemo.domain.{ConnectionSelected, _}

import scala.concurrent.duration._
import scala.reflect._

class TicketMachine(gatewayActor: ActorRef, connectionActor: ActorRef,
                    reservationActor: ActorRef, printOutActor: ActorRef) extends PersistentFSM[TicketMachineState, TicketMachineContext, TicketMachineEvent] {


  override def domainEventClassTag: ClassTag[TicketMachineEvent] = classTag[TicketMachineEvent]

  override def persistenceId: String = "TicketMachine"

  override def applyEvent(domainEvent: TicketMachineEvent, currentData: TicketMachineContext): TicketMachineContext =
    (domainEvent, currentData) match {
      case (TicketMachineCreated(id, origin), _) =>
        ContextWithOrigin(id, origin)
      case (SoonestConnectionsFetched(connections), ContextWithOrigin(id, origin)) =>
        ContextWithConnections(id, origin, connections)
      case (ConnectionSelected(connection), ContextWithConnections(id, origin, connections)) =>
        ContextWithSelectedConnection(id, origin, selectedConnection = connection)
      case (PaymentMade(paymentId), ContextWithSelectedConnection(id, origin, selectedConnection)) =>
        ContextWithPayment(id, origin, selectedConnection, paymentId)
    }

  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateTicketMachine(origin), Empty) =>
      val id = TicketMachineIdGenerator.generate
      goto(FetchingSoonestConnections) applying TicketMachineCreated(id, origin) replying id
  }

  when(FetchingSoonestConnections) {
    case Event(SoonestConnectionsFromOrigin(connections), data: ContextWithOrigin) =>
      goto(WaitingForConnectionSelection) applying SoonestConnectionsFetched(connections)
  }

  when(WaitingForConnectionSelection) {
    case Event(SelectConnection(connection), data: ContextWithConnections) =>
      goto(WaitingForPayment) applying ConnectionSelected(connection) replying connection.id
  }

  when(WaitingForPayment) {
    case Event(PaymentSuccessful(paymentId), data: ContextWithSelectedConnection) =>
      goto(PrintingOutTickets) applying PaymentMade(paymentId)
  }

  onTransition {
    case Idle -> FetchingSoonestConnections =>
      nextStateData match {
        case ContextWithOrigin(id, origin) => {
          println("Going to FetchingSoonestConnections")
          connectionActor ! FetchSoonestConnections(origin)
        }
      }
    case FetchingSoonestConnections -> WaitingForConnectionSelection =>
      nextStateData match {
        case ContextWithConnections(id, origin, connections) =>
          println("Going to WaitingForConnectionSelection")
      }
    case WaitingForConnectionSelection -> WaitingForPayment =>
      nextStateData match {
        case ContextWithSelectedConnection(id, origin, selectedConnection) =>
          println("Going to WaitingForPayment")
          reservationActor ! MakeReservation(selectedConnection)
      }
    case WaitingForPayment -> PrintingOutTickets =>
      nextStateData match {
        case ContextWithPayment(id, origin, selectedConnection, paymentId) =>
          println("Going to PrintingOutTickets")
          printOutActor ! PrintOutTicket(selectedConnection)
      }
  }

  override def onRecoveryCompleted(): Unit = println("Recovery completed :-)")

  override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
    println("Recovery failed")
    super.onRecoveryFailure(cause, event)
  }

}







object TicketMachine {
  def props(gatewayActor: ActorRef, connectionActor: ActorRef,
            reservationActor: ActorRef, printOutActor: ActorRef): Props = Props(new TicketMachine(gatewayActor, connectionActor, reservationActor, printOutActor))
}