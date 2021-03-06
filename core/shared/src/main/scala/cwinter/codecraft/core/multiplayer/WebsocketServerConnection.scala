package cwinter.codecraft.core.multiplayer

import cwinter.codecraft.core.SimulationContext
import cwinter.codecraft.core.objects.drone._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.language.postfixOps



private[core] class WebsocketServerConnection(
  val connection: WebsocketClient,
  val debug: Boolean = false
) extends RemoteServer {

  val initialWorldState = Promise[InitialSync]

  private[this] var serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
  private[this] var worldState = Promise[Iterable[DroneStateMessage]]


  connection.onMessage(handleMessage)
  connection.sendMessage(MultiplayerMessage.register)


  def handleMessage(client: WebsocketClient, message: String): Unit = {
    if (debug)
      println(message)
    MultiplayerMessage.parse(message) match {
      case CommandsMessage(commands) =>
        serverCommands.success(commands)
      case WorldStateMessage(state) =>
        worldState.success(state)
      case start: InitialSync =>
        initialWorldState.success(start)
      case Register =>
    }
  }

  def receiveInitialWorldState(): Future[InitialSync] =
    initialWorldState.future

  override def receiveCommands()(implicit context: SimulationContext): Future[Seq[(Int, DroneCommand)]] = {
    if (debug)
      println(s"[t=${context.timestep}] Waiting for commands...")
    for (commands <- serverCommands.future) yield {
      if (debug)
        println("Commands received.")
      serverCommands = Promise[Seq[(Int, SerializableDroneCommand)]]
      deserialize(commands)
    }
  }

  override def receiveWorldState(): Future[Iterable[DroneStateMessage]] = {
    for (state <- worldState.future) yield {
      worldState = Promise[Iterable[DroneStateMessage]]
      state
    }
  }

  override def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit = {
    val serializable =
      for ((id, command) <- commands)
        yield (id, command.toSerializable)
    val message = MultiplayerMessage.serialize(serializable)
    if (debug)
      println(s"sendCommands($message)")
    connection.sendMessage(message)
  }


  def deserialize(commands: Seq[(Int, SerializableDroneCommand)])(
    implicit context: SimulationContext
  ): Seq[(Int, DroneCommand)] =
    for ((id, command) <- commands)
      yield (id, DroneCommand(command))
}

