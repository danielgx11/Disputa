package disputa.domain

import disputa.commons.TeamId
import disputa.commons.randomId
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.rules.ChampionshipDraft
import disputa.domain.rules.SetupRules
import disputa.domain.rules.TeamRules
import kotlinx.datetime.LocalDate

internal fun teamId(label: String) = TeamId(label)

internal fun snapshot(
    format: ChampionshipFormat,
    labels: List<String>,
    qualifierCount: Int? = null,
    thirdPlace: Boolean = false,
    groups: List<List<String>>? = null,
    seed: Long = 1L,
): ChampionshipSnapshot {
    val ids = labels.map { teamId(it) }
    val draft = ChampionshipDraft(
        name = "Copa",
        date = LocalDate(2026, 10, 9),
        format = format,
        teamIds = ids,
        qualifierCount = qualifierCount,
        thirdPlaceEnabled = thirdPlace,
        groups = groups?.map { g -> g.map { teamId(it) } },
        drawGroupsAtRandom = groups == null && format == ChampionshipFormat.GroupsThenKnockout,
        fixtureSeed = seed,
    )
    return (SetupRules.create(draft) as disputa.commons.Outcome.Ok).value
}

internal fun namedTeam(label: String, name: String = label) =
    (TeamRules.create(name, null, null, teamId(label)) as disputa.commons.Outcome.Ok).value

internal fun freshId() = randomId()
