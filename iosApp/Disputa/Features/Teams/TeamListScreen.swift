import SwiftUI
import Shared
import UIKit

final class TeamListModel: ObservableObject {
    let vm: TeamListViewModel
    @Published var state: TeamListState
    private var watch: WatchHandle?

    init(container: AppContainer) {
        let vm = container.teamList()
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

struct TeamListScreen: View {
    let container: AppContainer
    @StateObject private var model: TeamListModel
    @State private var path = NavigationPath()

    init(container: AppContainer) {
        self.container = container
        _model = StateObject(wrappedValue: TeamListModel(container: container))
    }

    var body: some View {
        NavigationStack(path: $path) {
            Group {
                if model.state.teams.isEmpty {
                    ContentUnavailableView(String(localized: "teams_empty_title"), systemImage: "person.crop.circle.badge.plus", description: Text("teams_empty_body"))
                        .foregroundStyle(DisputaTheme.muted)
                } else {
                    List {
                        ForEach(model.state.teams, id: \.id.value) { team in
                            NavigationLink(value: team.id.value) {
                                TeamRow(team: team)
                            }
                            .listRowBackground(DisputaTheme.surface)
                            .swipeActions {
                                Button(role: .destructive) {
                                    model.vm.delete(id: team.id)
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
            .navigationTitle(String(localized: "tab_teams"))
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        path.append("new")
                    } label: {
                        Image(systemName: "plus").foregroundStyle(DisputaTheme.mango)
                    }
                }
            }
            .navigationDestination(for: String.self) { id in
                TeamFormScreen(container: container, teamId: id == "new" ? nil : id)
            }
            .alert(String(localized: "error_title"), isPresented: Binding(
                get: { model.state.error != nil },
                set: { if !$0 { model.vm.consumeError() } }
            )) {
                Button(String(localized: "ok"), role: .cancel) { model.vm.consumeError() }
            } message: {
                Text(model.state.error?.localizedMessage ?? "")
            }
        }
    }
}

struct TeamRow: View {
    let team: Team

    var body: some View {
        HStack(spacing: 12) {
            TeamPhoto(path: team.photoRelativePath, size: 44)
            VStack(alignment: .leading, spacing: 4) {
                Text(team.name).font(.headline).foregroundStyle(DisputaTheme.lemon)
                if let about = team.about, !about.isEmpty {
                    Text(about).font(.subheadline).foregroundStyle(DisputaTheme.muted)
                }
            }
        }
    }
}

struct TeamPhoto: View {
    let path: String?
    var size: CGFloat = 56

    var body: some View {
        Group {
            if let path, let image = UIImage(contentsOfFile: resolved(path)) {
                Image(uiImage: image).resizable().scaledToFill()
            } else {
                Image(systemName: "shield.fill")
                    .foregroundStyle(DisputaTheme.mango)
            }
        }
        .frame(width: size, height: size)
        .background(DisputaTheme.blueberry)
        .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
    }

    private func resolved(_ relative: String) -> String {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first
        return docs?.appendingPathComponent(relative).path ?? relative
    }
}
