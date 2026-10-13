import PhotosUI
import SwiftUI
import Shared

final class TeamFormModel: ObservableObject {
    let vm: TeamFormViewModel
    @Published var state: TeamFormState
    private var watch: WatchHandle?

    init(container: AppContainer, teamId: String?) {
        let vm = container.teamForm(id: teamId)
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

struct TeamFormScreen: View {
    @Environment(\.dismiss) private var dismiss
    @StateObject private var model: TeamFormModel
    @State private var picker: PhotosPickerItem?
    @State private var photoData: Data?
    @State private var keepPhoto = true

    init(container: AppContainer, teamId: String?) {
        _model = StateObject(wrappedValue: TeamFormModel(container: container, teamId: teamId))
    }

    var body: some View {
        Form {
            Section(String(localized: "team_section")) {
                TextField(String(localized: "team_name"), text: Binding(
                    get: { model.state.name },
                    set: { model.vm.setName(value: $0) }
                ))
                TextField(String(localized: "team_description"), text: Binding(
                    get: { model.state.about },
                    set: { model.vm.setDescription(value: $0) }
                ), axis: .vertical)
                PhotosPicker(selection: $picker, matching: .images) {
                    Label(String(localized: "team_photo"), systemImage: "camera")
                }
            }
        }
        .scrollContentBackground(.hidden)
        .disputaScreen()
        .navigationTitle(model.state.id == nil ? String(localized: "team_new") : String(localized: "team_edit"))
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(String(localized: "save")) {
                    model.vm.save(photoBytes: photoData.map(KotlinByteArray.from), keepExistingPhoto: keepPhoto && photoData == nil)
                }
            }
        }
        .onChange(of: picker) { _, item in
            Task {
                if let item, let data = try? await item.loadTransferable(type: Data.self) {
                    photoData = data
                    keepPhoto = false
                }
            }
        }
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

extension KotlinByteArray {
    static func from(_ data: Data) -> KotlinByteArray {
        let array = KotlinByteArray(size: Int32(data.count))
        data.enumerated().forEach { index, byte in
            array.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return array
    }
}
