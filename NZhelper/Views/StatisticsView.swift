import SwiftUI
import Charts

enum ChartPeriod: String, CaseIterable {
    case week = "周"
    case month = "月"
    case year = "年"
}

struct StatisticsView: View {
    @EnvironmentObject var repository: SessionRepository
    @State private var selectedPeriod: ChartPeriod = .week

    var body: some View {
        Group {
            if repository.sessions.isEmpty {
                ContentUnavailableView(
                    "暂无统计",
                    systemImage: "chart.bar.fill",
                    description: Text("(。・ω・。)")
                )
            } else {
                ScrollView {
                    VStack(spacing: 16) {
                        latestSessionCard
                        overallStatsCard
                        chartCard
                        Spacer(minLength: 20)
                    }
                    .padding()
                }
            }
        }
        .navigationTitle("统计")
        .navigationBarTitleDisplayMode(.large)
    }

    var latestSessionCard: some View {
        let sorted = repository.sessions.sorted { $0.timestamp > $1.timestamp }
        guard let latest = sorted.first else { return AnyView(EmptyView()) }
        let daysAgo = Calendar.current.dateComponents([.day], from: latest.timestamp, to: Date()).day ?? 0
        let messages = latestStatusMessage(daysAgo: daysAgo)

        return AnyView(GroupBox {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "clock.badge.checkmark")
                        .font(.title2)
                        .foregroundStyle(.blue)
                    Text("最近一次记录")
                        .font(.headline)
                }
                Divider()
                HStack {
                    Text("时间").foregroundStyle(.secondary)
                    Spacer()
                    Text(formatDateTime(latest.timestamp)).font(.subheadline)
                }
                HStack {
                    Text("持续").foregroundStyle(.secondary)
                    Spacer()
                    Text(formatDuration(latest.duration)).bold().font(.subheadline)
                }
                Divider()
                Text(messages.status)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(messages.motivation)
                    .font(.callout)
            }
        } label: {
            Label("概况", systemImage: "person.fill")
        })
    }

    var overallStatsCard: some View {
        let sessions = repository.sessions
        let totalDuration = sessions.reduce(0) { $0 + $1.duration }
        let avgDuration = sessions.isEmpty ? 0 : totalDuration / sessions.count
        let calendar = Calendar.current
        let now = Date()
        let weekAgo = calendar.date(byAdding: .day, value: -7, to: now)!
        let monthAgo = calendar.date(byAdding: .month, value: -1, to: now)!
        let yearAgo = calendar.date(byAdding: .year, value: -1, to: now)!
        let weekCount = sessions.filter { $0.timestamp >= weekAgo }.count
        let monthCount = sessions.filter { $0.timestamp >= monthAgo }.count
        let yearCount = sessions.filter { $0.timestamp >= yearAgo }.count
        let count = sessions.count
        let countMessages = [
            "千里之行，始于足下",
            "坚持就是胜利",
            "你已经超越了99%的人",
            "你已成为大师",
            "数据量惊人，值得骄傲",
            "你是真正的记录之王"
        ]
        let msg = countMessages[min(count / 10, countMessages.count - 1)]

        return GroupBox {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "chart.bar.doc.horizontal")
                        .font(.title2)
                        .foregroundStyle(.green)
                    Text("总体统计").font(.headline)
                }
                Divider()
                StatRow(label: "总时长", value: formatDuration(totalDuration))
                StatRow(label: "平均时长", value: formatDuration(avgDuration))
                StatRow(label: "总次数", value: "\(count) 次")
                StatRow(label: "本周", value: "\(weekCount) 次")
                StatRow(label: "本月", value: "\(monthCount) 次")
                StatRow(label: "本年", value: "\(yearCount) 次")
                Divider()
                Text(msg).font(.callout).foregroundStyle(.secondary)
            }
        } label: {
            Label("数据", systemImage: "chart.bar.fill")
        }
    }

    var chartCard: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 12) {
                Picker("周期", selection: $selectedPeriod) {
                    ForEach(ChartPeriod.allCases, id: \.self) { p in
                        Text(p.rawValue).tag(p)
                    }
                }
                .pickerStyle(.segmented)

                switch selectedPeriod {
                case .week:
                    weekChartView
                case .month:
                    monthChartView
                case .year:
                    yearChartView
                }
            }
        } label: {
            Label("趋势", systemImage: "chart.xyaxis.line")
        }
    }

    var weekChartView: some View {
        let data = dailyData(days: 7)
        if data.allSatisfy({ $0.minutes == 0 }) {
            return AnyView(
                Text("本周暂无记录").foregroundStyle(.secondary).padding()
            )
        }
        return AnyView(
            Chart {
                ForEach(data, id: \.date) { item in
                    BarMark(
                        x: .value("日期", item.date, unit: .day),
                        y: .value("分钟", item.minutes)
                    )
                    .foregroundStyle(.blue.gradient)
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day)) { value in
                    AxisValueLabel(format: .dateTime.weekday(.abbreviated))
                }
            }
            .chartYAxis {
                AxisMarks {
                    AxisValueLabel()
                    AxisGridLine()
                }
            }
            .frame(height: 160)
        )
    }

    var monthChartView: some View {
        let data = monthDailyData()
        if data.allSatisfy({ $0.minutes == 0 }) {
            return AnyView(
                Text("本月暂无记录").foregroundStyle(.secondary).padding()
            )
        }
        return AnyView(
            Chart {
                ForEach(data, id: \.date) { item in
                    BarMark(
                        x: .value("日期", item.date, unit: .day),
                        y: .value("分钟", item.minutes)
                    )
                    .foregroundStyle(.orange.gradient)
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: 5)) { value in
                    AxisValueLabel(format: .dateTime.day())
                }
            }
            .chartYAxis {
                AxisMarks {
                    AxisValueLabel()
                    AxisGridLine()
                }
            }
            .frame(height: 160)
        )
    }

    var yearChartView: some View {
        let data = yearMonthlyData()
        if data.allSatisfy({ $0.count == 0 }) {
            return AnyView(
                Text("今年暂无记录").foregroundStyle(.secondary).padding()
            )
        }
        return AnyView(
            Chart {
                ForEach(data, id: \.month) { item in
                    BarMark(
                        x: .value("月份", item.month),
                        y: .value("次数", item.count)
                    )
                    .foregroundStyle(.purple.gradient)
                }
            }
            .chartXAxis {
                AxisMarks(values: .automatic) { value in
                    if let month = value.as(Int.self), month >= 1, month <= 12 {
                        AxisValueLabel("\(month)月")
                    }
                }
            }
            .chartYAxis {
                AxisMarks {
                    AxisValueLabel()
                    AxisGridLine()
                }
            }
            .frame(height: 160)
        )
    }

    struct DailyStat {
        let date: Date
        let minutes: Double
    }

    struct MonthlyStat {
        let month: Int
        let count: Int
        let minutes: Double
    }

    func dailyData(days: Int) -> [DailyStat] {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        var result: [DailyStat] = []
        for i in 0..<days {
            let day = calendar.date(byAdding: .day, value: -(days - 1 - i), to: today)!
            let dayEnd = calendar.date(byAdding: .day, value: 1, to: day)!
            let seconds = repository.sessions
                .filter { $0.timestamp >= day && $0.timestamp < dayEnd }
                .reduce(0) { $0 + $1.duration }
            result.append(DailyStat(date: day, minutes: Double(seconds) / 60.0))
        }
        return result
    }

    func monthDailyData() -> [DailyStat] {
        let calendar = Calendar.current
        let now = Date()
        let range = calendar.range(of: .day, in: .month, for: now)!
        let components = calendar.dateComponents([.year, .month], from: now)
        let startOfMonth = calendar.date(from: components)!
        var result: [DailyStat] = []
        for day in range {
            let dayDate = calendar.date(byAdding: .day, value: day - 1, to: startOfMonth)!
            let dayEnd = calendar.date(byAdding: .day, value: 1, to: dayDate)!
            let seconds = repository.sessions
                .filter { $0.timestamp >= dayDate && $0.timestamp < dayEnd }
                .reduce(0) { $0 + $1.duration }
            result.append(DailyStat(date: dayDate, minutes: Double(seconds) / 60.0))
        }
        return result
    }

    func yearMonthlyData() -> [MonthlyStat] {
        let calendar = Calendar.current
        let year = calendar.component(.year, from: Date())
        var result: [MonthlyStat] = []
        for month in 1...12 {
            let components = DateComponents(year: year, month: month)
            guard let startOfMonth = calendar.date(from: components),
                  let endOfMonth = calendar.date(byAdding: .month, value: 1, to: startOfMonth) else { continue }
            let monthSessions = repository.sessions.filter {
                $0.timestamp >= startOfMonth && $0.timestamp < endOfMonth
            }
            let totalSeconds = monthSessions.reduce(0) { $0 + $1.duration }
            result.append(MonthlyStat(
                month: month,
                count: monthSessions.count,
                minutes: Double(totalSeconds) / 60.0
            ))
        }
        return result
    }

    func latestStatusMessage(daysAgo: Int) -> (status: String, motivation: String) {
        switch daysAgo {
        case 0:
            return ("今天", "今天已经努力过了！")
        case 1:
            return ("昨天", "昨天休息了一天，今天继续加油！")
        case 2...3:
            return ("\(daysAgo)天前", "已经\(daysAgo)天没有记录了，要注意节制哦！")
        case 4...7:
            return ("\(daysAgo)天前", "\(daysAgo)天了！你是不是在养精蓄锐？")
        default:
            return ("\(daysAgo)天前", "\(daysAgo)天了！不要忘记记录哦～")
        }
    }

    func formatDateTime(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return f.string(from: date)
    }

    func formatDuration(_ seconds: Int) -> String {
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        if h > 0 { return "\(h)小时\(m)分\(s)秒" }
        if m > 0 { return "\(m)分\(s)秒" }
        return "\(s)秒"
    }
}

struct StatRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label).foregroundStyle(.secondary)
            Spacer()
            Text(value).bold()
        }
        .font(.subheadline)
    }
}
