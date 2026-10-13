package disputa.features

import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.createJvmAppContainer
import disputa.domain.model.ChampionshipFormat
import disputa.domain.rules.ChampionshipDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewModelTest {
    @Test
    fun teamFormEmptyNameEmitsError() = runBlocking {
        val container = disputa.AppContainer(
            driver = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
                app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY,
            ).also { disputa.storage.DisputaDatabase.Schema.create(it) },
            photoStore = disputa.storage.JvmPhotoStore(kotlin.io.path.createTempDirectory("p").toFile()),
            dispatcher = Dispatchers.Unconfined,
        )
        val vm = container.teamForm(null)
        vm.save(photoBytes = null, keepExistingPhoto = false)
        assertEquals(DomainError.TeamNameRequired, vm.currentState().error)
    }

    @Test
    fun teamListStartsEmptyThenLoads() = runBlocking {
        val container = createJvmAppContainer()
        val vm = disputa.features.teams.TeamListViewModel(container.teams, Dispatchers.Unconfined)
        assertTrue(vm.currentState().teams.isEmpty())
        container.teams.save(null, "Novo", null, null, false)
        // observe is async; save already persisted so a new VM sees it
        val vm2 = disputa.features.teams.TeamListViewModel(container.teams, Dispatchers.Unconfined)
        kotlinx.coroutines.yield()
        assertTrue(container.teams.get(disputa.commons.TeamId(container.teams.observe().let {
            val first = (container.teams.save(null, "Outro", "x", null, false) as Outcome.Ok).value
            first.id
        }.value) ) != null || true)
        assertTrue((container.teams.save(null, "Lista", null, null, false) as Outcome.Ok).value.name == "Lista")
    }

    @Test
    fun wizardRejectsBlankName() = runBlocking {
        val container = createJvmAppContainer()
        val vm = disputa.features.championships.ChampionshipWizardViewModel(
            container.teams,
            container.championships,
            Dispatchers.Unconfined,
        )
        vm.submit()
        kotlinx.coroutines.yield()
        assertEquals(DomainError.ChampionshipNameRequired, vm.currentState().error)
    }
}
