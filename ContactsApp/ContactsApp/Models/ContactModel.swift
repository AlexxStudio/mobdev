import Foundation
import Contacts

struct ContactModel: Identifiable {
    let id: String
    let firstName: String
    let lastName: String
    let phoneNumbers: [String]
    let emails: [String]
    let organization: String
    let jobTitle: String
    let note: String

    var fullName: String {
        let name = [firstName, lastName]
            .filter { !$0.isEmpty }
            .joined(separator: " ")

        return name.isEmpty ? "Без имени" : name
    }

    var primaryPhone: String? {
        phoneNumbers.first
    }

    var hasEmail: Bool {
        !emails.isEmpty
    }
}
