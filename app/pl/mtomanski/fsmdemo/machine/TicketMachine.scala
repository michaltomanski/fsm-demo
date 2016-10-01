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
										reservationActor: ActorRef, printOutActor: ActorRef)
	extends PersistentFSM[TicketMachineState, TicketMachineContext, TicketMachineEvent] {

	val reservationTimeout = 10.seconds // todo props

	override def persistenceId: String = "TicketMachine"

	override def domainEventClassTag: ClassTag[TicketMachineEvent] = classTag[TicketMachineEvent]

	startWith(Idle, Empty)

	when(Idle) {
		case Event(CreateTicketMachine(origin), _) =>
			val id = TicketMachineIdGenerator.generate
			println("Going to FetchingSoonestConnections")
			goto(FetchingSoonestConnections) applying TicketMachineCreated(id, origin)
	}

	when(FetchingSoonestConnections) {
		case Event(SoonestConnectionsFromOrigin(connections), data: ContextWithOrigin) =>
			println("Going to WaitingForConnectionSelection")
			goto(WaitingForConnectionSelection) applying SoonestConnectionsFetched(connections)
	}

	when(WaitingForConnectionSelection) {
		case Event(SelectConnection(connection), data: ContextWithConnections) =>
			println("Going to WaitingForPayment")
			goto(WaitingForPayment) applying ConnectionSelected(connection)
	}

	when(WaitingForPayment, reservationTimeout) {
		case Event(PaymentSuccessful(paymentId), data: ContextWithSelectedConnection) =>
			println("Going to PrintingOutTickets")
			goto(PrintingOutTickets) applying PaymentMade(paymentId)
		case Event(StateTimeout, _) =>
			println("Going to FetchingSoonestConnections")
			goto(FetchingSoonestConnections) applying ReservationTimeoutOccurred
	}

	when(PrintingOutTickets) {
		case Event(event @ PrintOutFinished(ticketNumber, connection), data: ContextWithPayment) =>
			println(s"Ticket #$ticketNumber printed successfully")
			println("Stopping actor...")
			gatewayActor ! event
			stop()
	}

	whenUnhandled {
		case Event(event, data) =>
			val msg = s"Unhandled event $event while data is $data in state $stateName"
			println(msg)
			sender() !  UnhandledError(msg)
			stay()
	}

	onTransition {
		case Idle -> FetchingSoonestConnections =>
			nextStateData match {
				case ContextWithOrigin(id, origin) => {
					connectionActor ! FetchSoonestConnections(origin)
					sender() ! id
				}
				case _ =>
			}
		case FetchingSoonestConnections -> WaitingForConnectionSelection =>
			nextStateData match {
				case ContextWithConnections(_, _, connections) => gatewayActor ! SoonestConnections(connections)
				case _ =>
			}
		case WaitingForConnectionSelection -> WaitingForPayment =>
			nextStateData match {
				case ContextWithSelectedConnection(_, _, selectedConnection) =>
					reservationActor ! MakeReservation(selectedConnection)
					sender() ! selectedConnection.id
				case _ =>
			}
		case WaitingForPayment -> FetchingSoonestConnections =>
			stateData match {
				case ContextWithSelectedConnection(_, origin, selectedConnection) =>
					reservationActor ! CancelReservation(selectedConnection)
					gatewayActor ! PaymentFailed(selectedConnection)
					connectionActor ! FetchSoonestConnections(origin)
				case _ =>
			}
		case WaitingForPayment -> PrintingOutTickets =>
			nextStateData match {
				case ContextWithPayment(_, _, connection, paymentId) =>
					printOutActor ! PrintOutTicket(connection)
					sender() ! paymentId
				case _ =>
			}
	}

	override def applyEvent(domainEvent: TicketMachineEvent, currentData: TicketMachineContext): TicketMachineContext = {
		domainEvent match {
			case TicketMachineCreated(id, origin) =>
				ContextWithOrigin(id, origin)
			case SoonestConnectionsFetched(connections) =>
				currentData match {
					case ContextWithOrigin(id, origin) =>
						ContextWithConnections(id, origin, connections)
					case _ => currentData
				}
			case ConnectionSelected(selectedConnection) =>
				currentData match {
					case ContextWithConnections(id, origin, connections) =>
						ContextWithSelectedConnection(id, origin, selectedConnection)
					case _ => currentData
				}
			case PaymentMade(paymentId) =>
				currentData match {
					case ContextWithSelectedConnection(id, origin, selectedConnection) =>
						ContextWithPayment(id, origin, selectedConnection, paymentId)
					case _ => currentData
				}
			case ReservationTimeoutOccurred =>
				currentData match {
					case ContextWithSelectedConnection(id, origin, _) =>
						ContextWithOrigin(id, origin)
					case _ => currentData
				}
		}
	}

	override def receiveCommand: Receive = super.receiveCommand

	override def onRecoveryCompleted(): Unit = {
		println("Recovery completed")
	}
}

object TicketMachine {
	def props(gatewayActor: ActorRef, connectionActor: ActorRef, reservationActor: ActorRef,
						printOutActor: ActorRef): Props =
		Props(new TicketMachine(gatewayActor, connectionActor, reservationActor, printOutActor))
}

case class UnhandledError(msg: String)
