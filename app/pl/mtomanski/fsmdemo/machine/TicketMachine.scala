package pl.mtomanski.fsmdemo.machine

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import com.typesafe.config.ConfigFactory
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

	//val reservationTimeout = ConfigFactory.load().getString("reservation-timeout")
	val reservationTimeout = 2.seconds // todo props

	override def applyEvent(domainEvent: TicketMachineEvent, currentData: TicketMachineContext): TicketMachineContext = {
		domainEvent match {
			case TicketMachineCreated(id, origin) =>
				ContextWithOrigin(id, origin)
			case SoonestConnectionsFetched(connections) =>
				currentData match {
					case ContextWithOrigin(id, origin) =>
						ContextWithConnections(id, origin, connections)
				}
			case ConnectionSelected(selectedConnection) =>
				currentData match {
					case ContextWithConnections(id, origin, connections) =>
						ContextWithSelectedConnection(id, origin, selectedConnection)
				}
			case PaymentMade(paymentId) =>
				currentData match {
					case ContextWithSelectedConnection(id, origin, selectedConnection) =>
						ContextWithPayment(id, origin, selectedConnection, paymentId)
				}
			case ReservationTimeoutOccurred =>
				currentData match {
					case ContextWithSelectedConnection(id, origin, _) =>
						ContextWithOrigin(id, origin)
				}
		}
	}

	override def persistenceId: String = "TicketMachine"

	override def domainEventClassTag: ClassTag[TicketMachineEvent] = classTag[TicketMachineEvent]

	startWith(Idle, Empty)

	when(Idle) {
		case Event(CreateTicketMachine(origin), _) =>
			val id = TicketMachineIdGenerator.generate
			goto(FetchingSoonestConnections) applying TicketMachineCreated(id, origin)
	}

	when(FetchingSoonestConnections) {
		case Event(SoonestConnectionsFromOrigin(connections), data: ContextWithOrigin) =>
			goto(WaitingForConnectionSelection) applying SoonestConnectionsFetched(connections)
	}

	when(WaitingForConnectionSelection) {
		case Event(SelectConnection(connection), data: ContextWithConnections) =>
			goto(WaitingForPayment) applying ConnectionSelected(connection)
	}

	when(WaitingForPayment, reservationTimeout) {
		case Event(PaymentSuccessful(paymentId), data: ContextWithSelectedConnection) =>
			goto(PrintingOutTickets) applying PaymentMade(paymentId)
		case Event(StateTimeout, _) =>
			goto(FetchingSoonestConnections) applying ReservationTimeoutOccurred
	}

	when(PrintingOutTickets) {
		case Event(event @ PrintOutFinished(ticketNumber, connection), data: ContextWithPayment) =>
			println(s"Ticket #$ticketNumber printed successfully")
			gatewayActor ! event
			stop()
	}

	whenUnhandled {
		case Event(event, data) =>
			println(s"Unhandled event $event while data is $data in state $stateName")
			stay()
	}

	onTransition {
		case Idle -> FetchingSoonestConnections =>
			nextStateData match {
				case ContextWithOrigin(id, origin) => {
					connectionActor ! FetchSoonestConnections(origin)
					sender() ! id
				}
			}
		case FetchingSoonestConnections -> WaitingForConnectionSelection =>
			nextStateData match {
				case ContextWithConnections(_, _, connections) => gatewayActor ! SoonestConnections(connections)
			}
		case WaitingForConnectionSelection -> WaitingForPayment =>
			nextStateData match {
				case ContextWithSelectedConnection(_, _, selectedConnection) =>
					reservationActor ! MakeReservation(selectedConnection)
			}
		case WaitingForPayment -> FetchingSoonestConnections =>
			stateData match {
				case ContextWithSelectedConnection(_, _, selectedConnection) =>
					reservationActor ! CancelReservation(selectedConnection)
					gatewayActor ! PaymentFailed(selectedConnection)
			}
		case WaitingForPayment -> PrintingOutTickets =>
			nextStateData match {
				case ContextWithPayment(_, _, connection, _) =>
					printOutActor ! PrintOutTicket(connection)
			}
	}

	override def onRecoveryCompleted(): Unit = {
		println("Recovery completed")
	}
}

object TicketMachine {
	def props(gatewayActor: ActorRef, connectionActor: ActorRef, reservationActor: ActorRef,
						printOutActor: ActorRef): Props =
		Props(new TicketMachine(gatewayActor, connectionActor, reservationActor, printOutActor))
}