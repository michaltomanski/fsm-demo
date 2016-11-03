package pl.mtomanski.fsmdemo.domain

sealed trait TicketMachineData

case object Empty extends TicketMachineData {
  def addIdAndOrigin(id: Id, origin: Origin): TicketMachineData = DataWithOrigin(id, origin)
}

final case class DataWithOrigin(id: Id, origin: Origin) extends TicketMachineData {
  def addConnections(connections: Seq[Connection]): TicketMachineData = DataWithConnections(id, origin, connections)
}

final case class DataWithConnections(id: Id, origin: Origin, connections: Seq[Connection]) extends TicketMachineData {
  def selectConnection(connection: Connection): TicketMachineData = DataWithSelectedConnection(id, origin, connection)
}

final case class DataWithSelectedConnection(id: Id, origin: Origin, selectedConnection: Connection) extends TicketMachineData {
  def paymentDone(paymentId: Id): TicketMachineData = DataWithPayment(id, origin, selectedConnection, paymentId)
  def resetAfterTimeout() = DataWithOrigin(id, origin)
}

final case class DataWithPayment(id: Id, origin: Origin, selectedConnection: Connection, paymentId: Id) extends TicketMachineData