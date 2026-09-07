import Foundation
import UserNotifications

struct ReminderSettings {
    static let enabledKey = "dailyReminderEnabled"
    static let hourKey = "dailyReminderHour"
    static let minuteKey = "dailyReminderMinute"

    static let defaultHour = 21
    static let defaultMinute = 0
}

final class DailyReminderScheduler {
    static let shared = DailyReminderScheduler()

    private let notificationIdentifierPrefix = "daily-no-session-reminder"
    private let scheduleDays = 30
    private let center = UNUserNotificationCenter.current()

    private init() {}

    func requestAuthorization() async -> Bool {
        do {
            return try await center.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            print("Notification authorization failed: \(error)")
            return false
        }
    }

    func refresh(for sessions: [Session]) {
        let defaults = UserDefaults.standard
        guard defaults.bool(forKey: ReminderSettings.enabledKey) else {
            cancel()
            return
        }

        let hour = normalizedHour(defaults.object(forKey: ReminderSettings.hourKey) as? Int)
        let minute = normalizedMinute(defaults.object(forKey: ReminderSettings.minuteKey) as? Int)

        center.getNotificationSettings { [weak self] settings in
            guard let self else { return }
            guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else {
                self.cancel()
                return
            }

            self.scheduleUpcomingReminders(hour: hour, minute: minute, sessions: sessions)
        }
    }

    func cancel() {
        center.getPendingNotificationRequests { [weak self] requests in
            guard let self else { return }
            let identifiers = requests
                .map(\.identifier)
                .filter { $0.hasPrefix(self.notificationIdentifierPrefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: identifiers)
        }
    }

    private func scheduleUpcomingReminders(hour: Int, minute: Int, sessions: [Session]) {
        cancel()

        let calendar = Calendar.current
        let now = Date()

        for dayOffset in 0..<scheduleDays {
            guard let day = calendar.date(byAdding: .day, value: dayOffset, to: now),
                  let reminderDate = reminderDate(on: day, hour: hour, minute: minute, calendar: calendar),
                  reminderDate > now
            else { continue }

            let hasRecord = sessions.contains { calendar.isDate($0.timestamp, inSameDayAs: reminderDate) }
            guard !hasRecord else { continue }

            let components = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: reminderDate)
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            let content = UNMutableNotificationContent()
            content.title = "今天要起飞吗？"
            content.body = "今天还没有记录，点开看看要不要记一下。"
            content.sound = .default

            let request = UNNotificationRequest(
                identifier: notificationIdentifier(for: reminderDate, calendar: calendar),
                content: content,
                trigger: trigger
            )

            center.add(request) { error in
                if let error {
                    print("Failed to schedule daily reminder: \(error)")
                }
            }
        }
    }

    private func reminderDate(on date: Date, hour: Int, minute: Int, calendar: Calendar) -> Date? {
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = hour
        components.minute = minute
        return calendar.date(from: components)
    }

    private func normalizedHour(_ hour: Int?) -> Int {
        guard let hour, (0...23).contains(hour) else {
            return ReminderSettings.defaultHour
        }
        return hour
    }

    private func normalizedMinute(_ minute: Int?) -> Int {
        guard let minute, (0...59).contains(minute) else {
            return ReminderSettings.defaultMinute
        }
        return minute
    }

    private func notificationIdentifier(for date: Date, calendar: Calendar) -> String {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let year = components.year ?? 0
        let month = components.month ?? 0
        let day = components.day ?? 0
        return "\(notificationIdentifierPrefix)-\(year)-\(month)-\(day)"
    }
}
