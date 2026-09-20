import SwiftUI

@main
struct NZhelperApp: App {
    @StateObject private var timerManager = TimerManager()
    @StateObject private var repository = SessionRepository.shared

    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(timerManager)
                .environmentObject(repository)
                .onChange(of: scenePhase) { _, newPhase in
                    switch newPhase {
                    case .active:
                        timerManager.handleEnterForeground()
                        DailyReminderScheduler.shared.refresh(for: repository.sessions)
                    case .background, .inactive:
                        timerManager.handleEnterBackground()
                    @unknown default:
                        break
                    }
                }
        }
    }
}
