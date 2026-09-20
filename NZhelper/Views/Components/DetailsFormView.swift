import SwiftUI

struct DetailsFormView: View {
    let duration: Int
    var editingSession: Session? = nil
    let onSave: (Session) -> Void
    let onCancel: () -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var location = ""
    @State private var watchedMovie = false
    @State private var climax = true
    @State private var selectedProp = "手"
    @State private var rating: Float = 3.0
    @State private var selectedMood = "平静"
    @State private var remark = ""

    private let props = ["手", "斐济杯", "小胶妻"]
    private let moods = ["平静", "愉悦", "兴奋", "疲惫", "这是最后一次！"]

    init(duration: Int, editingSession: Session? = nil, onSave: @escaping (Session) -> Void, onCancel: @escaping () -> Void) {
        self.duration = duration
        self.editingSession = editingSession
        self.onSave = onSave
        self.onCancel = onCancel
        if let s = editingSession {
            _location = State(initialValue: s.location)
            _watchedMovie = State(initialValue: s.watchedMovie)
            _climax = State(initialValue: s.climax)
            _selectedProp = State(initialValue: s.props)
            _rating = State(initialValue: s.rating)
            _selectedMood = State(initialValue: s.mood)
            _remark = State(initialValue: s.remark)
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    HStack {
                        Text("时长")
                        Spacer()
                        Text(formatDuration(duration))
                            .foregroundStyle(.secondary)
                    }
                    TextField("地点", text: $location)
                }

                Section {
                    Toggle("观看成人内容", isOn: $watchedMovie)
                    Toggle("高潮", isOn: $climax)
                }

                Section("道具") {
                    Picker("道具", selection: $selectedProp) {
                        ForEach(props, id: \.self) { p in
                            Text(p).tag(p)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("评分") {
                    VStack(spacing: 8) {
                        Slider(value: Binding<Double>(
                            get: { Double(rating) },
                            set: { rating = Float(($0 * 2).rounded() / 2) }
                        ), in: 0...5, step: 0.5)
                        HStack {
                            Text("0.0").font(.caption).foregroundStyle(.secondary)
                            Spacer()
                            Text(String(format: "%.1f", rating)).font(.title3).bold()
                            Spacer()
                            Text("5.0").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }

                Section("心情") {
                    Picker("心情", selection: $selectedMood) {
                        ForEach(moods, id: \.self) { m in
                            Text(m).tag(m)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("备注") {
                    TextField("备注", text: $remark, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(editingSession != nil ? "编辑记录" : "记录详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onCancel()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") {
                        let session = Session(
                            id: editingSession?.id ?? UUID(),
                            timestamp: editingSession?.timestamp ?? Date(),
                            duration: duration,
                            remark: remark,
                            location: location,
                            watchedMovie: watchedMovie,
                            climax: climax,
                            rating: rating,
                            mood: selectedMood,
                            props: selectedProp
                        )
                        onSave(session)
                        dismiss()
                    }
                }
            }
        }
    }

    func formatDuration(_ seconds: Int) -> String {
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        var parts: [String] = []
        if h > 0 { parts.append("\(h)小时") }
        if m > 0 { parts.append("\(m)分") }
        parts.append("\(s)秒")
        return parts.joined()
    }
}
