package disputa.features.teams

import disputa.commons.WatchHandle
import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.features.BaseViewModel
import disputa.storage.TeamRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamFormState(
    val id: TeamId? = null,
    val name: String = "",
    val about: String = "",
    val photoRelativePath: String? = null,
    val saved: Boolean = false,
    val error: DomainError? = null,
)

class TeamFormViewModel(
    private val teams: TeamRepository,
    existingId: TeamId?,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel(dispatcher) {
    private val _state = MutableStateFlow(
        existingId?.let { teams.get(it) }?.let { team ->
            TeamFormState(id = team.id, name = team.name, about = team.about.orEmpty(), photoRelativePath = team.photoRelativePath)
        } ?: TeamFormState(id = existingId),
    )
    val state: StateFlow<TeamFormState> = _state.asStateFlow()

    fun currentState(): TeamFormState = _state.value
    fun watch(onEach: (TeamFormState) -> Unit): WatchHandle = state.observe(onEach)

    fun setName(value: String) {
        _state.update { it.copy(name = value, error = null) }
    }

    fun setDescription(value: String) {
        _state.update { it.copy(about = value) }
    }

    fun save(photoBytes: ByteArray?, keepExistingPhoto: Boolean) {
        scope.launch {
            val current = _state.value
            when (
                val result = teams.save(
                    existingId = current.id,
                    name = current.name,
                    about = current.about,
                    photoBytes = photoBytes,
                    keepExistingPhoto = keepExistingPhoto,
                )
            ) {
                is Outcome.Ok -> _state.update {
                    it.copy(id = result.value.id, saved = true, photoRelativePath = result.value.photoRelativePath, error = null)
                }
                is Outcome.Err -> _state.update { it.copy(error = result.error, saved = false) }
            }
        }
    }
}
