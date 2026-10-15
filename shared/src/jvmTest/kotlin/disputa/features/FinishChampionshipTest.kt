package disputa.features

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import disputa.AppContainer
import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.MatchPhase
import disputa.domain.rules.ChampionshipDraft
import disputa.features.championships.ChampionshipHubViewModel
import disputa.storage.DisputaDatabase
import disputa.storage.JvmPhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FinishChampionshipTest {
    private fun container(): AppContainer {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        DisputaDatabase.Schema.create(driver)
        return AppContainer(driver, JvmPhotoStore(createTempDirectory("finish").toFile()), Dispatchers.Unconfined)
    }

    private fun draft(format: ChampionshipFormat, ids: List<disputa.commons.TeamId>, thirdPlace: Boolean) =
        ChampionshipDraft(
            name = "Copa",
            date = LocalDate(2026, 10, 15),
            format = format,
            teamIds = ids,
            qualifierCount = null,
            thirdPlaceEnabled = thirdPlace,
            groups = null,
            drawGroupsAtRandom = false,
            fixtureSeed = 1,
        )

    @Test
    fun finishWithoutFinalKeepsTheErrorAfterRefresh() = runBlocking {
        val app = container()
        val a = (app.teams.save(null, "Alpha", null, null, false) as Outcome.Ok).value
        val b = (app.teams.save(null, "Beta", null, null, false) as Outcome.Ok).value
        val created = (app.championships.create(draft(ChampionshipFormat.KnockoutOnly, listOf(a.id, b.id), false)) as Outcome.Ok).value
        app.championships.generateKnockout(created.championship.id, emptyList())
        val vm = ChampionshipHubViewModel(created.championship.id, app.championships, app.teams, Dispatchers.Unconfined)
        vm.finishChampionship()
        assertEquals(DomainError.ChampionUndefined, vm.currentState().error)
        vm.refresh()
        assertEquals(DomainError.ChampionUndefined, vm.currentState().error)
        assertEquals(ChampionshipStatus.Open, vm.currentState().snapshot?.championship?.status)
    }

    @Test
    fun finishAfterFinalShowsChampion() = runBlocking {
        val app = container()
        val a = (app.teams.save(null, "Alpha", null, null, false) as Outcome.Ok).value
        val b = (app.teams.save(null, "Beta", null, null, false) as Outcome.Ok).value
        val created = (app.championships.create(draft(ChampionshipFormat.KnockoutOnly, listOf(a.id, b.id), false)) as Outcome.Ok).value
        val bracket = (app.championships.generateKnockout(created.championship.id, emptyList()) as Outcome.Ok).value
        val finalMatch = bracket.matches.first { it.phase == MatchPhase.Knockout }
        app.championships.applyScore(created.championship.id, finalMatch.id, 1, 0, null)
        val vm = ChampionshipHubViewModel(created.championship.id, app.championships, app.teams, Dispatchers.Unconfined)
        vm.finishChampionship()
        assertNull(vm.currentState().error)
        assertEquals(ChampionshipStatus.Closed, vm.currentState().snapshot?.championship?.status)
        assertEquals(finalMatch.teamA?.let { app.teams.get(it)?.name }, vm.currentState().championName)
    }

    @Test
    fun finishWithoutThirdPlaceStaysOpen() = runBlocking {
        val app = container()
        val ids = listOf("A", "B", "C", "D").map { name ->
            (app.teams.save(null, name, null, null, false) as Outcome.Ok).value.id
        }
        val created = (app.championships.create(draft(ChampionshipFormat.KnockoutOnly, ids, true)) as Outcome.Ok).value
        var snapshot = (app.championships.generateKnockout(created.championship.id, emptyList()) as Outcome.Ok).value
        snapshot.matches.filter { it.phase == MatchPhase.Knockout && it.roundIndex == 0 }.forEach { semi ->
            snapshot = (app.championships.applyScore(created.championship.id, semi.id, 1, 0, null) as Outcome.Ok).value
        }
        val finalMatch = snapshot.matches.first { it.phase == MatchPhase.Knockout && it.roundIndex == 1 }
        app.championships.applyScore(created.championship.id, finalMatch.id, 2, 1, null)
        val vm = ChampionshipHubViewModel(created.championship.id, app.championships, app.teams, Dispatchers.Unconfined)
        vm.finishChampionship()
        assertEquals(DomainError.ThirdPlaceIncomplete, vm.currentState().error)
        vm.refresh()
        assertEquals(DomainError.ThirdPlaceIncomplete, vm.currentState().error)
        assertEquals(ChampionshipStatus.Open, vm.currentState().snapshot?.championship?.status)
    }
}
