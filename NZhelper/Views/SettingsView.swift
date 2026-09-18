import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var repository: SessionRepository

    @AppStorage(ReminderSettings.enabledKey) private var reminderEnabled = false
    @AppStorage(ReminderSettings.hourKey) private var reminderHour = ReminderSettings.defaultHour
    @AppStorage(ReminderSettings.minuteKey) private var reminderMinute = ReminderSettings.defaultMinute

    @State private var showPermissionAlert = false

    var body: some View {
        List {
            Section {
                Toggle("今天没有记录时提醒", isOn: reminderBinding)

                DatePicker(
                    "提醒时间",
                    selection: reminderTimeBinding,
                    displayedComponents: .hourAndMinute
                )
                .disabled(!reminderEnabled)
            } header: {
                Text("每日提醒")
            } footer: {
                Text("到设定时间时，如果当天还没有记录，会发送「今天要起飞吗？」通知。")
            }

            NavigationLink {
                AboutView()
            } label: {
                Label("关于", systemImage: "info.circle")
            }
        }
        .navigationTitle("设置")
        .alert("通知未开启", isPresented: $showPermissionAlert) {
            Button("知道了", role: .cancel) {}
        } message: {
            Text("请在系统设置中允许通知后再开启每日提醒。")
        }
        .onAppear {
            DailyReminderScheduler.shared.refresh(for: repository.sessions)
        }
        .onChange(of: reminderHour) { _, _ in
            DailyReminderScheduler.shared.refresh(for: repository.sessions)
        }
        .onChange(of: reminderMinute) { _, _ in
            DailyReminderScheduler.shared.refresh(for: repository.sessions)
        }
    }

    private var reminderBinding: Binding<Bool> {
        Binding {
            reminderEnabled
        } set: { newValue in
            if newValue {
                Task {
                    let granted = await DailyReminderScheduler.shared.requestAuthorization()
                    await MainActor.run {
                        reminderEnabled = granted
                        showPermissionAlert = !granted
                        DailyReminderScheduler.shared.refresh(for: repository.sessions)
                    }
                }
            } else {
                reminderEnabled = false
                DailyReminderScheduler.shared.cancel()
            }
        }
    }

    private var reminderTimeBinding: Binding<Date> {
        Binding {
            var components = DateComponents()
            components.hour = reminderHour
            components.minute = reminderMinute
            return Calendar.current.date(from: components) ?? Date()
        } set: { newValue in
            let components = Calendar.current.dateComponents([.hour, .minute], from: newValue)
            reminderHour = components.hour ?? ReminderSettings.defaultHour
            reminderMinute = components.minute ?? ReminderSettings.defaultMinute
        }
    }
}
