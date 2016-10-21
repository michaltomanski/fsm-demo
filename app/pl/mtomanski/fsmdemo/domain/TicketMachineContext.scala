package pl.mtomanski.fsmdemo.domain

sealed trait TicketMachineContext {
  def addConnections(connections: Seq[Connection]): TicketMachineContext = this match {
    case ContextWithOrigin(id, origin) => ContextWithConnections(id, origin, connections)
  }
  def selectConnection(connection: Connection): TicketMachineContext = this match {
    case ContextWithConnections(id, origin, connections) => ContextWithSelectedConnection(id, origin, connection)
  }
  def paymentDone(paymentId: Id): TicketMachineContext = this match {
    case ContextWithSelectedConnection(id, origin, connection) => ContextWithPayment(id, origin, connection, paymentId)
  }
  def addIdAndOrigin(id: Id, origin: Origin): TicketMachineContext = ContextWithOrigin(id, origin)

  def resetAfterTimeout() = this match {
    case ContextWithSelectedConnection(id, origin, connection) => ContextWithOrigin(id, origin)
  }
}

case object Empty extends TicketMachineContext

final case class ContextWithOrigin(id: Id, origin: Origin) extends TicketMachineContext

final case class ContextWithConnections(id: Id, origin: Origin, connections: Seq[Connection]) extends TicketMachineContext

final case class ContextWithSelectedConnection(id: Id, origin: Origin, selectedConnection: Connection) extends TicketMachineContext

final case class ContextWithPayment(id: Id, origin: Origin, selectedConnection: Connection, paymentId: Id) extends TicketMachineContext