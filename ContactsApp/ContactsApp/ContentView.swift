import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = ContactsViewModel()
    @State private var showSettingsAlert = false
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        NavigationStack {
            Group {
                switch viewModel.state {
                case .idle, .loading:
                    ProgressView("Загрузка контактов...")

                case .permissionDenied:
                    permissionDeniedView

                case .error(let message):
                    errorView(message)

                case .loaded:
                    contactsList
                }
            }
            .navigationTitle("Контакты")
            .toolbar {
                if case .loaded = viewModel.state {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            viewModel.toggleSort()
                        } label: {
                            Label(
                                viewModel.sortAscending ? "А–Я" : "Я–А",
                                systemImage: "arrow.up.arrow.down"
                            )
                        }
                    }
                }
            }
        }
        .task {
            await viewModel.checkPermissionAndLoad()
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .active {
                Task {
                    await viewModel.refreshAfterSettings()
                }
            }
        }
    }

    private var contactsList: some View {
        List {
            Section {
                if viewModel.filteredContacts.isEmpty {
                    ContentUnavailableView(
                        viewModel.searchText.isEmpty
                            ? "Контактов нет"
                            : "Ничего не найдено",
                        systemImage: "person.crop.circle.badge.questionmark",
                        description: Text(
                            viewModel.searchText.isEmpty
                                ? "Адресная книга пуста"
                                : "Попробуйте изменить запрос"
                        )
                    )
                } else {
                    ForEach(viewModel.filteredContacts) { contact in
                        NavigationLink {
                            ContactDetailView(contact: contact)
                        } label: {
                            ContactRowView(contact: contact)
                        }
                    }
                }
            } header: {
                Text("Найдено: \(viewModel.contactsCount)")
            }
        }
        .searchable(
            text: $viewModel.searchText,
            prompt: "Поиск по имени"
        )
    }

    private var permissionDeniedView: some View {
        ContentUnavailableView {
            Label(
                "Нет доступа к контактам",
                systemImage: "person.crop.circle.badge.xmark"
            )
        } description: {
            Text("Разрешите доступ к контактам в настройках устройства.")
        } actions: {
            Button("Запросить доступ") {
                showSettingsAlert = true
            }
            .alert("Доступ к контактам запрещён", isPresented: $showSettingsAlert) {
                Button("Открыть настройки") {
                    openSettings()
                }
                Button("Отмена", role: .cancel) {}
            } message: {
                Text("iOS не позволяет повторно запросить доступ. Разрешите доступ к контактам в настройках устройства.")
            }

        }
    }

    private func errorView(_ message: String) -> some View {
        ContentUnavailableView {
            Label("Ошибка загрузки", systemImage: "exclamationmark.triangle")
        } description: {
            Text(message)
        } actions: {
            Button("Повторить") {
                Task {
                    await viewModel.loadContactsIfNeeded()
                }
            }
        }
    }

    private func openSettings() {
        guard let url = URL(string: UIApplication.openSettingsURLString) else {
            return
        }
        UIApplication.shared.open(url)
    }
}

#Preview {
    ContentView()
}

