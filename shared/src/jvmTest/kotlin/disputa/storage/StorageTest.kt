package disputa.storage

import disputa.commons.Outcome
import disputa.createJvmAppContainer
import disputa.domain.model.ChampionshipFormat
import disputa.domain.rules.Career
import disputa.domain.rules.ChampionshipDraft
import kotlinx.datetime.LocalDate
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class StorageTest {
    @Test
    fun persistsChampionshipAndMatches() {
        val container = createJvmAppContainer()
        val a = (container.teams.save(null, "Alpha", null, null, false) as Outcome.Ok).value
        val b = (container.teams.save(null, "Beta", null, null, false) as Outcome.Ok).value
        val created = container.championships.create(
            ChampionshipDraft(
                name = "Copa",
                date = LocalDate(2026, 10, 9),
                format = ChampionshipFormat.LeagueOnly,
                teamIds = listOf(a.id, b.id),
                qualifierCount = null,
                thirdPlaceEnabled = false,
                groups = null,
                drawGroupsAtRandom = false,
                fixtureSeed = 3,
            ),
        )
        assertIs<Outcome.Ok<*>>(created)
        val id = (created as Outcome.Ok).value.championship.id
        val fixtures = (container.championships.generateFixtures(id) as Outcome.Ok).value
        assertEquals(1, fixtures.matches.size)
        val reloaded = container.championships.getSnapshot(id)!!
        assertEquals(1, reloaded.matches.size)
        assertEquals("Copa", reloaded.championship.name)
    }

    @Test
    fun replacingPhotoDeletesOldFile() {
        val dir = createTempDirectory("photos").toFile()
        val container = createJvmAppContainer(dir)
        val first = (container.teams.save(null, "Foto", null, byteArrayOf(1, 2, 3), false) as Outcome.Ok).value
        val firstFile = File(dir, first.photoRelativePath!!.substringAfterLast('/'))
        assertTrue(firstFile.exists())
        val second = (container.teams.save(first.id, "Foto", null, byteArrayOf(4, 5), false) as Outcome.Ok).value
        assertTrue(File(dir, second.photoRelativePath!!.substringAfterLast('/')).exists())
    }

    @Test
    fun deletingClosedChampionshipRemovesCareer() {
        val container = createJvmAppContainer()
        val a = (container.teams.save(null, "A", null, null, false) as Outcome.Ok).value
        val b = (container.teams.save(null, "B", null, null, false) as Outcome.Ok).value
        val created = (container.championships.create(
            ChampionshipDraft(
                name = "Final",
                date = LocalDate(2026, 10, 9),
                format = ChampionshipFormat.KnockoutOnly,
                teamIds = listOf(a.id, b.id),
                qualifierCount = null,
                thirdPlaceEnabled = false,
                groups = null,
                drawGroupsAtRandom = false,
                fixtureSeed = 1,
            ),
        ) as Outcome.Ok).value
        val withKo = (container.championships.generateKnockout(created.championship.id, emptyList()) as Outcome.Ok).value
        val match = withKo.matches.first()
        val scored = (container.championships.applyScore(created.championship.id, match.id, 2, 0, null) as Outcome.Ok).value
        val closed = (container.championships.close(created.championship.id) as Outcome.Ok).value
        val winner = Career.championOf(closed)!!
        val teams = listOf(container.teams.get(a.id)!!, container.teams.get(b.id)!!)
        val before = Career.board(teams, listOf(closed))
        assertEquals(1, before.titles.first { it.teamId == winner }.titles)
        assertTrue(before.points.first { it.teamId == winner }.goalsFor >= 2)
        container.championships.delete(created.championship.id)
        val after = Career.board(teams, emptyList())
        assertEquals(0, after.titles.first { it.teamId == winner }.titles)
        assertEquals(0, after.points.first { it.teamId == winner }.goalsFor)
        assertEquals(scored.matches.first().isPlayed, true)
    }

    @Test
    fun cannotDeleteUsedTeam() {
        val container = createJvmAppContainer()
        val a = (container.teams.save(null, "A", null, null, false) as Outcome.Ok).value
        val b = (container.teams.save(null, "B", null, null, false) as Outcome.Ok).value
        container.championships.create(
            ChampionshipDraft(
                name = "X",
                date = LocalDate(2026, 10, 9),
                format = ChampionshipFormat.LeagueOnly,
                teamIds = listOf(a.id, b.id),
                qualifierCount = null,
                thirdPlaceEnabled = false,
                groups = null,
                drawGroupsAtRandom = false,
                fixtureSeed = 1,
            ),
        )
        val deleted = container.teams.delete(a.id)
        assertIs<Outcome.Err>(deleted)
    }
}
