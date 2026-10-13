package disputa.domain.rules

import disputa.commons.TeamId
import disputa.domain.model.CareerBoard
import disputa.domain.model.ChampionshipFormat
import disputa.domain.model.ChampionshipSnapshot
import disputa.domain.model.ChampionshipStatus
import disputa.domain.model.MatchPhase
import disputa.domain.model.PointsRow
import disputa.domain.model.Team
import disputa.domain.model.TitleRow

object Career {
    fun championOf(snapshot: ChampionshipSnapshot): TeamId? {
        if (snapshot.championship.status != ChampionshipStatus.Closed) return null
        return when (snapshot.championship.format) {
            ChampionshipFormat.LeagueOnly -> {
                val table = Standings.compute(
                    snapshot.teamIds,
                    snapshot.matches.filter { it.phase == MatchPhase.League },
                )
                Standings.uniqueFirst(table)
            }
            else -> {
                val finalMatch = snapshot.matches
                    .filter { it.phase == MatchPhase.Knockout }
                    .maxByOrNull { it.roundIndex }
                finalMatch?.let { Scoring.winner(it) }
            }
        }
    }

    fun board(teams: List<Team>, closed: List<ChampionshipSnapshot>): CareerBoard {
        val titles = mutableMapOf<TeamId, Int>()
        val points = mutableMapOf<TeamId, Int>()
        val gf = mutableMapOf<TeamId, Int>()
        val ga = mutableMapOf<TeamId, Int>()
        teams.forEach { team ->
            titles[team.id] = 0
            points[team.id] = 0
            gf[team.id] = 0
            ga[team.id] = 0
        }
        for (snapshot in closed.filter { it.championship.status == ChampionshipStatus.Closed }) {
            championOf(snapshot)?.let { titles[it] = (titles[it] ?: 0) + 1 }
            snapshot.matches.filter { it.isPlayed && it.teamA != null && it.teamB != null }.forEach { match ->
                val a = match.teamA!!
                val b = match.teamB!!
                gf[a] = (gf[a] ?: 0) + match.goalsA!!
                ga[a] = (ga[a] ?: 0) + match.goalsB!!
                gf[b] = (gf[b] ?: 0) + match.goalsB!!
                ga[b] = (ga[b] ?: 0) + match.goalsA!!
                if (match.phase == MatchPhase.League || match.phase == MatchPhase.Group) {
                    when {
                        match.goalsA!! > match.goalsB!! -> points[a] = (points[a] ?: 0) + 3
                        match.goalsA!! < match.goalsB!! -> points[b] = (points[b] ?: 0) + 3
                        else -> {
                            points[a] = (points[a] ?: 0) + 1
                            points[b] = (points[b] ?: 0) + 1
                        }
                    }
                }
            }
        }
        val names = teams.associate { it.id to it.name }
        val titleRows = teams.map { TitleRow(it.id, titles.getValue(it.id)) }
            .sortedWith(compareByDescending<TitleRow> { it.titles }.thenBy { names[it.teamId] })
        val pointRows = teams.map {
            PointsRow(it.id, points.getValue(it.id), gf.getValue(it.id), ga.getValue(it.id))
        }.sortedWith(
            compareByDescending<PointsRow> { it.points }
                .thenByDescending { it.goalsFor }
                .thenBy { it.goalsAgainst }
                .thenBy { names[it.teamId] },
        )
        return CareerBoard(titles = titleRows, points = pointRows)
    }
}
