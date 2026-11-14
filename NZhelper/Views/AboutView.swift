import SwiftUI

struct AboutView: View {
    var body: some View {
        List {
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "chart.bar.doc.horizontal.fill")
                            .font(.system(size: 56))
                            .foregroundStyle(.blue)
                        Text("牛子小助手")
                            .font(.title2)
                            .bold()
                        Text("v\(version)")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                }
                .padding(.vertical, 16)
                .listRowBackground(Color.clear)
            }

            Section {
                Link(destination: URL(string: "https://github.com/bug-bit/NzHelper")!) {
                    HStack {
                        Label("GitHub 仓库", systemImage: "link")
                        Spacer()
                        Image(systemName: "arrow.up.right")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section {
                NavigationLink {
                    OpenSourceView()
                } label: {
                    Label("开源许可", systemImage: "doc.text")
                }
            }
        }
        .navigationTitle("关于")
        .navigationBarTitleDisplayMode(.inline)
    }

    var version: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }
}
