import SwiftUI
import Shared

final class WizardModel: ObservableObject {
    let vm: ChampionshipWizardViewModel
    @Published var state: ChampionshipWizardState
    private var watch: WatchHandle?

    init(container: AppContainer) {
        let vm = container.championshipWizard()
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

struct ChampionshipWizardScreen: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var model: WizardModel
    @State private var date = Date()

    init(container: AppContainer) {
        _model = StateObject(wrappedValue: WizardModel(container: container))
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(String(localized: "wizard_basics")) {
                    TextField(String(localized: "championship_name"), text: Binding(
                        get: { model.state.name },
                        set: { model.vm.setName(value: $0) }
                    ))
                    DatePicker(String(localized: "championship_date"), selection: $date, displayedComponents: .date)
                    Picker(String(localized: "championship_format"), selection: Binding(
                        get: { model.state.format },
                        set: { model.vm.setFormat(format: $0) }
                    )) {
                        Text("format_league").tag(ChampionshipFormat.leagueonly)
                        Text("format_knockout").tag(ChampionshipFormat.knockoutonly)
                        Text("format_groups").tag(ChampionshipFormat.groupsthenknockout)
                        Text("format_league_ko").tag(ChampionshipFormat.leaguethenknockout)
                    }
                }
                Section(String(localized: "wizard_teams")) {
                    ForEach(model.state.teams, id: \.id.value) { team in
                        Button {
                            model.vm.toggleTeam(id: team.id)
                        } label: {
                            HStack {
                                Text(team.name).foregroundStyle(DisputaTheme.text)
                                Spacer()
                                if model.state.selected.contains(team.id.value) {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(DisputaTheme.mango)
                                }
                            }
                        }
                    }
                }
                if model.state.format == .leaguethenknockout || model.state.format == .groupsthenknockout {
                    Section(String(localized: "wizard_knockout")) {
                        Stepper(value: Binding(
                            get: { Int(model.state.qualifierCount) },
                            set: { model.vm.setQualifierCount(value: Int32($0)) }
                        ), in: 1...16) {
                            Text("\(String(localized: "qualifiers")): \(model.state.qualifierCount)")
                        }
                        Toggle(String(localized: "third_place"), isOn: Binding(
                            get: { model.state.thirdPlace },
                            set: { model.vm.setThirdPlace(value: $0) }
                        ))
                    }
                }
                if model.state.format == .groupsthenknockout {
                    Section(String(localized: "wizard_groups")) {
                        Stepper(value: Binding(
                            get: { Int(model.state.groupCount) },
                            set: { model.vm.setGroupCount(value: Int32($0)) }
                        ), in: 2...8) {
                            Text("\(String(localized: "group_count")): \(model.state.groupCount)")
                        }
                        Toggle(String(localized: "draw_groups"), isOn: Binding(
                            get: { model.state.drawGroups },
                            set: { model.vm.setDrawGroups(value: $0) }
                        ))
                    }
                }
            }
            .scrollContentBackground(.hidden)
            .disputaScreen()
            .navigationTitle(String(localized: "wizard_title"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "cancel")) { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "create")) { model.vm.submit() }
                }
            }
            .onChange(of: date) { _, newDate in
                let parts = Calendar.current.dateComponents([.year, .month, .day], from: newDate)
                model.vm.setDate(year: Int32(parts.year ?? 2026), month: Int32(parts.month ?? 1), day: Int32(parts.day ?? 1))
            }
            .onChange(of: model.state.createdId) { _, id in
                if id != nil { dismiss() }
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
}
