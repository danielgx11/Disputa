package disputa.domain.rules

import disputa.commons.TeamId
import disputa.domain.model.Match
import disputa.domain.model.StandingRow
import disputa.domain.model.StandingsTable

private data class Stats(
    var played: Int = 0,
    var won: Int = 0,
    var drawn: Int = 0,
    var lost: Int = 0,
    var goalsFor: Int = 0,
    var goalsAgainst: Int = 0,
    var points: Int = 0,
) {
    val gd: Int get() = goalsFor - goalsAgainst
    fun key(): Triple<Int, Int, Int> = Triple(points, gd, goalsFor)
}

object Standings {
    fun compute(teamIds: List<TeamId>, matches: List<Match>): StandingsTable {
        val stats = teamIds.associateWith { Stats() }
        val played = matches.filter { it.isPlayed && it.teamA != null && it.teamB != null }
        for (match in played) {
            apply(stats, match.teamA!!, match.teamB!!, match.goalsA!!, match.goalsB!!)
        }
        val clusters = teamIds.groupBy { stats.getValue(it).key() }
            .toList()
            .sortedWith(
                compareByDescending<Pair<Triple<Int, Int, Int>, List<TeamId>>> { it.first.first }
                    .thenByDescending { it.first.second }
                    .thenByDescending { it.first.third },
            )
        val ordered = mutableListOf<Pair<TeamId, Boolean>>()
        val unresolved = mutableListOf<TeamId>()
        for ((_, cluster) in clusters) {
            if (cluster.size == 1) {
                ordered += cluster.first() to false
                continue
            }
            val h2h = headToHeadStats(cluster, played)
            if (h2h == null) {
                cluster.forEach { ordered += it to true }
                unresolved += cluster
            } else {
                val h2hClusters = cluster.groupBy { h2h.getValue(it).key() }
                    .toList()
                    .sortedWith(
                        compareByDescending<Pair<Triple<Int, Int, Int>, List<TeamId>>> { it.first.first }
                            .thenByDescending { it.first.second }
                            .thenByDescending { it.first.third },
                    )
                for ((_, inner) in h2hClusters) {
                    val flag = inner.size > 1
                    inner.forEach { ordered += it to flag }
                    if (flag) unresolved += inner
                }
            }
        }
        val rows = ordered.mapIndexed { index, (id, flag) ->
            val s = stats.getValue(id)
            StandingRow(
                teamId = id,
                played = s.played,
                won = s.won,
                drawn = s.drawn,
                lost = s.lost,
                goalsFor = s.goalsFor,
                goalsAgainst = s.goalsAgainst,
                points = s.points,
                rank = index + 1,
                unresolved = flag,
            )
        }
        return StandingsTable(rows = rows, unresolvedTeamIds = unresolved.distinct())
    }

    fun cutLineUnresolved(table: StandingsTable, take: Int): List<TeamId>? {
        if (take <= 0 || take >= table.rows.size) return null
        val lastTaken = table.rows[take - 1]
        val firstLeft = table.rows[take]
        val sameBand = lastTaken.points == firstLeft.points &&
            lastTaken.goalDifference == firstLeft.goalDifference &&
            lastTaken.goalsFor == firstLeft.goalsFor
        if (sameBand && (lastTaken.unresolved || firstLeft.unresolved)) {
            return table.rows
                .filter {
                    it.points == lastTaken.points &&
                        it.goalDifference == lastTaken.goalDifference &&
                        it.goalsFor == lastTaken.goalsFor
                }
                .map { it.teamId }
        }
        return null
    }

    fun uniqueFirst(table: StandingsTable): TeamId? {
        val first = table.rows.firstOrNull() ?: return null
        if (first.unresolved) return null
        return first.teamId
    }

    private fun apply(stats: Map<TeamId, Stats>, a: TeamId, b: TeamId, ga: Int, gb: Int) {
        val sa = stats[a] ?: return
        val sb = stats[b] ?: return
        sa.played += 1
        sb.played += 1
        sa.goalsFor += ga
        sa.goalsAgainst += gb
        sb.goalsFor += gb
        sb.goalsAgainst += ga
        when {
            ga > gb -> {
                sa.won += 1
                sa.points += 3
                sb.lost += 1
            }
            ga < gb -> {
                sb.won += 1
                sb.points += 3
                sa.lost += 1
            }
            else -> {
                sa.drawn += 1
                sb.drawn += 1
                sa.points += 1
                sb.points += 1
            }
        }
    }

    private fun headToHeadStats(cluster: List<TeamId>, matches: List<Match>): Map<TeamId, Stats>? {
        for (i in cluster.indices) {
            for (j in i + 1 until cluster.size) {
                val a = cluster[i]
                val b = cluster[j]
                val played = matches.any { setOf(it.teamA, it.teamB) == setOf(a, b) }
                if (!played) return null
            }
        }
        val set = cluster.toSet()
        val stats = cluster.associateWith { Stats() }
        matches.filter { it.teamA in set && it.teamB in set }.forEach { match ->
            apply(stats, match.teamA!!, match.teamB!!, match.goalsA!!, match.goalsB!!)
        }
        return stats
    }
}
