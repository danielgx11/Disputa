package disputa.commons

sealed class Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>()
    data class Err(
        val error: DomainError,
        val tiedTeamIds: List<String> = emptyList(),
        val proposedExtraIds: List<String> = emptyList(),
    ) : Outcome<Nothing>()
}

inline fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Ok)?.value
