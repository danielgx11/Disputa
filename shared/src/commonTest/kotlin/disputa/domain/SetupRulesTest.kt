package disputa.domain

import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.rules.ChampionshipDraft
import disputa.domain.rules.SetupRules
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SetupRulesTest {
    private val date = LocalDate(2026, 10, 9)

    @Test
    fun eightTeamKnockoutAllowed() {
        val draft = ChampionshipDraft(
            name = "Mata",
            date = date,
            format = ChampionshipFormat.KnockoutOnly,
            teamIds = (1..8).map { TeamId("$it") },
            qualifierCount = null,
            thirdPlaceEnabled = true,
            groups = null,
            drawGroupsAtRandom = false,
            fixtureSeed = 1,
        )
        assertIs<Outcome.Ok<*>>(SetupRules.create(draft))
    }

    @Test
    fun sixTeamKnockoutRejected() {
        val draft = ChampionshipDraft(
            name = "Mata",
            date = date,
            format = ChampionshipFormat.KnockoutOnly,
            teamIds = (1..6).map { TeamId("$it") },
            qualifierCount = null,
            thirdPlaceEnabled = false,
            groups = null,
            drawGroupsAtRandom = false,
            fixtureSeed = 1,
        )
        val result = SetupRules.create(draft)
        assertIs<Outcome.Err>(result)
        assertEquals(DomainError.KnockoutTeamCountNotPowerOfTwo, result.error)
    }

    @Test
    fun unevenGroupsRejected() {
        val draft = ChampionshipDraft(
            name = "Grupos",
            date = date,
            format = ChampionshipFormat.GroupsThenKnockout,
            teamIds = (1..8).map { TeamId("$it") },
            qualifierCount = 2,
            thirdPlaceEnabled = true,
            groups = listOf(
                (1..3).map { TeamId("$it") },
                (4..8).map { TeamId("$it") },
            ),
            drawGroupsAtRandom = false,
            fixtureSeed = 1,
        )
        val result = SetupRules.create(draft)
        assertIs<Outcome.Err>(result)
        assertEquals(DomainError.UnevenGroups, result.error)
    }
}
