package disputa.features.teams

import disputa.commons.WatchHandle
import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.Team
import disputa.features.BaseViewModel
import disputa.storage.TeamRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamListState(
    val teams: List<Team> = emptyList(),
    val error: DomainError? = null,
)

class TeamListViewModel(
    private val teams: TeamRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(TeamListState())
    val state: StateFlow<TeamListState> = _state.asStateFlow()

    init {
        scope.launch {
            teams.observe().collect { list ->
                _state.update { it.copy(teams = list) }
            }
        }
    }

    fun currentState(): TeamListState = _state.value

    fun watch(onEach: (TeamListState) -> Unit): WatchHandle = state.observe(onEach)

    fun delete(id: TeamId) {
        scope.launch {
            when (val result = teams.delete(id)) {
                is Outcome.Ok -> _state.update { it.copy(error = null) }
                is Outcome.Err -> _state.update { it.copy(error = result.error) }
            }
        }
    }

    fun consumeError() {
        _state.update { it.copy(error = null) }
    }
}
