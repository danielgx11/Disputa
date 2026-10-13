package disputa

import app.cash.sqldelight.db.SqlDriver
import disputa.commons.ChampionshipId
import disputa.commons.MatchId
import disputa.commons.TeamId
import disputa.features.career.CareerViewModel
import disputa.features.championships.ChampionshipHubViewModel
import disputa.features.championships.ChampionshipListViewModel
import disputa.features.championships.ChampionshipWizardViewModel
import disputa.features.championships.ScoreEntryViewModel
import disputa.features.teams.TeamFormViewModel
import disputa.features.teams.TeamListViewModel
import disputa.storage.ChampionshipRepository
import disputa.storage.DisputaDatabase
import disputa.storage.PhotoStore
import disputa.storage.TeamRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AppContainer(
    driver: SqlDriver,
    photoStore: PhotoStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val database = DisputaDatabase(driver)
    val teams = TeamRepository(database, photoStore)
    val championships = ChampionshipRepository(database)

    fun teamList(): TeamListViewModel = TeamListViewModel(teams, dispatcher)
    fun teamForm(id: String?): TeamFormViewModel = TeamFormViewModel(teams, id?.let(::TeamId), dispatcher)
    fun championshipList(): ChampionshipListViewModel = ChampionshipListViewModel(championships, dispatcher)
    fun championshipWizard(): ChampionshipWizardViewModel = ChampionshipWizardViewModel(teams, championships, dispatcher)
    fun championshipHub(id: String): ChampionshipHubViewModel =
        ChampionshipHubViewModel(ChampionshipId(id), championships, teams, dispatcher)
    fun scoreEntry(championshipId: String, matchId: String): ScoreEntryViewModel =
        ScoreEntryViewModel(ChampionshipId(championshipId), MatchId(matchId), championships, dispatcher)
    fun career(): CareerViewModel = CareerViewModel(teams, championships, dispatcher)
}
