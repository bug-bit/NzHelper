import Combine
import Foundation

class SessionRepository: ObservableObject {
    static let shared = SessionRepository()

    @Published var sessions: [Session] = []

    private let fileURL: URL

    private init() {
        let docs = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first!
        fileURL = docs.appendingPathComponent("sessions.json")
        loadSessions()
    }

    func loadSessions() {
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            sessions = []
            DailyReminderScheduler.shared.refresh(for: sessions)
            return
        }
        do {
            let data = try Data(contentsOf: fileURL)
            sessions = try JSONDecoder.sessionDecoder.decode([Session].self, from: data)
        } catch {
            sessions = []
            print("Failed to load sessions: \(error)")
        }
        DailyReminderScheduler.shared.refresh(for: sessions)
    }

    func saveSessions() {
        do {
            let data = try JSONEncoder.sessionEncoder.encode(sessions)
            try data.write(to: fileURL, options: .atomic)
            DailyReminderScheduler.shared.refresh(for: sessions)
        } catch {
            print("Failed to save sessions: \(error)")
        }
    }

    func addSession(_ session: Session) {
        sessions.append(session)
        saveSessions()
    }

    func updateSession(_ session: Session) {
        if let index = sessions.firstIndex(where: { $0.id == session.id }) {
            sessions[index] = session
            saveSessions()
        }
    }

    func deleteSession(_ session: Session) {
        sessions.removeAll { $0.id == session.id }
        saveSessions()
    }

    func deleteAllSessions() {
        sessions.removeAll()
        saveSessions()
    }

    func exportSessions() -> URL? {
        do {
            let data = try JSONEncoder.sessionEncoder.encode(sessions)
            let temp = FileManager.default.temporaryDirectory
                .appendingPathComponent("NzHelper_export_\(Date().timeIntervalSince1970).json")
            try data.write(to: temp)
            return temp
        } catch {
            print("Export failed: \(error)")
            return nil
        }
    }

    func importSessions(from url: URL) -> Bool {
        let gotAccess = url.startAccessingSecurityScopedResource()
        defer { if gotAccess { url.stopAccessingSecurityScopedResource() } }

        do {
            let data = try Data(contentsOf: url)
            if let imported = try? JSONDecoder.sessionDecoder.decode([Session].self, from: data) {
                sessions.append(contentsOf: imported)
                saveSessions()
                return true
            }
            if let json = try? JSONSerialization.jsonObject(with: data) as? [[Any]] {
                var imported: [Session] = []
                for item in json {
                    guard item.count >= 9,
                          let ts = item[0] as? String,
                          let date = Session.dateFormatter.date(from: ts),
                          let duration = item[1] as? Int,
                          let remark = item[2] as? String,
                          let location = item[3] as? String,
                          let movie = item[4] as? Bool,
                          let climax = item[5] as? Bool,
                          let rating = item[6] as? Double,
                          let mood = item[7] as? String,
                          let props = item[8] as? String
                    else { continue }
                    imported.append(Session(
                        timestamp: date,
                        duration: duration,
                        remark: remark,
                        location: location,
                        watchedMovie: movie,
                        climax: climax,
                        rating: Float(rating),
                        mood: mood,
                        props: props
                    ))
                }
                sessions.append(contentsOf: imported)
                saveSessions()
                return true
            }
            return false
        } catch {
            print("Import failed: \(error)")
            return false
        }
    }
}
