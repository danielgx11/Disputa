import SwiftUI
import Shared

final class ScoreModel: ObservableObject {
    let vm: ScoreEntryViewModel
    @Published var state: ScoreEntryViewModel.State
    private var watch: WatchHandle?

    init(container: AppContainer, championshipId: String, matchId: String) {
        let vm = container.scoreEntry(championshipId: championshipId, matchId: matchId)
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

struct ScoreSheet: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var model: ScoreModel

    init(container: AppContainer, championshipId: String, match: Match) {
        _model = StateObject(wrappedValue: ScoreModel(container: container, championshipId: championshipId, matchId: match.id.value))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                HStack {
                    stepper(Int(model.state.goalsA)) { model.vm.setGoalsA(value: Int32($0)) }
                    Text("x").font(.largeTitle).foregroundStyle(DisputaTheme.lemon)
                    stepper(Int(model.state.goalsB)) { model.vm.setGoalsB(value: Int32($0)) }
                }
                if model.state.needsPenalties && model.state.goalsA == model.state.goalsB {
                    Picker(String(localized: "penalty_winner"), selection: Binding(
                        get: { model.state.penaltyWinner ?? "" },
                        set: { model.vm.setPenaltyWinner(id: $0.isEmpty ? nil : $0) }
                    )) {
                        Text("—").tag("")
                        if let match = model.state.match {
                            if let a = match.teamA { Text(a.value).tag(a.value) }
                            if let b = match.teamB { Text(b.value).tag(b.value) }
                        }
                    }
                }
                Button(String(localized: "save"), action: model.vm.save).buttonStyle(PrimaryButtonStyle())
                Button(String(localized: "clear_score"), action: model.vm.clearScore)
                    .foregroundStyle(DisputaTheme.mango)
            }
            .padding()
            .disputaScreen()
            .navigationTitle(String(localized: "score_title"))
            .onChange(of: model.state.saved) { _, saved in
                if saved { dismiss() }
            }
            .alert(String(localized: "error_title"), isPresented: Binding(
                get: { model.state.error != nil },
                set: { _ in }
            )) {
                Button(String(localized: "ok"), role: .cancel) {}
            } message: {
                Text(model.state.error?.localizedMessage ?? "")
            }
        }
    }

    private func stepper(_ value: Int, set: @escaping (Int) -> Void) -> some View {
        VStack {
            Text("\(value)").font(.system(size: 48, weight: .bold)).foregroundStyle(DisputaTheme.lemon)
            HStack {
                Button("-") { set(max(0, value - 1)) }
                Button("+") { set(value + 1) }
            }
            .buttonStyle(.bordered)
        }
    }
}
