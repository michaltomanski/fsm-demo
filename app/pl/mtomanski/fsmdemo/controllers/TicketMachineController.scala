package pl.mtomanski.fsmdemo.controllers


import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import pl.mtomanski.fsmdemo.domain.{CreateTicketMachine, Id, Origin}
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TicketMachineController (ticketMachine: ActorRef) extends Controller {

  implicit val timeout = Timeout(10.seconds)

  def index = Action {
    Ok
  }

  def create = Action.async(parse.json) { request =>
    val origin = request.body.as[Origin]
    (ticketMachine ? CreateTicketMachine(origin)).map {
      case id: Id => Ok(id)
    }
  }

}
