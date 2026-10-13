package disputa.domain

import disputa.commons.ChampionshipId
import disputa.commons.TeamId
import disputa.domain.model.MatchPhase
import disputa.domain.rules.RoundRobin
import kotlin.test.Test
import kotlin.test.assertEquals

class RoundRobinTest {
    @Test
    fun fourTeamLeagueHasSixPairingsRegardlessOfShuffle() {
        val teams = listOf("A", "B", "C", "D").map { TeamId(it) }
        val first = RoundRobin.generate(ChampionshipId("c"), teams, seed = 1, MatchPhase.League)
        val second = RoundRobin.generate(ChampionshipId("c"), teams, seed = 99, MatchPhase.League)
        fun pairs(matches: List<disputa.domain.model.Match>) =
            matches.map { setOf(it.teamA, it.teamB) }.toSet()
        assertEquals(6, first.size)
        assertEquals(pairs(first), pairs(second))
    }
}
