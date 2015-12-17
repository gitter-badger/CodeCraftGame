package cwinter.codecraft.core.network

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.objects.drone.{DroneCommand, DroneDynamicsState}


private[core] trait RemoteClient {
  def waitForCommands(): Seq[(Int, DroneCommand)]
  def sendCommands(commands: Seq[(Int, DroneCommand)]): Unit
  def sendWorldState(worldState: Iterable[DroneDynamicsState]): Unit
  def players: Set[Player]
}

