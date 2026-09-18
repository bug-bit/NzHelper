import SwiftUI

struct ContentView: View {
    @Environment(\.openURL) private var openURL

    @State private var showUpdateAlert = false
    @State private var updateURL: String = ""

    var body: some View {
        MainTabView()
            .task {
                if let release = await UpdateChecker.fetchLatestVersion() {
                    if UpdateChecker.isNewer(release.version, than: UpdateChecker.currentVersion) {
                        updateURL = release.url
                        showUpdateAlert = true
                    }
                }
            }
            .alert("发现新版本", isPresented: $showUpdateAlert) {
                Button("以后再说", role: .cancel) {}
                Button("前往下载") {
                    let fallbackURL = URL(string: "https://github.com/bug-bit/NzHelper/releases")!
                    openURL(URL(string: updateURL) ?? fallbackURL)
                }
            } message: {
                Text("GitHub 上有新版本可用，是否前往下载？")
            }
    }
}
