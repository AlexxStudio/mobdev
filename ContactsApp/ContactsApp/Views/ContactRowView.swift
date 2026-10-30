import SwiftUI

struct ContactRowView: View {
    let contact: ContactModel

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 42))
                .foregroundStyle(.blue)

            VStack(alignment: .leading, spacing: 5) {
                Text(contact.fullName)
                    .font(.headline)
                    .lineLimit(1)

                if let phone = contact.primaryPhone {
                    Text(phone)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                } else {
                    Text("Нет номера телефона")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            if contact.hasEmail {
                Image(systemName: "envelope.fill")
                    .foregroundStyle(.blue)
                    .accessibilityLabel("Есть email")
            }
        }
        .padding(.vertical, 5)
    }
}
