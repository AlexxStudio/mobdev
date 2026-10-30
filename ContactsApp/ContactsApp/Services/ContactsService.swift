import Foundation
import Contacts

final class ContactsService {
    private let store = CNContactStore()

    func authorizationStatus() -> CNAuthorizationStatus {
        CNContactStore.authorizationStatus(for: .contacts)
    }

    func requestAccess() async throws -> Bool {
        try await store.requestAccess(for: .contacts)
    }

    func fetchContacts() async throws -> [ContactModel] {
        print("ВЫЗВАН fetchContacts()")
        let store = self.store

        return try await Task.detached(priority: .userInitiated) {
            let keys: [any CNKeyDescriptor] = [
                CNContactGivenNameKey as CNKeyDescriptor,
                CNContactFamilyNameKey as CNKeyDescriptor,
                CNContactPhoneNumbersKey as CNKeyDescriptor,
                CNContactEmailAddressesKey as CNKeyDescriptor,
                CNContactOrganizationNameKey as CNKeyDescriptor,
                CNContactJobTitleKey as CNKeyDescriptor
            ]

            let request = CNContactFetchRequest(keysToFetch: keys)
            var contacts: [ContactModel] = []

            try store.enumerateContacts(with: request) { contact, _ in
                let model = ContactModel(
                    id: contact.identifier,
                    firstName: contact.givenName,
                    lastName: contact.familyName,
                    phoneNumbers: contact.phoneNumbers.map {
                        $0.value.stringValue
                    },
                    emails: contact.emailAddresses.map {
                        String($0.value)
                    },
                    organization: contact.organizationName,
                    jobTitle: contact.jobTitle,
                    note: ""
                )

                contacts.append(model)
            }

            return contacts
        }.value
    }
}
