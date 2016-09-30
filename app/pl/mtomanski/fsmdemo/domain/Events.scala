package pl.mtomanski.fsmdemo.domain


sealed trait TicketMachineEvent

case class TicketMachineCreated(id: Id, origin: Origin) extends TicketMachineEvent

case class SoonestConnectionsFetched(connections: Seq[Connection]) extends TicketMachineEvent

case class ConnectionSelected(connection: Connection) extends TicketMachineEvent

case class PaymentMade(paymentId: Id) extends TicketMachineEvent

case object ReservationTimeoutOccurred extends TicketMachineEvent

