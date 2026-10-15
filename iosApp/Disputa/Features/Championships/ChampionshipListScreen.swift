import SwiftUI
import Shared

final class ChampionshipListModel: ObservableObject {
    let vm: ChampionshipListViewModel
    @Published var state: ChampionshipListState
    private var watch: WatchHandle?

    init(container: AppContainer) {
        let vm = container.championshipList()
        self.vm = vm
        self.state = vm.currentState()
        self.watch = vm.watch { [weak self] next in
            DispatchQueue.main.async { self?.state = next }
        }
    }

    deinit {
        watch?.cancel()
        vm.clear()
    }
}

struct ChampionshipListScreen: View {
    let container: AppContainer
    @StateObject private var model: ChampionshipListModel
    @State private var showWizard = false

    init(container: AppContainer) {
        self.container = container
        _model = StateObject(wrappedValue: ChampionshipListModel(container: container))
    }

    var body: some View {
        NavigationStack {
            Group {
                if model.state.items.isEmpty {
                    ContentUnavailableView(String(localized: "champs_empty_title"), systemImage: "trophy", description: Text("champs_empty_body"))
                } else {
                    List {
                        ForEach(model.state.items, id: \.id.value) { item in
                            NavigationLink {
                                ChampionshipHubScreen(container: container, championshipId: item.id.value)
                            } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(item.name).font(.headline).foregroundStyle(DisputaTheme.lemon)
                                    Text("\(item.dateIso) · \(item.format.label) · \(item.status == .closed ? String(localized: "status_closed") : String(localized: "status_open"))")
                                        .font(.subheadline)
                                        .foregroundStyle(DisputaTheme.muted)
                                }
                            }
                            .listRowBackground(DisputaTheme.surface)
                            .swipeActions {
                                Button(role: .destructive) {
                                    model.vm.delete(id: item.id)
                                } label: {
                                    Label(String(localized: "delete"), systemImage: "trash")
                                }
                            }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .disputaScreen()
            .navigationTitle(String(localized: "tab_championships"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showWizard = true
                    } label: {
                        Image(systemName: "plus").foregroundStyle(DisputaTheme.mango)
                    }
                }
            }
            .sheet(isPresented: $showWizard) {
                ChampionshipWizardScreen(container: container)
            }
        }
    }
}

extension ChampionshipFormat {
    var label: String {
        switch self {
        case .leagueonly: String(localized: "format_league")
        case .knockoutonly: String(localized: "format_knockout")
        case .groupsthenknockout: String(localized: "format_groups")
        case .leaguethenknockout: String(localized: "format_league_ko")
        default: String(describing: self)
        }
    }
}
