package pl.mtomanski.fsmdemo

import pl.mtomanski.fsmdemo.actors.{ConnectionActor, GatewayActor, PrintoutActor, ReservationActor}
import pl.mtomanski.fsmdemo.machine.TicketMachine
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext}
import play.api.ApplicationLoader.Context
import router.Routes

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = new MyComponents(context).application
}


class MyComponents(context: Context) extends BuiltInComponentsFromContext(context) {

  val gatewayActor = actorSystem.actorOf(GatewayActor.props())
  val connectionActor = actorSystem.actorOf(ConnectionActor.props())
  val reservationActor = actorSystem.actorOf(ReservationActor.props())
  val printOutActor = actorSystem.actorOf(PrintoutActor.props())

  val ticketMachine = actorSystem.actorOf(TicketMachine.props(
    gatewayActor, connectionActor, reservationActor, printOutActor))


  lazy val router = new Routes(httpErrorHandler, applicationController, null)

  lazy val applicationController = new controllers.TicketMachineController(ticketMachine)

}