package cwinter.codecraft.core.api

import cwinter.codecraft.core._
import cwinter.codecraft.core.ai.basicplus
import cwinter.codecraft.core.ai.cheese.Mothership
import cwinter.codecraft.core.multiplayer.{WebsocketServerConnection, WebsocketClient}
import cwinter.codecraft.core.replay.{DummyDroneController, Replayer}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}

import scala.async.Async.{async, await}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


private[codecraft] trait GameMasterLike {
  /**
   * Default dimensions for the size of the game world.
   */
  final val DefaultWorldSize = Rectangle(-3000, 3000, -2000, 2000)

  /**
    * Smaller than default dimensions for the size of the game world.
    */
  final val SmallWorldSize = Rectangle(-2000, 2000, -1500, 1500)

  /**
    * Larger than default dimensions for the size of the game world.
    */
  final val LargeWorldSize = Rectangle(-4500, 4500, -3000, 3000)

  /**
   * Identifies the current version of the JavaScript API.
   * This value is changed whenever a backwards incompatible change is made to the JavaScript API.
   */
  final val JavascriptAPIVersion = "0.1"

  private final val DefaultResourceDistribution = Seq(
      (10, 4), (10, 4),
      (10, 6), (10, 6),
      (10, 8), (10, 8),
      (10, 10), (10, 10),
      (7, 20), (7, 20),
      (5, 30), (5, 30)
    )

  private final val SmallResourceDistribution =
    for (group <- DefaultResourceDistribution.grouped(2).toSeq)
      yield group.head

  private final val LargeResourceDistribution =
    for ((n, size) <- DefaultResourceDistribution)
      yield (n, (size * 1.5).toInt)

  /**
   * Default number of modules for the initial mothership.
   */
  final val DefaultMothership = new DroneSpec(
    missileBatteries = 3,
    constructors = 3,
    storageModules = 3,
    shieldGenerators = 1
  )


  private def constructSpawns(pos1: Vector2, pos2: Vector2): Seq[Spawn] = {
    val spawn1 = Spawn(DefaultMothership, pos1, BluePlayer, 21)
    val spawn2 = Spawn(DefaultMothership, pos2, OrangePlayer, 21)
    Seq(spawn1, spawn2)
  }


  /**
    * Runs the `simulator`.
    */
  def run(simulator: DroneWorldSimulator): DroneWorldSimulator

  /**
    * Creates a new [[DroneWorldSimulator]] for a singleplayer game with the specified settings.
    */
  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    worldSize: Rectangle,
    resourceClusters: Seq[(Int, Int)],
    spawn1: Vector2,
    spawn2: Vector2
  ): DroneWorldSimulator = {
    val spawns = constructSpawns(spawn1, spawn2)
    val map = WorldMap(worldSize, resourceClusters, spawns).withDefaultWinConditions
    val controllers = Seq(mothership1, mothership2)
    new DroneWorldSimulator(map, controllers, devEvents)
  }

  /**
    * Creates a new [[DroneWorldSimulator]] for a singleplayer game with the specified settings.
    */
  def createSimulator(
    mothership1: DroneControllerBase,
    mothership2: DroneControllerBase,
    map: WorldMap
  ): DroneWorldSimulator = {
    val controllers = Seq(mothership1, mothership2)
    new DroneWorldSimulator(map, controllers, devEvents)
  }

  /**
    * Creates a new drone world simulator from a replay string.
    */
  def createReplaySimulator(replayText: String): DroneWorldSimulator = {
    val replayer = new Replayer(replayText.lines)
    new DroneWorldSimulator(replayer.map, replayer.controllers, devEvents, Some(replayer))
  }


  /**
   * Starts a new game with two players.
   *
   * @param mothership1 The controller for the initial mothership of player 1.
   * @param mothership2 The controller for the initial mothership of player 2.
   */
  @deprecated("This method has been renamed to `runGame` and will be removed in a future version.", "0.2.4.3")
  def startGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): DroneWorldSimulator =
    runGame(mothership1, mothership2)

  /**
    * Runs a game with default settings.
    * @param mothership1 The drone controller for player 1.
    * @param mothership2 The drone controller for player 2.
    */
  def runGame(mothership1: DroneControllerBase, mothership2: DroneControllerBase): DroneWorldSimulator = {
    val controllers = Seq(mothership1, mothership2)
    val simulator = new DroneWorldSimulator(defaultMap, controllers, devEvents)
    run(simulator)
    simulator
  }


  /**
    * Returns a drone controller for the level 1 AI.
    */
  def level1AI(): DroneControllerBase = new ai.basic.Mothership

  /**
    * Returns a drone controller for the level 2 AI.
    */
  def level2AI(): DroneControllerBase = new basicplus.Mothership

  /**
    * Returns a drone controller for the level 3 AI.
    */
  def bonusLevelAI(): DroneControllerBase = new ai.cheese.Mothership

  /**
    * Returns a drone controller for the Replicator AI.
    */
  def replicatorAI(greedy: Boolean = false, confident: Boolean = false, aggressive: Boolean = false): DroneControllerBase =
    new ai.replicator.Replicator(greedy, confident, aggressive)

  /**
    * Returns a drone controller for the Destroyer AI.
    */
  def destroyerAI(): DroneControllerBase = new ai.destroyer.Mothership

  /**
    * The default [[WorldMap]].
    */
  val defaultMap: WorldMap = {
    val spawns = constructSpawns(Vector2(2500, 500), Vector2(-2500, -500))
    WorldMap(DefaultWorldSize, DefaultResourceDistribution, spawns).withDefaultWinConditions
  }

  /**
    * A small [[WorldMap]].
    */
  val smallMap: WorldMap = {
    val spawns = constructSpawns(Vector2(1750, 500), Vector2(-1750, -500))
    WorldMap(SmallWorldSize, SmallResourceDistribution, spawns).withDefaultWinConditions
  }

  /**
    * A large [[WorldMap]].
    */
  val largeMap: WorldMap = {
    val spawns = constructSpawns(Vector2(3800, 1000), Vector2(-3800, -1000))
    WorldMap(LargeWorldSize, LargeResourceDistribution, spawns).withDefaultWinConditions
  }

  /**
    * The [[WorldMap]] for the first level.
    */
  val level1Map: WorldMap = {
    val worldSize = Rectangle(-2000, 2000, -1000, 1000)
    val spawns = constructSpawns(Vector2(1000, 200), Vector2(-1000, -200))
    WorldMap(worldSize, Seq.fill(8)((2, 40)), spawns).withDefaultWinConditions
  }

  /**
   * The [[WorldMap]] for the second level.
   */
  val level2Map: WorldMap = defaultMap

  /**
   * The [[WorldMap]] for the bonus level.
   */
  val bonusLevelMap: WorldMap = defaultMap

  /**
   * Runs the first level.
   *
   * @param mothership1 The controller for your mothership.
   */
  def runLevel1(mothership1: DroneControllerBase): DroneWorldSimulator = {
    val map = level1Map
    val controllers = Seq(mothership1, level1AI())
    val simulator = new DroneWorldSimulator(map, controllers, devEvents)
    run(simulator)
    simulator
  }

  /**
   * Runs the second level.
   *
   * @param mothership The controller for your mothership.
   */
  def runLevel2(mothership: DroneControllerBase): DroneWorldSimulator = {
    runGame(mothership, new Mothership)
  }

  /**
   * Runs the third level.
   *
   * @param mothership The controller for your mothership.
   */
  def runLevel3(mothership: DroneControllerBase): DroneWorldSimulator = {
    runGame(mothership, new ai.basicplus.Mothership)
  }

  /**
   * Runs a game with the level 1 AI versus the level 2 AI.
   */
  def runL1vL2(): DroneWorldSimulator = {
    runGame(new ai.basic.Mothership, new Mothership)
  }

  /**
   * Runs a game with the level 3 AI versus the level 3 AI.
   */
  def runL3vL3(): DroneWorldSimulator = {
    runGame(new ai.basicplus.Mothership, new ai.basicplus.Mothership)
  }

  /**
   * Sets up a multiplayer game with the specified server.
   */
  def prepareMultiplayerGame(
    serverAddress: String,
    controller: DroneControllerBase
  ): Future[DroneWorldSimulator] =  async {
    val (map, connection) = await { prepareMultiplayerGame(serverAddress) }

    assert(map.initialDrones.count(d => connection.isLocalPlayer(d.player)) == 1,
      "Must have one drone owned by local player.")

    new DroneWorldSimulator(
      map,
      map.initialDrones.map(drone =>
        if (connection.isLocalPlayer(drone.player)) controller
        else new DummyDroneController
      ),
      t => Seq.empty,
      None,
      connection
    )
  }

  /**
   * Sets up a multiplayer game.
   */
  def prepareMultiplayerGame(serverAddress: String): Future[(WorldMap, MultiplayerConfig)] = async {
    val websocketClient = connectToWebsocket(s"ws://$serverAddress:8080")
    val serverConnection = new WebsocketServerConnection(websocketClient)
    val sync = await { serverConnection.receiveInitialWorldState() }

    val clientPlayers = sync.localPlayers
    val serverPlayers = sync.remotePlayers
    val map = sync.worldMap
    val connection = MultiplayerClientConfig(clientPlayers, serverPlayers, serverConnection)
    (map, connection)
  }

  protected def connectToWebsocket(connectionString: String): WebsocketClient

  protected var devEvents: Int => Seq[SimulatorEvent] = t => Seq()
  protected[codecraft] def setDevEvents(generator: Int => Seq[SimulatorEvent]): Unit = {
    devEvents = generator
  }
}

