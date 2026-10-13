import Combine
import Shared

final class ObservableBox<Vm: AnyObject, State: AnyObject>: ObservableObject {
    let vm: Vm
    @Published var state: State
    private var watch: WatchHandle?

    init(vm: Vm, current: State, start: (@escaping (State) -> Void) -> WatchHandle) {
        self.vm = vm
        self.state = current
        self.watch = start { [weak self] next in
            DispatchQueue.main.async {
                self?.state = next
            }
        }
    }

    deinit {
        watch?.cancel()
        (vm as? BaseViewModel)?.clear()
    }
}
