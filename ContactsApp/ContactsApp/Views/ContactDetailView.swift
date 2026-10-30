import SwiftUI

struct ContactDetailView: View {
    let contact: ContactModel

    var body: some View {
        List {
            Section {
                VStack(spacing: 12) {
                    Image(systemName: "person.crop.circle.fill")
                        .font(.system(size: 80))
                        .foregroundStyle(.blue)

                    Text(contact.fullName)
                        .font(.title2)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 20)
            }

            if !contact.phoneNumbers.isEmpty {
                Section("Телефоны") {
                    ForEach(
                        Array(contact.phoneNumbers.enumerated()),
                        id: \.offset
                    ) { _, phone in
                        Label(phone, systemImage: "phone")
                    }
                }
            }

            if !contact.emails.isEmpty {
                Section("Email") {
                    ForEach(
                        Array(contact.emails.enumerated()),
                        id: \.offset
                    ) { _, email in
                        Label(email, systemImage: "envelope")
                    }
                }
            }

            if !contact.organization.isEmpty {
                Section("Организация") {
                    Text(contact.organization)
                }
            }

            if !contact.jobTitle.isEmpty {
                Section("Должность") {
                    Text(contact.jobTitle)
                }
            }

            if !contact.note.isEmpty {
                Section("Дополнительно") {
                    Text(contact.note)
                }
            }
        }
        .navigationTitle("Контакт")
        .navigationBarTitleDisplayMode(.inline)
    }
}

