package pl.mtomanski.fsmdemo

import play.api.libs.json.Json

package object domain {
  type Id = String

  type TicketNumber = String

  final case class Origin(id: Id, name: String)

  object Origin {
    implicit val format = Json.format[Origin]
  }

  final case class Destination(id: Id, name: String)

  object Destination {
    implicit val format = Json.format[Destination]
  }

  final case class Connection(id: Id, origin: Origin, destination: Destination, departure: String)

  object Connection {
    implicit val format = Json.format[Connection]
  }
}
