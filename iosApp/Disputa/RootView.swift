import SwiftUI
import Shared

struct RootView: View {
    let container: AppContainer

    var body: some View {
        TabView {
            ChampionshipListScreen(container: container)
                .tabItem { Label(String(localized: "tab_championships"), systemImage: "trophy") }
            CareerScreen(container: container)
                .tabItem { Label(String(localized: "tab_rankings"), systemImage: "list.number") }
            TeamListScreen(container: container)
                .tabItem { Label(String(localized: "tab_teams"), systemImage: "person.3") }
        }
        .tint(DisputaTheme.mango)
    }
}
