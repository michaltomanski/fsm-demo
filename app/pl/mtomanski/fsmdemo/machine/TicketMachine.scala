package pl.mtomanski.fsmdemo.machine

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import pl.mtomanski.fsmdemo.actors.ConnectionActor.{FetchSoonestConnections, SoonestConnectionsFromOrigin}
import pl.mtomanski.fsmdemo.actors.PrintoutActor.{PrintOutFinished, PrintOutTicket}
import pl.mtomanski.fsmdemo.actors.ReservationActor.{CancelReservation, MakeReservation}
import pl.mtomanski.fsmdemo.domain._

import scala.concurrent.duration._
import scala.reflect._

class TicketMachine(connectionActor: ActorRef,
                    reservationActor: ActorRef, printOutActor: ActorRef) extends PersistentFSM[TicketMachineState, TicketMachineData, TicketMachineEvent] {


  override def domainEventClassTag: ClassTag[TicketMachineEvent] = classTag[TicketMachineEvent]

  override def persistenceId: String = "TicketMachine"

  val reservationTimeout = 20.seconds

  override def applyEvent(domainEvent: TicketMachineEvent, currentData: TicketMachineData): TicketMachineData =
    domainEvent match {
      case TicketMachineCreated(id, origin) =>
        currentData.addIdAndOrigin(id, origin)
      case SoonestConnectionsFetched(connections) =>
        currentData.addConnections(connections)
      case ConnectionSelected(connection) =>
        currentData.selectConnection(connection)
      case PaymentMade(paymentId) =>
        currentData.paymentDone(paymentId)
      case ReservationTimeoutOccurred =>
        currentData.resetAfterTimeout()
      }
  
  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateTicketMachine(origin), Empty) =>
      val id = TicketMachineIdGenerator.generate
      goto(FetchingSoonestConnections) applying TicketMachineCreated(id, origin) replying id
  }

  when(FetchingSoonestConnections) {
    case Event(SoonestConnectionsFromOrigin(connections), data: DataWithOrigin) =>
      goto(WaitingForConnectionSelection) applying SoonestConnectionsFetched(connections)
  }

  when(WaitingForConnectionSelection) {
    case Event(SelectConnection(connection), data: DataWithConnections) =>
      goto(WaitingForPayment) applying ConnectionSelected(connection) replying connection.id
  }

  when(WaitingForPayment, reservationTimeout) {
    case Event(PaymentSuccessful(paymentId), data: DataWithSelectedConnection) =>
      goto(PrintingOutTickets) applying PaymentMade(paymentId) replying paymentId
    case Event(StateTimeout, _) =>
      goto(FetchingSoonestConnections) applying ReservationTimeoutOccurred
  }

  when(PrintingOutTickets) {
    case Event(PrintOutFinished(ticketNumber, connection), data: DataWithPayment) =>
      println(s"Ticket $ticketNumber printed successfully")
      stop()
  }

  onTransition {
    case Idle -> FetchingSoonestConnections =>
      nextStateData match {
        case DataWithOrigin(id, origin) =>
          connectionActor ! FetchSoonestConnections(origin)
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

  override def onRecoveryCompleted(): Unit = println("Recovery completed :-)")

  override def onRecoveryFailure(cause: Throwable, event: Option[Any]): Unit = {
    println("Recovery failed")
    super.onRecoveryFailure(cause, event)
  }

}







object TicketMachine {
  def props(connectionActor: ActorRef,
            reservationActor: ActorRef, printOutActor: ActorRef): Props = Props(new TicketMachine(connectionActor, reservationActor, printOutActor))
}