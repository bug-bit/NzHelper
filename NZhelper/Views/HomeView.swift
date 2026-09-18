import SwiftUI

struct HomeView: View {
    @EnvironmentObject var timerManager: TimerManager
    @EnvironmentObject var repository: SessionRepository
    @State private var showEndConfirm = false
    @State private var showDetails = false

    var body: some View {
        VStack(spacing: 44) {
            Spacer()

            Text(formatElapsed(timerManager.elapsedTime))
                .font(.system(size: 56, weight: .light, design: .monospaced))
                .monospacedDigit()
                .contentTransition(.numericText())

            HStack(spacing: 20) {
                if !timerManager.isRunning {
                    Button {
                        timerManager.start()
                    } label: {
                        Label("开始", systemImage: "play.fill")
                            .frame(minWidth: 72)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                } else {
                    if !timerManager.isPaused {
                        Button {
                            timerManager.pause()
                        } label: {
                            Label("暂停", systemImage: "pause.fill")
                                .frame(minWidth: 72)
                        }
                        .buttonStyle(.bordered)
                        .controlSize(.large)
                    } else {
                        Button {
                            timerManager.start()
                        } label: {
                            Label("继续", systemImage: "play.fill")
                                .frame(minWidth: 72)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                    }

                    Button {
                        if timerManager.totalElapsedSeconds > 0 {
                            showEndConfirm = true
                        } else {
                            timerManager.reset()
                        }
                    } label: {
                        Label("结束", systemImage: "stop.fill")
                            .frame(minWidth: 72)
                    }
                    .buttonStyle(.bordered)
                    .tint(.red)
                    .controlSize(.large)
                }
            }

            Spacer()
        }
        .navigationTitle("计时")
        .alert("结束了吗？", isPresented: $showEndConfirm) {
            Button("再坚持一下", role: .cancel) {}
            Button("燃尽了") {
                timerManager.stop()
                showDetails = true
            }
        }
        .sheet(isPresented: $showDetails) {
            DetailsFormView(
                duration: timerManager.totalElapsedSeconds,
                onSave: { session in
                    repository.addSession(session)
                    timerManager.reset()
                },
                onCancel: {
                    timerManager.reset()
                }
            )
        }
    }

    func formatElapsed(_ interval: TimeInterval) -> String {
        let total = Int(interval)
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 {
            return String(format: "%02d:%02d:%02d", h, m, s)
        }
        return String(format: "%02d:%02d", m, s)
    }
}
