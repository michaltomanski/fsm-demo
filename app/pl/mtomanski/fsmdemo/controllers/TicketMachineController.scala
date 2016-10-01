package pl.mtomanski.fsmdemo.controllers


import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import pl.mtomanski.fsmdemo.domain._
import pl.mtomanski.fsmdemo.machine.UnhandledError
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
class TicketMachineController (ticketMachine: ActorRef) extends Controller {

  implicit val timeout = Timeout(2.seconds)

  def index = Action {
    Ok
  }

  def create = Action.async(parse.json) { request =>
    val origin = request.body.as[Origin]
    (ticketMachine ? CreateTicketMachine(origin)).map {
      case id: Id => Ok(id)
      case err: UnhandledError => BadRequest(err.msg)
    }
  }

  def selectConnection = Action.async(parse.json) { request =>
    val selection = request.body.as[Connection]
    (ticketMachine ? SelectConnection(selection)).map {
      case id: Id => Ok(id)
    }
  }

  def payment = Action.async(parse.json) { request =>
    val paymentSuccessful = request.body.as[PaymentSuccessful]
    (ticketMachine ? paymentSuccessful).map {
      case id: Id => Ok(id)
    }
  }
}


