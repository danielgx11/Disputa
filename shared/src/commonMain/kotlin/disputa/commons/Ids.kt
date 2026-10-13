package disputa.commons

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class TeamId(val value: String)

data class ChampionshipId(val value: String)

data class MatchId(val value: String)

data class GroupId(val value: String)

@OptIn(ExperimentalUuidApi::class)
fun randomId(): String = Uuid.random().toString()
