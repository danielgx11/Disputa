package disputa.commons

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun interface WatchHandle {
    fun cancel()
}

fun <T> StateFlow<T>.watch(scope: CoroutineScope, onEach: (T) -> Unit): WatchHandle {
    val job: Job = scope.launch {
        collect { onEach(it) }
    }
    return WatchHandle { job.cancel() }
}
