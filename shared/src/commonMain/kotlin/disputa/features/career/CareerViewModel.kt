package disputa.features.career

import disputa.commons.WatchHandle
import disputa.domain.model.CareerBoard
import disputa.domain.model.Team
import disputa.domain.rules.Career
import disputa.features.BaseViewModel
import disputa.storage.ChampionshipRepository
import disputa.storage.TeamRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class CareerState(
    val teams: Map<String, Team> = emptyMap(),
    val board: CareerBoard = CareerBoard(emptyList(), emptyList()),
)

class CareerViewModel(
    private val teams: TeamRepository,
    private val championships: ChampionshipRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(CareerState())
    val state: StateFlow<CareerState> = _state.asStateFlow()

    init {
        scope.launch {
            combine(teams.observe(), championships.observeClosedSnapshots()) { teamList, closed ->
                CareerState(
                    teams = teamList.associateBy { it.id.value },
                    board = Career.board(teamList, closed),
                )
            }.collect { _state.value = it }
        }
    }

    fun currentState(): CareerState = _state.value
    fun watch(onEach: (CareerState) -> Unit): WatchHandle = state.observe(onEach)
}
