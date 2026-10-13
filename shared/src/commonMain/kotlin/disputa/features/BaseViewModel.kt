package disputa.features

import disputa.commons.WatchHandle
import disputa.commons.watch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    fun <T> StateFlow<T>.observe(onEach: (T) -> Unit): WatchHandle = watch(scope, onEach)

    fun clear() {
        scope.cancel()
    }
}
