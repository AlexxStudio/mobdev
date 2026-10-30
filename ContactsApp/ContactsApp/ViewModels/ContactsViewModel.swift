import Foundation
import Contacts
import SwiftUI

enum ContactsScreenState {
    case idle
    case loading
    case loaded
    case permissionDenied
    case error(String)
}

@MainActor
final class ContactsViewModel: ObservableObject {

    @Published private(set) var contacts: [ContactModel] = []
    @Published var searchText = ""
    @Published var sortAscending = true
    @Published private(set) var state: ContactsScreenState = .idle

    private let service = ContactsService()
    private var hasLoadedContacts = false
    private var isLoading = false

    var filteredContacts: [ContactModel] {
        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)

        let filtered = query.isEmpty
            ? contacts
            : contacts.filter {
                $0.fullName.localizedStandardContains(query)
            }

        return filtered.sorted {
            let comparison = $0.fullName.localizedStandardCompare($1.fullName)

            if comparison == .orderedSame {
                return sortAscending
                    ? $0.id < $1.id
                    : $0.id > $1.id
            }

            return sortAscending
                ? comparison == .orderedAscending
                : comparison == .orderedDescending
        }
    }

    var contactsCount: Int {
        filteredContacts.count
    }

    func checkPermissionAndLoad() async {
        switch service.authorizationStatus() {
        case .authorized, .limited:
            await loadContactsIfNeeded()

        case .notDetermined:
            await requestPermission()

        case .denied, .restricted:
            state = .permissionDenied

        @unknown default:
            state = .permissionDenied
        }
    }

    func requestPermission() async {
        let status = service.authorizationStatus()

        switch status {
        case .authorized, .limited:
            await loadContactsIfNeeded()

        case .denied, .restricted:
            state = .permissionDenied

        case .notDetermined:
            do {
                let granted = try await service.requestAccess()

                if granted {
                    await loadContactsIfNeeded()
                } else {
                    state = .permissionDenied
                }
            } catch {
                state = .permissionDenied
            }

        @unknown default:
            state = .permissionDenied
        }
    }

    func loadContactsIfNeeded() async {
        guard !hasLoadedContacts && !isLoading else { return }

        isLoading = true
        state = .loading
        defer { isLoading = false }

        do {
            contacts = try await service.fetchContacts()
            hasLoadedContacts = true
            state = .loaded
        } catch {
            state = .error(error.localizedDescription)
        }
    }

    func refreshAfterSettings() async {
        let status = service.authorizationStatus()

        switch status {
        case .authorized, .limited:
            if !hasLoadedContacts {
                await loadContactsIfNeeded()
            } else {
                state = .loaded
            }

        case .denied, .restricted:
            contacts = []
            hasLoadedContacts = false
            state = .permissionDenied

        case .notDetermined:
            state = .idle

        @unknown default:
            state = .permissionDenied
        }
    }

    func toggleSort() {
        sortAscending.toggle()
    }
}
