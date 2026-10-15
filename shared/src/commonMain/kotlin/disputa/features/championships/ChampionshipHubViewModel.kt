package disputa.features.championships

import disputa.commons.WatchHandle
import disputa.commons.ChampionshipId
import disputa.commons.DomainError
import disputa.commons.MatchId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase
import disputa.domain.model.StandingRow
import disputa.domain.model.Team
import disputa.domain.rules.Career
import disputa.domain.rules.Knockout
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

data class ChampionshipHubState(
    val snapshot: ChampionshipSnapshot? = null,
    val teams: Map<String, Team> = emptyMap(),
    val standings: List<StandingRow> = emptyList(),
    val groupStandings: Map<String, List<StandingRow>> = emptyMap(),
    val proposedExtras: List<String> = emptyList(),
    val championName: String? = null,
    val error: DomainError? = null,
) {
    val leagueMatches: List<Match>
        get() = snapshot?.matches?.filter { it.phase == MatchPhase.League || it.phase == MatchPhase.Group }.orEmpty()
    val knockoutMatches: List<Match>
        get() = snapshot?.matches?.filter { it.phase == MatchPhase.Knockout || it.phase == MatchPhase.ThirdPlace }.orEmpty()
}

class ChampionshipHubViewModel(
    private val id: ChampionshipId,
    private val championships: ChampionshipRepository,
    private val teams: TeamRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(ChampionshipHubState())
    val state: StateFlow<ChampionshipHubState> = _state.asStateFlow()

    init {
        refresh()
        scope.launch {
            championships.observe().collect { refresh() }
        }
    }

    fun currentState(): ChampionshipHubState = _state.value
    fun watch(onEach: (ChampionshipHubState) -> Unit): WatchHandle = state.observe(onEach)

    fun generateFixtures() = run("fixtures") { championships.generateFixtures(id) }
    fun generateKnockout() = run("ko") {
        championships.generateKnockout(id, _state.value.proposedExtras.map(::TeamId))
    }
    fun finishChampionship() = run("finish") { championships.close(id) }
    fun delete() {
        scope.launch { championships.delete(id) }
    }

    fun consumeError() = _state.update { it.copy(error = null) }

    private fun run(op: String, block: suspend () -> Outcome<ChampionshipSnapshot>) {
        scope.launch {
            when (val result = block()) {
                is Outcome.Ok -> applySnapshot(result.value, extras = emptyList(), error = null)
                is Outcome.Err -> {
                    if (result.error == DomainError.FillQualifiersRequired) {
                        _state.update { it.copy(proposedExtras = result.proposedExtraIds, error = result.error) }
                    } else {
                        _state.update { it.copy(error = result.error) }
                    }
                }
            }
        }
    }

    internal fun refresh() {
        val snapshot = championships.getSnapshot(id) ?: return
        applySnapshot(snapshot, _state.value.proposedExtras, _state.value.error)
    }

    private fun applySnapshot(snapshot: ChampionshipSnapshot, extras: List<String>, error: DomainError?) {
        val teamMap = snapshot.teamIds.mapNotNull { teams.get(it) }.associateBy { it.id.value }
        val standings = if (snapshot.groups.isEmpty()) {
            Knockout.leagueTable(snapshot).rows
        } else {
            emptyList()
        }
        val groups = snapshot.groups.associate { group ->
            group.name to Knockout.groupTable(snapshot, group).rows
        }
        val championId = Career.championOf(snapshot)
        _state.value = ChampionshipHubState(
            snapshot = snapshot,
            teams = teamMap,
            standings = standings,
            groupStandings = groups,
            proposedExtras = extras,
            championName = championId?.let { teamMap[it.value]?.name },
            error = error,
        )
    }
}

class ScoreEntryViewModel(
    private val championshipId: ChampionshipId,
    private val matchId: MatchId,
    private val championships: ChampionshipRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    data class State(
        val match: Match? = null,
        val goalsA: Int = 0,
        val goalsB: Int = 0,
        val penaltyWinner: String? = null,
        val needsPenalties: Boolean = false,
        val error: DomainError? = null,
        val saved: Boolean = false,
        val locked: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        val match = championships.getSnapshot(championshipId)?.matches?.find { it.id == matchId }
        _state.value = State(
            match = match,
            goalsA = match?.goalsA ?: 0,
            goalsB = match?.goalsB ?: 0,
            penaltyWinner = match?.penaltyWinner?.value,
            needsPenalties = match?.phase == MatchPhase.Knockout || match?.phase == MatchPhase.ThirdPlace,
        )
    }

    fun currentState(): State = _state.value
    fun watch(onEach: (State) -> Unit): WatchHandle = state.observe(onEach)
    fun setGoalsA(value: Int) = _state.update { it.copy(goalsA = value.coerceAtLeast(0), saved = false) }
    fun setGoalsB(value: Int) = _state.update { it.copy(goalsB = value.coerceAtLeast(0), saved = false) }
    fun setPenaltyWinner(id: String?) = _state.update { it.copy(penaltyWinner = id) }

    fun save() {
        scope.launch {
            val current = _state.value
            val penalty = current.penaltyWinner?.let(::TeamId)?.takeIf { current.needsPenalties && current.goalsA == current.goalsB }
            when (
                val result = championships.applyScore(
                    championshipId,
                    matchId,
                    current.goalsA,
                    current.goalsB,
                    penalty,
                )
            ) {
                is Outcome.Ok -> _state.update { it.copy(saved = true, error = null, match = result.value.matches.first { m -> m.id == matchId }) }
                is Outcome.Err -> _state.update { it.copy(error = result.error, saved = false) }
            }
        }
    }

    fun clearScore() {
        scope.launch {
            when (val result = championships.clearScore(championshipId, matchId)) {
                is Outcome.Ok -> _state.update {
                    it.copy(goalsA = 0, goalsB = 0, penaltyWinner = null, saved = true, error = null)
                }
                is Outcome.Err -> _state.update { it.copy(error = result.error) }
            }
        }
    }
}
