package pl.mtomanski.fsmdemo.domain

import java.util.UUID

object TicketMachineIdGenerator {
  def generate: Id = UUID.randomUUID().toString
}
