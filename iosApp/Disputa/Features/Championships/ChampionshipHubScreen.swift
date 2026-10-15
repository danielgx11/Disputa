import SwiftUI
import Shared

final class HubModel: ObservableObject {
    let vm: ChampionshipHubViewModel
    @Published var state: ChampionshipHubState
    private var watch: WatchHandle?

    init(container: AppContainer, id: String) {
        let vm = container.championshipHub(id: id)
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

struct ChampionshipHubScreen: View {
    let container: AppContainer
    @StateObject private var model: HubModel
    @State private var tab = 0
    @State private var score: Match?

    init(container: AppContainer, championshipId: String) {
        self.container = container
        _model = StateObject(wrappedValue: HubModel(container: container, id: championshipId))
    }

    var body: some View {
        VStack(spacing: 16) {
            Picker("", selection: $tab) {
                Text("hub_table").tag(0)
                Text("hub_matches").tag(1)
                Text("hub_bracket").tag(2)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal)
            if model.state.snapshot?.championship.status == .closed {
                Text("\(String(localized: "status_closed")) · \(String(format: String(localized: "champion_line"), model.state.championName ?? "—"))")
                    .font(.headline)
                    .foregroundStyle(DisputaTheme.lemon)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal)
            }
            ScrollView {
                switch tab {
                case 0: table
                case 1: matches(model.state.leagueMatches)
                default: matches(model.state.knockoutMatches)
                }
            }
            actions
        }
        .disputaScreen()
        .navigationTitle(model.state.snapshot?.championship.name ?? "")
        .sheet(item: $score) { match in
            if let championshipId = model.state.snapshot?.championship.id.value {
                ScoreSheet(container: container, championshipId: championshipId, match: match)
            }
        }
        .alert(String(localized: "error_title"), isPresented: Binding(
            get: { model.state.error != nil },
            set: { if !$0 { model.vm.consumeError() } }
        )) {
            if model.state.error == .fillqualifiersrequired {
                Button(String(localized: "confirm_fill")) { model.vm.generateKnockout() }
            }
            Button(String(localized: "ok"), role: .cancel) { model.vm.consumeError() }
        } message: {
            Text(model.state.error?.localizedMessage ?? "")
        }
    }

    @ViewBuilder private var table: some View {
        if !model.state.standings.isEmpty {
            standingsCard(model.state.standings)
        }
        ForEach(Array(model.state.groupStandings.keys.sorted()), id: \.self) { name in
            VStack(alignment: .leading) {
                Text("Grupo \(name)").font(.headline).foregroundStyle(DisputaTheme.lemon)
                standingsCard(model.state.groupStandings[name] ?? [])
            }
            .padding(.horizontal)
        }
    }

    private func standingsCard(_ rows: [StandingRow]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(rows, id: \.teamId.value) { row in
                HStack {
                    Text("\(row.rank)").foregroundStyle(DisputaTheme.lemon).frame(width: 24)
                    Text(model.state.teams[row.teamId.value]?.name ?? row.teamId.value)
                    Spacer()
                    Text("\(row.points) pts  \(row.goalsFor):\(row.goalsAgainst)")
                        .foregroundStyle(DisputaTheme.muted)
                }
                .padding(.vertical, 4)
            }
        }
        .modifier(Card())
        .padding(.horizontal)
    }

    private func matches(_ items: [Match]) -> some View {
        VStack(spacing: 8) {
            if items.isEmpty {
                Text(String(localized: "matches_empty")).foregroundStyle(DisputaTheme.muted)
            }
            ForEach(items, id: \.id.value) { match in
                Button {
                    score = match
                } label: {
                    HStack {
                        Text(name(match.teamA)).frame(maxWidth: .infinity, alignment: .trailing)
                        Text(scoreText(match)).font(.headline).foregroundStyle(DisputaTheme.mango)
                        Text(name(match.teamB)).frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .foregroundStyle(DisputaTheme.text)
                    .modifier(Card())
                }
            }
        }
        .padding(.horizontal)
    }

    private var actions: some View {
        VStack(spacing: 8) {
            if model.state.leagueMatches.isEmpty && model.state.snapshot?.championship.format != .knockoutonly {
                Button(String(localized: "generate_fixtures"), action: model.vm.generateFixtures).buttonStyle(PrimaryButtonStyle())
            }
            if model.state.knockoutMatches.isEmpty && model.state.snapshot?.championship.format != .leagueonly {
                Button(String(localized: "generate_bracket"), action: model.vm.generateKnockout).buttonStyle(PrimaryButtonStyle())
            }
            if model.state.snapshot?.championship.status != .closed {
                Button(String(localized: "close_championship"), action: model.vm.finishChampionship).buttonStyle(PrimaryButtonStyle())
            }
        }
        .padding()
    }

    private func name(_ id: TeamId?) -> String {
        guard let id else { return "—" }
        return model.state.teams[id.value]?.name ?? id.value
    }

    private func scoreText(_ match: Match) -> String {
        if let a = match.goalsA, let b = match.goalsB {
            return "\(a) x \(b)"
        }
        return "vs"
    }
}

extension Match: Identifiable {}
