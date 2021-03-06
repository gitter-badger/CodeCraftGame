package cwinter.codecraft.core

import cwinter.codecraft.core.api.Player
import cwinter.codecraft.core.multiplayer.{RemoteClient, RemoteServer}


sealed trait MultiplayerConfig {
  def isMultiplayerGame: Boolean
  def commandRecorder: CommandRecorder
  def isLocalPlayer(player: Player): Boolean
}

private[core] case class AuthoritativeServerConfig(
  localPlayers: Set[Player],
  remotePlayers: Set[Player],
  clients: Set[RemoteClient]
) extends MultiplayerConfig {
  def isMultiplayerGame = true
  def isLocalPlayer(player: Player): Boolean = localPlayers.contains(player)
  val commandRecorder = new CommandRecorder
}

private[core] case class MultiplayerClientConfig(
  localPlayers: Set[Player],
  remotePlayers: Set[Player],
  server: RemoteServer
) extends MultiplayerConfig {
  def isMultiplayerGame = true
  def isLocalPlayer(player: Player): Boolean = localPlayers.contains(player)
  val commandRecorder = new CommandRecorder
}

private[core] object SingleplayerConfig extends MultiplayerConfig {
  def isMultiplayerGame = false
  def commandRecorder = throw new Exception("Trying to call commandRecorder on SingleplayerConfig.")
  def isLocalPlayer(player: Player): Boolean = true
}





