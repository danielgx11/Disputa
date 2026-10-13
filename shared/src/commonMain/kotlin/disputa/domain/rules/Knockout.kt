package disputa.domain.rules

import disputa.commons.ChampionshipId
import disputa.commons.DomainError
import disputa.commons.MatchId
import disputa.commons.Outcome
import disputa.commons.TeamId
import disputa.commons.randomId
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.Group
import disputa.domain.model.Match
import disputa.domain.model.MatchPhase
import disputa.domain.model.StandingRow
import disputa.domain.model.StandingsTable

object Knockout {
    fun bracketPositions(n: Int): List<Int> {
        require(isPowerOfTwo(n))
        var positions = listOf(1, 2)
        while (positions.size < n) {
            val m = positions.size * 2
            val next = mutableListOf<Int>()
            for (p in positions) {
                next += p
                next += m + 1 - p
            }
            positions = next
        }
        return positions
    }

    fun pairSeeds(n: Int): List<Pair<Int, Int>> {
        val slots = bracketPositions(n)
        return slots.chunked(2).map { it[0] to it[1] }
    }

    fun generateFromSeeds(
        championshipId: ChampionshipId,
        seeds: List<TeamId>,
        thirdPlace: Boolean,
        groupOf: Map<TeamId, disputa.commons.GroupId> = emptyMap(),
    ): Outcome<List<Match>> {
        val n = seeds.size
        if (!isPowerOfTwo(n)) return Outcome.Err(DomainError.BracketNotPowerOfTwo)
        var pairs = pairSeeds(n).map { (s1, s2) -> seeds[s1 - 1] to seeds[s2 - 1] }
        if (groupOf.isNotEmpty()) {
            pairs = avoidSameGroup(pairs, groupOf)
        }
        val matches = mutableListOf<Match>()
        val firstRoundCount = n / 2
        pairs.forEachIndexed { slot, (a, b) ->
            matches += match(championshipId, MatchPhase.Knockout, roundIndex = 0, slot = slot, a, b)
        }
        var remaining = firstRoundCount
        var round = 1
        while (remaining > 1) {
            remaining /= 2
            repeat(remaining) { slot ->
                matches += match(championshipId, MatchPhase.Knockout, roundIndex = round, slot = slot, null, null)
            }
            round += 1
        }
        if (thirdPlace && n >= 4) {
            matches += match(championshipId, MatchPhase.ThirdPlace, roundIndex = 0, slot = 0, null, null)
        }
        return Outcome.Ok(matches)
    }

    fun orderedQualifiers(
        snapshot: ChampionshipSnapshot,
        extraIds: List<TeamId>,
    ): Outcome<List<TeamId>> {
        val championship = snapshot.championship
        return when (championship.format) {
            disputa.domain.model.ChampionshipFormat.KnockoutOnly -> Outcome.Ok(snapshot.teamIds)
            disputa.domain.model.ChampionshipFormat.LeagueOnly -> Outcome.Err(DomainError.BracketNotPowerOfTwo)
            disputa.domain.model.ChampionshipFormat.LeagueThenKnockout -> {
                val n = championship.qualifierCount ?: return Outcome.Err(DomainError.QualifierCountInvalid)
                val table = leagueTable(snapshot)
                cutQualifiers(table, n, extraIds, snapshot.teamIds)
            }
            disputa.domain.model.ChampionshipFormat.GroupsThenKnockout -> {
                val perGroup = championship.qualifierCount ?: return Outcome.Err(DomainError.QualifierCountInvalid)
                groupQualifiers(snapshot, perGroup, extraIds)
            }
        }
    }

    fun groupOf(snapshot: ChampionshipSnapshot): Map<TeamId, disputa.commons.GroupId> =
        snapshot.groups.flatMap { group -> group.teamIds.map { it to group.id } }.toMap()

    fun leagueTable(snapshot: ChampionshipSnapshot): StandingsTable {
        val league = snapshot.matches.filter { it.phase == MatchPhase.League }
        return Standings.compute(snapshot.teamIds, league)
    }

    fun groupTable(snapshot: ChampionshipSnapshot, group: Group): StandingsTable {
        val matches = snapshot.matches.filter { it.phase == MatchPhase.Group && it.groupId == group.id }
        return Standings.compute(group.teamIds, matches)
    }

    private fun cutQualifiers(
        table: StandingsTable,
        take: Int,
        extraIds: List<TeamId>,
        allTeams: List<TeamId>,
    ): Outcome<List<TeamId>> {
        val blocked = Standings.cutLineUnresolved(table, take)
        if (blocked != null) return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = blocked.map { it.value })
        val automatic = table.rows.take(take).map { it.teamId }
        val fieldSize = nextPowerOfTwo(automatic.size)
        if (automatic.size == fieldSize) return Outcome.Ok(automatic)
        val needed = fieldSize - automatic.size
        val remainingRows = table.rows.drop(take)
        val remainingBlocked = Standings.cutLineUnresolved(
            StandingsTable(remainingRows.mapIndexed { i, row -> row.copy(rank = i + 1) }, remainingRows.filter { it.unresolved }.map { it.teamId }),
            needed,
        )
        if (remainingBlocked != null) {
            return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = remainingBlocked.map { it.value })
        }
        val proposed = remainingRows.take(needed).map { it.teamId }
        if (extraIds.isEmpty()) {
            return Outcome.Err(DomainError.FillQualifiersRequired, proposedExtraIds = proposed.map { it.value })
        }
        if (extraIds.toSet() != proposed.toSet() && extraIds.size != needed) {
            val stillValid = extraIds.all { it in remainingRows.map { row -> row.teamId } } && extraIds.size == needed
            if (!stillValid) return Outcome.Err(DomainError.FillQualifiersRequired, proposedExtraIds = proposed.map { it.value })
        }
        return Outcome.Ok(automatic + extraIds)
    }

    private fun groupQualifiers(
        snapshot: ChampionshipSnapshot,
        perGroup: Int,
        extraIds: List<TeamId>,
    ): Outcome<List<TeamId>> {
        if (snapshot.groups.size == 2 && perGroup == 2) {
            val first = snapshot.groups[0]
            val second = snapshot.groups[1]
            val tableA = groupTable(snapshot, first)
            val tableB = groupTable(snapshot, second)
            val blockedA = Standings.cutLineUnresolved(tableA, 2)
            val blockedB = Standings.cutLineUnresolved(tableB, 2)
            if (blockedA != null) return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = blockedA.map { it.value })
            if (blockedB != null) return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = blockedB.map { it.value })
            val seeds = listOf(
                tableA.rows[0].teamId,
                tableB.rows[0].teamId,
                tableA.rows[1].teamId,
                tableB.rows[1].teamId,
            )
            return Outcome.Ok(seeds)
        }
        val pots = mutableListOf<MutableList<StandingRow>>()
        repeat(perGroup) { pots.add(mutableListOf()) }
        val leftovers = mutableListOf<StandingRow>()
        for (group in snapshot.groups) {
            val table = groupTable(snapshot, group)
            val blocked = Standings.cutLineUnresolved(table, perGroup)
            if (blocked != null) return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = blocked.map { it.value })
            table.rows.take(perGroup).forEachIndexed { index, row -> pots[index] += row }
            leftovers += table.rows.drop(perGroup)
        }
        fun sortPot(rows: List<StandingRow>): Outcome<List<TeamId>> {
            val ordered = rows.sortedWith(
                compareByDescending<StandingRow> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor },
            )
            val unresolved = ordered.zipWithNext().any { (a, b) ->
                a.points == b.points && a.goalDifference == b.goalDifference && a.goalsFor == b.goalsFor
            }
            if (unresolved) {
                val tied = ordered.filter { row ->
                    ordered.any { other ->
                        other.teamId != row.teamId &&
                            other.points == row.points &&
                            other.goalDifference == row.goalDifference &&
                            other.goalsFor == row.goalsFor
                    }
                }
                return Outcome.Err(DomainError.UnresolvedTie, tiedTeamIds = tied.map { it.teamId.value })
            }
            return Outcome.Ok(ordered.map { it.teamId })
        }
        val seeds = mutableListOf<TeamId>()
        for (pot in pots) {
            when (val ordered = sortPot(pot)) {
                is Outcome.Err -> return ordered
                is Outcome.Ok -> seeds += ordered.value
            }
        }
        val fieldSize = nextPowerOfTwo(seeds.size)
        if (seeds.size == fieldSize) return Outcome.Ok(seeds)
        val needed = fieldSize - seeds.size
        when (val extra = sortPot(leftovers)) {
            is Outcome.Err -> return extra
            is Outcome.Ok -> {
                val proposed = extra.value.take(needed)
                if (extraIds.isEmpty()) {
                    return Outcome.Err(DomainError.FillQualifiersRequired, proposedExtraIds = proposed.map { it.value })
                }
                if (extraIds.size != needed) {
                    return Outcome.Err(DomainError.FillQualifiersRequired, proposedExtraIds = proposed.map { it.value })
                }
                return Outcome.Ok(seeds + extraIds)
            }
        }
    }

    private fun avoidSameGroup(
        pairs: List<Pair<TeamId, TeamId>>,
        groupOf: Map<TeamId, disputa.commons.GroupId>,
    ): List<Pair<TeamId, TeamId>> {
        val result = pairs.toMutableList()
        for (i in result.indices) {
            val (a, b) = result[i]
            val ga = groupOf[a] ?: continue
            val gb = groupOf[b] ?: continue
            if (ga != gb) continue
            val swapAt = result.indices.firstOrNull { j ->
                if (j == i) return@firstOrNull false
                val (c, d) = result[j]
                groupOf[d] != ga && groupOf[a] != groupOf[d] && groupOf[c] != groupOf[b]
            }
            if (swapAt != null) {
                val (c, d) = result[swapAt]
                result[i] = a to d
                result[swapAt] = c to b
            }
        }
        return result
    }

    private fun match(
        championshipId: ChampionshipId,
        phase: MatchPhase,
        roundIndex: Int,
        slot: Int,
        a: TeamId?,
        b: TeamId?,
    ) = Match(
        id = MatchId(randomId()),
        championshipId = championshipId,
        phase = phase,
        groupId = null,
        roundIndex = roundIndex,
        slot = slot,
        teamA = a,
        teamB = b,
        goalsA = null,
        goalsB = null,
        penaltyWinner = null,
    )
}
