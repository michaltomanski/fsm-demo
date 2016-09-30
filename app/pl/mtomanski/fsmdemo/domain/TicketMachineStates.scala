package pl.mtomanski.fsmdemo.domain

import akka.persistence.fsm.PersistentFSM.FSMState

sealed trait TicketMachineState extends FSMState

case object Idle extends TicketMachineState {
  override def identifier: String = "Idle"
}

case object FetchingSoonestConnections extends TicketMachineState {
  override def identifier: String = "FetchingSoonestConnections"
}

case object WaitingForConnectionSelection extends TicketMachineState {
  override def identifier: String = "WaitingForConnectionSelection"
}

case object WaitingForPayment extends TicketMachineState {
  override def identifier: String = "WaitingForPayment"
}

case object PrintingOutTickets extends TicketMachineState {
  override def identifier: String = "PrintingTickets"
}