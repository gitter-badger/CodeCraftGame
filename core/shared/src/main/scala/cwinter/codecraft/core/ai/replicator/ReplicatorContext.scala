package cwinter.codecraft.core.ai.replicator

import cwinter.codecraft.core.ai.replicator.combat.{ReplicatorCommand, ReplicatorBattleCoordinator}
import cwinter.codecraft.core.ai.shared.{HarvestCoordinatorWithZones, SharedContext}


private[codecraft] class ReplicatorContext(
  val greedy: Boolean,
  val confident: Boolean,
  val aggressive: Boolean
) extends SharedContext[ReplicatorCommand] {
  val battleCoordinator = new ReplicatorBattleCoordinator(this)
  val mothershipCoordinator = new MothershipCoordinator
  val harvestCoordinator = new HarvestCoordinatorWithZones

  var isReplicatorInConstruction: Boolean = false
}

