package disputa.domain

import disputa.commons.ChampionshipId
import disputa.commons.MatchId
import disputa.commons.TeamId
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase
import disputa.domain.rules.Standings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StandingsTest {
    private fun match(a: String, b: String, ga: Int, gb: Int) = Match(
        id = MatchId("$a$b"),
        championshipId = ChampionshipId("c"),
        phase = MatchPhase.League,
        groupId = null,
        roundIndex = 0,
        slot = 0,
        teamA = TeamId(a),
        teamB = TeamId(b),
        goalsA = ga,
        goalsB = gb,
        penaltyWinner = null,
    )

    @Test
    fun goalDifferenceBreaksPointsTie() {
        val teams = listOf("A", "B", "C").map { TeamId(it) }
        val table = Standings.compute(
            teams,
            listOf(
                match("A", "C", 4, 0),
                match("B", "C", 1, 0),
            ),
        )
        assertEquals(TeamId("A"), table.rows.first().teamId)
        assertEquals(3, table.rows.first().points)
        assertEquals(3, table.rows[1].points)
        assertTrue(table.rows.first().goalDifference > table.rows[1].goalDifference)
    }

    @Test
    fun unresolvedWhenTwoTeamsNeverMet() {
        val teams = listOf("A", "B", "C", "D").map { TeamId(it) }
        val table = Standings.compute(
            teams,
            listOf(
                match("A", "C", 1, 0),
                match("B", "D", 1, 0),
            ),
        )
        val a = table.rows.first { it.teamId == TeamId("A") }
        val b = table.rows.first { it.teamId == TeamId("B") }
        assertEquals(a.points, b.points)
        assertEquals(a.goalDifference, b.goalDifference)
        assertEquals(a.goalsFor, b.goalsFor)
        assertTrue(a.unresolved && b.unresolved)
        assertNull(Standings.uniqueFirst(table))
    }
}
