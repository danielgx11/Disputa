import SwiftUI
import Shared

@main
struct DisputaApp: App {
    private let container = AppGraph.shared

    var body: some Scene {
        WindowGroup {
            RootView(container: container)
                .preferredColorScheme(.dark)
        }
    }
}

enum AppGraph {
    static let shared: AppContainer = IosAppContainerKt.createIosAppContainer()
}
