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
    domainEvent match {
      case TicketMachineCreated(id, origin) =>
        ContextWithOrigin(id, origin)
    }

  startWith(Idle, Empty)

  when(Idle) {
    case Event(CreateTicketMachine(origin), Empty) =>
      val id = TicketMachineIdGenerator.generate
      goto(FetchingSoonestConnections) applying TicketMachineCreated(id, origin) replying id
  }

  onTransition {
    case Idle -> FetchingSoonestConnections =>
      nextStateData match {
        case ContextWithOrigin(id, origin) => {
          println("Going to FetchingSoonestConnections")
          connectionActor ! FetchSoonestConnections(origin)
        }
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