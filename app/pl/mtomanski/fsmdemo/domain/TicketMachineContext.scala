package pl.mtomanski.fsmdemo.domain

sealed trait TicketMachineContext

case object Empty extends TicketMachineContext

final case class ContextWithOrigin(id: Id, origin: Origin) extends TicketMachineContext

final case class ContextWithConnections(id: Id, origin: Origin, connections: Seq[Connection]) extends TicketMachineContext

final case class ContextWithSelectedConnection(id: Id, origin: Origin, selectedConnection: Connection) extends TicketMachineContext

final case class ContextWithPayment(id: Id, origin: Origin, selectedConnection: Connection, paymentId: Id) extends TicketMachineContext