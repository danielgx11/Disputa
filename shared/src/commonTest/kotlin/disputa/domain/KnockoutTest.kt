package disputa.domain

import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.MatchPhase
import disputa.domain.rules.Knockout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KnockoutTest {
    @Test
    fun fourLeagueQualifiersPairOneVsFour() {
        val snapshot = snapshot(ChampionshipFormat.LeagueThenKnockout, listOf("A", "B", "C", "D"), qualifierCount = 4)
        val withFixtures = (ChampionshipEngine.generatePhaseFixtures(snapshot) as Outcome.Ok).value
        val played = withFixtures.matches.fold(withFixtures) { acc, match ->
            val a = match.teamA!!.value
            val b = match.teamB!!.value
            val ga = when (a) {
                "A" -> 3
                "B" -> 2
                "C" -> 1
                else -> 0
            }
            val gb = when (b) {
                "A" -> 3
                "B" -> 2
                "C" -> 1
                else -> 0
            }
            (ChampionshipEngine.applyScore(acc, match.id, ga, gb, null) as Outcome.Ok).value
        }
        val bracket = (ChampionshipEngine.generateKnockout(played) as Outcome.Ok).value
        val semis = bracket.matches.filter { it.phase == MatchPhase.Knockout && it.roundIndex == 0 }
        val pairs = semis.map { setOf(it.teamA, it.teamB) }.toSet()
        assertEquals(
            setOf(setOf(TeamId("A"), TeamId("D")), setOf(TeamId("B"), TeamId("C"))),
            pairs,
        )
    }

    @Test
    fun eightSeedPairings() {
        val seeds = (1..8).map { TeamId("$it") }
        val matches = (Knockout.generateFromSeeds(disputa.commons.ChampionshipId("c"), seeds, thirdPlace = false) as Outcome.Ok).value
        val first = matches.filter { it.roundIndex == 0 }.map { setOf(it.teamA!!.value, it.teamB!!.value) }.toSet()
        assertEquals(
            setOf(setOf("1", "8"), setOf("4", "5"), setOf("2", "7"), setOf("3", "6")),
            first,
        )
        val halves = matches.filter { it.roundIndex == 0 }.chunked(2)
        val left = halves[0].flatMap { listOf(it.teamA!!.value, it.teamB!!.value) }.toSet()
        val right = halves[1].flatMap { listOf(it.teamA!!.value, it.teamB!!.value) }.toSet()
        assertTrue("1" in left || "1" in right)
        assertTrue(
            ("1" in left && "2" in right) || ("1" in right && "2" in left),
        )
    }

    @Test
    fun twoGroupsCrossBracket() {
        val snapshot = snapshot(
            ChampionshipFormat.GroupsThenKnockout,
            listOf("A1", "A2", "A3", "A4", "B1", "B2", "B3", "B4"),
            qualifierCount = 2,
            thirdPlace = true,
            groups = listOf(
                listOf("A1", "A2", "A3", "A4"),
                listOf("B1", "B2", "B3", "B4"),
            ),
        )
        val withFixtures = (ChampionshipEngine.generatePhaseFixtures(snapshot) as Outcome.Ok).value
        fun strength(id: String) = when {
            id.endsWith("1") -> 4
            id.endsWith("2") -> 2
            id.endsWith("3") -> 1
            else -> 0
        }
        val played = withFixtures.matches.fold(withFixtures) { acc, match ->
            val ga = strength(match.teamA!!.value)
            val gb = strength(match.teamB!!.value)
            (ChampionshipEngine.applyScore(acc, match.id, ga, gb, null) as Outcome.Ok).value
        }
        val bracket = (ChampionshipEngine.generateKnockout(played) as Outcome.Ok).value
        val semis = bracket.matches.filter { it.phase == MatchPhase.Knockout && it.roundIndex == 0 }
        val pairs = semis.map { setOf(it.teamA!!.value, it.teamB!!.value) }.toSet()
        assertEquals(setOf(setOf("A1", "B2"), setOf("B1", "A2")), pairs)
        assertTrue(bracket.matches.any { it.phase == MatchPhase.ThirdPlace })
    }

    @Test
    fun sixQualifiersNeedTwoExtras() {
        val snapshot = snapshot(
            ChampionshipFormat.LeagueThenKnockout,
            listOf("A", "B", "C", "D", "E", "F", "G", "H"),
            qualifierCount = 6,
        )
        val withFixtures = (ChampionshipEngine.generatePhaseFixtures(snapshot) as Outcome.Ok).value
        val order = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val played = withFixtures.matches.fold(withFixtures) { acc, match ->
            val ga = 8 - order.indexOf(match.teamA!!.value)
            val gb = 8 - order.indexOf(match.teamB!!.value)
            (ChampionshipEngine.applyScore(acc, match.id, ga, gb, null) as Outcome.Ok).value
        }
        val missing = ChampionshipEngine.generateKnockout(played)
        assertIs<Outcome.Err>(missing)
        assertEquals(disputa.commons.DomainError.FillQualifiersRequired, missing.error)
        val extras = missing.proposedExtraIds.map { TeamId(it) }
        val filled = ChampionshipEngine.generateKnockout(played, extras)
        assertIs<Outcome.Ok<*>>(filled)
        val ko = (filled as Outcome.Ok).value.matches.filter { it.phase == MatchPhase.Knockout && it.roundIndex == 0 }
        assertEquals(4, ko.size)
    }
}
