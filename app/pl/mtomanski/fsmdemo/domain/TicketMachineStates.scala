package pl.mtomanski.fsmdemo.domain

sealed trait TicketMachineState

case object Idle extends TicketMachineState

case object FetchingSoonestConnections extends TicketMachineState

case object WaitingForConnectionSelection extends TicketMachineState

case object WaitingForPayment extends TicketMachineState

case object PrintingOutTickets extends TicketMachineState