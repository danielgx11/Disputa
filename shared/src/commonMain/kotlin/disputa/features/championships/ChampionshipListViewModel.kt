package disputa.features.championships

import disputa.commons.WatchHandle
import disputa.commons.ChampionshipId
import disputa.domain.model.Championship
import disputa.features.BaseViewModel
import disputa.storage.ChampionshipRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChampionshipListState(
    val items: List<Championship> = emptyList(),
)

class ChampionshipListViewModel(
    private val championships: ChampionshipRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(ChampionshipListState())
    val state: StateFlow<ChampionshipListState> = _state.asStateFlow()

    init {
        scope.launch {
            championships.observe().collect { items ->
                _state.value = ChampionshipListState(items)
            }
        }
    }

    fun currentState(): ChampionshipListState = _state.value
    fun watch(onEach: (ChampionshipListState) -> Unit): WatchHandle = state.observe(onEach)

    fun delete(id: ChampionshipId) {
        scope.launch { championships.delete(id) }
    }
}
