package disputa.features.championships

import disputa.commons.WatchHandle
import disputa.commons.ChampionshipId
import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.Team
import disputa.domain.rules.ChampionshipDraft
import disputa.features.BaseViewModel
import disputa.storage.ChampionshipRepository
import disputa.storage.TeamRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlin.random.Random

data class ChampionshipWizardState(
    val name: String = "",
    val year: Int = 2026,
    val month: Int = 10,
    val day: Int = 9,
    val format: ChampionshipFormat = ChampionshipFormat.LeagueThenKnockout,
    val teams: List<Team> = emptyList(),
    val selected: Set<String> = emptySet(),
    val groupCount: Int = 2,
    val drawGroups: Boolean = true,
    val qualifierCount: Int = 4,
    val thirdPlace: Boolean = true,
    val createdId: ChampionshipId? = null,
    val error: DomainError? = null,
)

class ChampionshipWizardViewModel(
    private val teams: TeamRepository,
    private val championships: ChampionshipRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(ChampionshipWizardState())
    val state: StateFlow<ChampionshipWizardState> = _state.asStateFlow()

    init {
        scope.launch {
            teams.observe().collect { list ->
                _state.update { it.copy(teams = list) }
            }
        }
    }

    fun currentState(): ChampionshipWizardState = _state.value
    fun watch(onEach: (ChampionshipWizardState) -> Unit): WatchHandle = state.observe(onEach)

    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setDate(year: Int, month: Int, day: Int) = _state.update { it.copy(year = year, month = month, day = day) }
    fun setFormat(format: ChampionshipFormat) = _state.update { it.copy(format = format) }
    fun toggleTeam(id: TeamId) = _state.update {
        val next = it.selected.toMutableSet()
        if (!next.add(id.value)) next.remove(id.value)
        it.copy(selected = next)
    }
    fun setGroupCount(value: Int) = _state.update { it.copy(groupCount = value.coerceAtLeast(2)) }
    fun setDrawGroups(value: Boolean) = _state.update { it.copy(drawGroups = value) }
    fun setQualifierCount(value: Int) = _state.update { it.copy(qualifierCount = value.coerceAtLeast(1)) }
    fun setThirdPlace(value: Boolean) = _state.update { it.copy(thirdPlace = value) }

    fun submit() {
        scope.launch {
            val current = _state.value
            val ids = current.selected.map(::TeamId)
            val groups = if (current.format == ChampionshipFormat.GroupsThenKnockout && !current.drawGroups) {
                val size = ids.size / current.groupCount
                if (size < 2) {
                    _state.update { it.copy(error = DomainError.UnevenGroups) }
                    return@launch
                }
                ids.chunked(size)
            } else if (current.format == ChampionshipFormat.GroupsThenKnockout) {
                List(current.groupCount) { emptyList() }
            } else {
                null
            }
            val qualifier = when (current.format) {
                ChampionshipFormat.LeagueThenKnockout, ChampionshipFormat.GroupsThenKnockout -> current.qualifierCount
                else -> null
            }
            val draft = ChampionshipDraft(
                name = current.name,
                date = LocalDate(current.year, current.month, current.day),
                format = current.format,
                teamIds = ids,
                qualifierCount = qualifier,
                thirdPlaceEnabled = current.thirdPlace,
                groups = groups,
                drawGroupsAtRandom = current.format == ChampionshipFormat.GroupsThenKnockout && current.drawGroups,
                fixtureSeed = Random.Default.nextLong(),
            )
            when (val result = championships.create(draft)) {
                is Outcome.Ok -> _state.update { it.copy(createdId = result.value.championship.id, error = null) }
                is Outcome.Err -> _state.update { it.copy(error = result.error) }
            }
        }
    }
}
