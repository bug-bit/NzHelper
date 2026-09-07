import Foundation

struct UpdateChecker {
    static let repoOwner = "bug-bit"
    static let repoName = "NzHelper"

    static func fetchLatestVersion() async -> (version: String, url: String)? {
        let url = URL(string: "https://api.github.com/repos/\(repoOwner)/\(repoName)/releases/latest")!
        var request = URLRequest(url: url)
        request.setValue("NzHelper-iOS/1.0", forHTTPHeaderField: "User-Agent")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard (response as? HTTPURLResponse)?.statusCode == 200 else { return nil }

            struct Release: Codable {
                let tag_name: String
                let name: String
                let html_url: String
            }
            let release = try JSONDecoder().decode(Release.self, from: data)
            return (release.tag_name, release.html_url)
        } catch {
            print("Update check failed: \(error)")
            return nil
        }
    }

    static var currentVersion: String {
        Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0"
    }

    static func isNewer(_ remote: String, than local: String) -> Bool {
        let remoteParts = remote
            .trimmingCharacters(in: CharacterSet(charactersIn: "v"))
            .split(separator: ".")
            .compactMap { Int($0) }
        let localParts = local
            .split(separator: ".")
            .compactMap { Int($0) }
        let maxLen = max(remoteParts.count, localParts.count)
        for i in 0..<maxLen {
            let r = i < remoteParts.count ? remoteParts[i] : 0
            let l = i < localParts.count ? localParts[i] : 0
            if r > l { return true }
            if r < l { return false }
        }
        return false
    }
}
