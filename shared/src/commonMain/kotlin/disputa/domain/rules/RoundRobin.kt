package disputa.domain.rules

import disputa.commons.ChampionshipId
import disputa.commons.GroupId
import disputa.commons.MatchId
import disputa.commons.TeamId
import disputa.commons.randomId
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase
import kotlin.random.Random

object RoundRobin {
    fun generate(
        championshipId: ChampionshipId,
        teamIds: List<TeamId>,
        seed: Long,
        phase: MatchPhase,
        groupId: GroupId? = null,
    ): List<Match> {
        require(teamIds.size >= 2)
        val withBye = teamIds.map { it as TeamId? }.toMutableList()
        if (withBye.size % 2 == 1) withBye.add(null)
        val n = withBye.size
        val rounds = n - 1
        val half = n / 2
        val rotation = withBye.toMutableList()
        val roundPairings = mutableListOf<List<Pair<TeamId, TeamId>>>()
        repeat(rounds) {
            val pairs = mutableListOf<Pair<TeamId, TeamId>>()
            for (i in 0 until half) {
                val a = rotation[i]
                val b = rotation[n - 1 - i]
                if (a != null && b != null) pairs += a to b
            }
            roundPairings += pairs
            val last = rotation.removeAt(rotation.lastIndex)
            rotation.add(1, last)
        }
        val shuffled = roundPairings.shuffled(Random(seed))
        return shuffled.flatMapIndexed { roundIndex, pairs ->
            pairs.mapIndexed { slot, (a, b) ->
                Match(
                    id = MatchId(randomId()),
                    championshipId = championshipId,
                    phase = phase,
                    groupId = groupId,
                    roundIndex = roundIndex,
                    slot = slot,
                    teamA = a,
                    teamB = b,
                    goalsA = null,
                    goalsB = null,
                    penaltyWinner = null,
                )
            }
        }
    }
}
