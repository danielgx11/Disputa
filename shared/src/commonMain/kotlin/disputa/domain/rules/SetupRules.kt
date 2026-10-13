package disputa.domain.rules

import disputa.commons.ChampionshipId
import disputa.commons.DomainError
import disputa.commons.GroupId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.commons.randomId
import disputa.domain.model.Championship
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.Group
import disputa.domain.model.KnockoutSeeding
import kotlinx.datetime.LocalDate
import kotlin.random.Random

data class ChampionshipDraft(
    val name: String,
    val date: LocalDate,
    val format: ChampionshipFormat,
    val teamIds: List<TeamId>,
    val qualifierCount: Int?,
    val thirdPlaceEnabled: Boolean,
    val groups: List<List<TeamId>>?,
    val drawGroupsAtRandom: Boolean,
    val fixtureSeed: Long,
)

object SetupRules {
    fun create(draft: ChampionshipDraft, id: ChampionshipId = ChampionshipId(randomId())): Outcome<ChampionshipSnapshot> {
        val name = draft.name.trim()
        if (name.isEmpty()) return Outcome.Err(DomainError.ChampionshipNameRequired)
        val teams = draft.teamIds.distinct()
        if (teams.size < 2) return Outcome.Err(DomainError.NotEnoughTeams)
        if (teams.size != draft.teamIds.size) return Outcome.Err(DomainError.SameTeamTwice)

        when (draft.format) {
            ChampionshipFormat.KnockoutOnly -> {
                if (!isPowerOfTwo(teams.size)) {
                    return Outcome.Err(DomainError.KnockoutTeamCountNotPowerOfTwo)
                }
            }
            ChampionshipFormat.LeagueThenKnockout -> {
                val n = draft.qualifierCount ?: return Outcome.Err(DomainError.QualifierCountInvalid)
                if (n < 2 || n > teams.size) return Outcome.Err(DomainError.QualifierCountInvalid)
            }
            ChampionshipFormat.GroupsThenKnockout -> {
                val perGroup = draft.qualifierCount ?: return Outcome.Err(DomainError.QualifierCountInvalid)
                if (perGroup < 1) return Outcome.Err(DomainError.QualifierCountInvalid)
            }
            ChampionshipFormat.LeagueOnly -> Unit
        }

        val groups = when (draft.format) {
            ChampionshipFormat.GroupsThenKnockout -> {
                buildGroups(draft, teams) ?: return groupError(draft, teams)
            }
            else -> emptyList()
        }

        if (draft.format == ChampionshipFormat.GroupsThenKnockout) {
            val perGroup = draft.qualifierCount!!
            if (groups.any { it.teamIds.size < 2 }) return Outcome.Err(DomainError.GroupsTooSmall)
            if (perGroup > groups.first().teamIds.size) return Outcome.Err(DomainError.QualifierCountInvalid)
        }

        val knockoutSize = estimatedKnockoutSize(draft.format, teams.size, groups.size, draft.qualifierCount)
        val thirdPlace = draft.thirdPlaceEnabled && knockoutSize >= 4
        if (draft.thirdPlaceEnabled && knockoutSize < 4 && draft.format != ChampionshipFormat.LeagueOnly) {
            return Outcome.Err(DomainError.ThirdPlaceNotAllowed)
        }

        val championship = Championship(
            id = id,
            name = name,
            date = draft.date,
            format = draft.format,
            status = ChampionshipStatus.Open,
            qualifierCount = draft.qualifierCount,
            thirdPlaceEnabled = thirdPlace,
            fixtureSeed = draft.fixtureSeed,
            legCount = 1,
            knockoutSeeding = KnockoutSeeding.ByPosition,
        )
        return Outcome.Ok(
            ChampionshipSnapshot(
                championship = championship,
                teamIds = teams,
                groups = groups,
                matches = emptyList(),
            ),
        )
    }

    private fun estimatedKnockoutSize(
        format: ChampionshipFormat,
        teamCount: Int,
        groupCount: Int,
        qualifierCount: Int?,
    ): Int = when (format) {
        ChampionshipFormat.LeagueOnly -> 0
        ChampionshipFormat.KnockoutOnly -> teamCount
        ChampionshipFormat.LeagueThenKnockout -> qualifierCount ?: 0
        ChampionshipFormat.GroupsThenKnockout -> (qualifierCount ?: 0) * groupCount
    }

    private fun groupError(draft: ChampionshipDraft, teams: List<TeamId>): Outcome.Err {
        val proposed = draft.groups
        if (proposed == null && !draft.drawGroupsAtRandom) return Outcome.Err(DomainError.NeedTwoGroups)
        if (proposed != null && proposed.size < 2) return Outcome.Err(DomainError.NeedTwoGroups)
        if (proposed != null && proposed.map { it.size }.distinct().size > 1) {
            return Outcome.Err(DomainError.UnevenGroups)
        }
        if (teams.size < 4) return Outcome.Err(DomainError.GroupsTooSmall)
        return Outcome.Err(DomainError.UnevenGroups)
    }

    private fun buildGroups(draft: ChampionshipDraft, teams: List<TeamId>): List<Group>? {
        val buckets: List<List<TeamId>> = when {
            draft.drawGroupsAtRandom -> {
                val groupCount = draft.groups?.size
                    ?: if (teams.size % 2 == 0 && teams.size >= 4) 2 else return null
                if (teams.size % groupCount != 0) return null
                val shuffled = teams.shuffled(Random(draft.fixtureSeed))
                shuffled.chunked(teams.size / groupCount)
            }
            draft.groups != null -> draft.groups
            else -> return null
        }
        if (buckets.size < 2) return null
        if (buckets.any { it.size < 2 }) return null
        if (buckets.map { it.size }.distinct().size != 1) return null
        val assigned = buckets.flatten()
        if (assigned.toSet() != teams.toSet()) return null
        return buckets.mapIndexed { index, ids ->
            Group(
                id = GroupId(randomId()),
                name = groupName(index),
                teamIds = ids,
            )
        }
    }

    fun groupName(index: Int): String = ('A' + index).toString()
}
