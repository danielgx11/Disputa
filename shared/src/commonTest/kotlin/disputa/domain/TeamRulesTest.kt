package disputa.domain

import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.domain.rules.TeamRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TeamRulesTest {
    @Test
    fun rejectsBlankName() {
        val result = TeamRules.create("   ", null, null)
        assertIs<Outcome.Err>(result)
        assertEquals(DomainError.TeamNameRequired, result.error)
    }

    @Test
    fun keepsStableIdOnEdit() {
        val created = (TeamRules.create("Galáticos", "amigos", null) as Outcome.Ok).value
        val edited = (TeamRules.edit(created, "Galaticos", "ok", "photos/a.jpg") as Outcome.Ok).value
        assertEquals(created.id, edited.id)
        assertEquals("Galaticos", edited.name)
        assertEquals("photos/a.jpg", edited.photoRelativePath)
        assertTrue(created.id.value.isNotEmpty())
    }
}
