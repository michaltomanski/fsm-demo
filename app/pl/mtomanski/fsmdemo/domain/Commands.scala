package pl.mtomanski.fsmdemo.domain

import play.api.libs.json.Json

sealed trait TicketMachineCommand

case class CreateTicketMachine(origin: Origin)

case class SelectConnection(connection: Connection)

case class PaymentSuccessful(paymentId: Id)

object PaymentSuccessful {
  implicit val format = Json.format[PaymentSuccessful]
}