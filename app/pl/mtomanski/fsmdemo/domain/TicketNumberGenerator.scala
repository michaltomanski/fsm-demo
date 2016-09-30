package pl.mtomanski.fsmdemo.domain

import java.util.UUID

object TicketNumberGenerator {
  def generate: TicketNumber = UUID.randomUUID().toString
}
