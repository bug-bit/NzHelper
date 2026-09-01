import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct HistoryView: View {
    @EnvironmentObject var repository: SessionRepository

    @State private var detailSession: Session?
    @State private var editSession: Session?
    @State private var sessionToDelete: Session?
    @State private var showDeleteConfirm = false
    @State private var showClearConfirm = false
    @State private var showImportPicker = false
    @State private var importResult: (success: Bool, message: String)?
    @State private var showImportAlert = false
    @State private var exportURL: URL?
    @State private var showShareSheet = false

    var body: some View {
        Group {
            if repository.sessions.isEmpty {
                ContentUnavailableView(
                    "暂无记录",
                    systemImage: "clock.arrow.circlepath",
                    description: Text("开始计时以创建第一条记录")
                )
            } else {
                List {
                    ForEach(repository.sessions.reversed()) { session in
                        SessionRowView(session: session)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                detailSession = session
                            }
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    sessionToDelete = session
                                    showDeleteConfirm = true
                                } label: {
                                    Label("删除", systemImage: "trash")
                                }

                                Button {
                                    editSession = session
                                } label: {
                                    Label("编辑", systemImage: "pencil")
                                }
                                .tint(.orange)
                            }
                    }
                }
            }
        }
        .navigationTitle("历史")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if !repository.sessions.isEmpty {
                    Menu {
                        Button {
                            showImportPicker = true
                        } label: {
                            Label("导入", systemImage: "square.and.arrow.down")
                        }
                        Button {
                            exportURL = repository.exportSessions()
                            showShareSheet = exportURL != nil
                        } label: {
                            Label("导出", systemImage: "square.and.arrow.up")
                        }
                        Divider()
                        Button(role: .destructive) {
                            showClearConfirm = true
                        } label: {
                            Label("清空全部", systemImage: "trash")
                        }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
        .sheet(item: $detailSession) { session in
            SessionDetailView(session: session)
        }
        .sheet(item: $editSession) { session in
            DetailsFormView(
                duration: session.duration,
                editingSession: session,
                onSave: { updated in
                    repository.updateSession(updated)
                },
                onCancel: {}
            )
        }
        .alert("确认删除", isPresented: $showDeleteConfirm) {
            Button("取消", role: .cancel) {}
            Button("删除", role: .destructive) {
                if let s = sessionToDelete {
                    repository.deleteSession(s)
                }
            }
        } message: {
            Text("此操作无法撤销")
        }
        .alert("确认清空", isPresented: $showClearConfirm) {
            Button("取消", role: .cancel) {}
            Button("清空", role: .destructive) {
                repository.deleteAllSessions()
            }
        } message: {
            Text("将删除所有历史记录，此操作无法撤销")
        }
        .fileImporter(isPresented: $showImportPicker, allowedContentTypes: [.json]) { result in
            switch result {
            case .success(let url):
                let ok = repository.importSessions(from: url)
                importResult = (ok, ok ? "导入成功" : "导入失败，文件格式不正确")
            case .failure:
                importResult = (false, "导入失败")
            }
            showImportAlert = true
        }
        .alert(importResult?.message ?? "", isPresented: $showImportAlert) {
            Button("确定", role: .cancel) {}
        }
        .sheet(isPresented: $showShareSheet) {
            if let url = exportURL {
                ShareSheet(items: [url])
            }
        }
    }
}

struct SessionRowView: View {
    let session: Session

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(formatDate(session.timestamp))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(formatDuration(session.duration))
                    .font(.subheadline)
                    .bold()
                    .monospacedDigit()
            }
            if !session.remark.isEmpty {
                Text(session.remark)
                    .font(.body)
                    .lineLimit(2)
            }
        }
        .padding(.vertical, 2)
    }

    func formatDate(_ date: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.string(from: date)
    }

    func formatDuration(_ seconds: Int) -> String {
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        let s = seconds % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%02d:%02d", m, s)
    }
}

struct SessionDetailView: View {
    let session: Session

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Form {
                Section("基本信息") {
                    LabeledContent("时间", value: formatDateTime(session.timestamp))
                    LabeledContent("时长", value: formatDuration(session.duration))
                    LabeledContent("地点", value: session.location.isEmpty ? "未填写" : session.location)
                }
                Section {
                    LabeledContent("观看成人内容", value: session.watchedMovie ? "是" : "否")
                    LabeledContent("高潮", value: session.climax ? "是" : "否")
                }
                Section {
                    LabeledContent("道具", value: session.props)
                    LabeledContent("评分", value: String(format: "%.1f", session.rating))
                    LabeledContent("心情", value: session.mood)
                }
                if !session.remark.isEmpty {
                    Section("备注") {
                        Text(session.remark)
                    }
                }
            }
            .navigationTitle("记录详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("关闭") { dismiss() }
                }
            }
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
        if h > 0 {
            return String(format: "%d小时%d分%d秒", h, m, s)
        }
        if m > 0 {
            return String(format: "%d分%d秒", m, s)
        }
        return "\(s)秒"
    }
}

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
