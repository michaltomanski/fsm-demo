package pl.mtomanski.fsmdemo.domain

sealed trait TicketMachineData {
  def addConnections(connections: Seq[Connection]): TicketMachineData = this match {
    case DataWithOrigin(id, origin) => DataWithConnections(id, origin, connections)
  }
  def selectConnection(connection: Connection): TicketMachineData = this match {
    case DataWithConnections(id, origin, connections) => DataWithSelectedConnection(id, origin, connection)
  }
  def paymentDone(paymentId: Id): TicketMachineData = this match {
    case DataWithSelectedConnection(id, origin, connection) => DataWithPayment(id, origin, connection, paymentId)
  }
  def addIdAndOrigin(id: Id, origin: Origin): TicketMachineData = DataWithOrigin(id, origin)

  def resetAfterTimeout() = this match {
    case DataWithSelectedConnection(id, origin, connection) => DataWithOrigin(id, origin)
  }
}

case object Empty extends TicketMachineData

final case class DataWithOrigin(id: Id, origin: Origin) extends TicketMachineData

final case class DataWithConnections(id: Id, origin: Origin, connections: Seq[Connection]) extends TicketMachineData

final case class DataWithSelectedConnection(id: Id, origin: Origin, selectedConnection: Connection) extends TicketMachineData

final case class DataWithPayment(id: Id, origin: Origin, selectedConnection: Connection, paymentId: Id) extends TicketMachineData