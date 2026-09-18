import SwiftUI

struct LicenseItem: Identifiable {
    let id = UUID()
    let name: String
    let author: String
    let url: String
    let license: String
}

struct OpenSourceView: View {
    let licenses: [LicenseItem] = [
        LicenseItem(name: "SwiftUI", author: "Apple Inc.", url: "https://developer.apple.com/xcode/swiftui/", license: "Proprietary"),
        LicenseItem(name: "Swift Charts", author: "Apple Inc.", url: "https://developer.apple.com/documentation/charts", license: "Proprietary"),
        LicenseItem(name: "Foundation", author: "Apple Inc.", url: "https://developer.apple.com/documentation/foundation", license: "Proprietary"),
        LicenseItem(name: "Combine", author: "Apple Inc.", url: "https://developer.apple.com/documentation/combine", license: "Proprietary"),
        LicenseItem(name: "UniformTypeIdentifiers", author: "Apple Inc.", url: "https://developer.apple.com/documentation/uniformtypeidentifiers", license: "Proprietary"),
    ]

    var body: some View {
        List(licenses) { item in
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name).font(.body).bold()
                Text(item.author).font(.caption).foregroundStyle(.secondary)
                HStack {
                    if let url = URL(string: item.url) {
                        Link(destination: url) {
                            Text(item.url)
                                .font(.caption2)
                                .foregroundStyle(.blue)
                                .lineLimit(1)
                        }
                    }
                    Spacer()
                    Text(item.license)
                        .font(.caption2)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(.quaternary, in: RoundedRectangle(cornerRadius: 4))
                }
            }
            .padding(.vertical, 2)
        }
        .navigationTitle("开源许可")
        .navigationBarTitleDisplayMode(.inline)
    }
}
