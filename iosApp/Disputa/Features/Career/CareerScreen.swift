import SwiftUI
import Shared

final class CareerModel: ObservableObject {
    let vm: CareerViewModel
    @Published var state: CareerState
    private var watch: WatchHandle?

    init(container: AppContainer) {
        let vm = container.career()
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

struct CareerScreen: View {
    @StateObject private var model: CareerModel
    @State private var tab = 0

    init(container: AppContainer) {
        _model = StateObject(wrappedValue: CareerModel(container: container))
    }

    var body: some View {
        NavigationStack {
            VStack {
                Picker("", selection: $tab) {
                    Text("career_titles").tag(0)
                    Text("career_points").tag(1)
                }
                .pickerStyle(.segmented)
                .padding()
                if model.state.board.titles.allSatisfy({ $0.titles == 0 }) && tab == 0 {
                    ContentUnavailableView(String(localized: "career_empty_title"), systemImage: "chart.bar", description: Text("career_empty_body"))
                } else {
                    List {
                        if tab == 0 {
                            ForEach(model.state.board.titles, id: \.teamId.value) { row in
                                HStack {
                                    Text(model.state.teams[row.teamId.value]?.name ?? row.teamId.value)
                                        .foregroundStyle(DisputaTheme.lemon)
                                    Spacer()
                                    Text("\(row.titles)").foregroundStyle(DisputaTheme.mango)
                                }
                                .listRowBackground(DisputaTheme.surface)
                            }
                        } else {
                            ForEach(model.state.board.points, id: \.teamId.value) { row in
                                VStack(alignment: .leading, spacing: 4) {
                                    HStack {
                                        Text(model.state.teams[row.teamId.value]?.name ?? row.teamId.value)
                                            .foregroundStyle(DisputaTheme.lemon)
                                        Spacer()
                                        Text("\(row.points) pts").foregroundStyle(DisputaTheme.mango)
                                    }
                                    Text("GP \(row.goalsFor)  GC \(row.goalsAgainst)")
                                        .font(.caption)
                                        .foregroundStyle(DisputaTheme.muted)
                                }
                                .listRowBackground(DisputaTheme.surface)
                            }
                        }
                    }
                    .scrollContentBackground(.hidden)
                }
            }
            .disputaScreen()
            .navigationTitle(String(localized: "tab_rankings"))
        }
    }
}
