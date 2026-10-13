package disputa.storage

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.domain.model.Team
import disputa.domain.rules.TeamRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TeamRepository(
    private val db: DisputaDatabase,
    private val photoStore: PhotoStore,
) {
    fun observe(): Flow<List<Team>> =
        db.disputaQueries.selectAllTeams().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    fun get(id: TeamId): Team? = db.disputaQueries.selectTeam(id.value).executeAsOneOrNull()?.toDomain()

    fun isUsed(id: TeamId): Boolean = db.disputaQueries.teamUsed(id.value).executeAsOne() > 0

    fun save(
        existingId: TeamId?,
        name: String,
        about: String?,
        photoBytes: ByteArray?,
        keepExistingPhoto: Boolean,
    ): Outcome<Team> {
        val current = existingId?.let { get(it) }
        val built = if (current == null) {
            TeamRules.create(name, about, null)
        } else {
            TeamRules.edit(current, name, about, current.photoRelativePath)
        }
        val team = when (built) {
            is Outcome.Err -> return built
            is Outcome.Ok -> built.value
        }
        val photoPath = when {
            photoBytes != null -> {
                current?.photoRelativePath?.let { photoStore.delete(it) }
                photoStore.save(team.id.value, photoBytes)
            }
            keepExistingPhoto -> current?.photoRelativePath
            else -> {
                current?.photoRelativePath?.let { photoStore.delete(it) }
                null
            }
        }
        val stored = team.copy(photoRelativePath = photoPath)
        db.disputaQueries.insertTeam(stored.id.value, stored.name, stored.about, stored.photoRelativePath)
        return Outcome.Ok(stored)
    }

    fun delete(id: TeamId): Outcome<Unit> {
        if (isUsed(id)) return Outcome.Err(DomainError.TeamInUse)
        val existing = get(id)
        existing?.photoRelativePath?.let { photoStore.delete(it) }
        db.disputaQueries.deleteTeam(id.value)
        return Outcome.Ok(Unit)
    }
}
