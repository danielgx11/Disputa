package disputa.domain.rules

import disputa.commons.DomainError
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.commons.randomId
import disputa.domain.model.Team

object TeamRules {
    fun create(
        name: String,
        about: String?,
        photoRelativePath: String?,
        id: TeamId = TeamId(randomId()),
    ): Outcome<Team> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Outcome.Err(DomainError.TeamNameRequired)
        val desc = about?.trim()?.ifEmpty { null }
        return Outcome.Ok(
            Team(
                id = id,
                name = trimmed,
                about = desc,
                photoRelativePath = photoRelativePath,
            ),
        )
    }

    fun edit(existing: Team, name: String, about: String?, photoRelativePath: String?): Outcome<Team> {
        return create(name, about, photoRelativePath, existing.id)
    }
}
