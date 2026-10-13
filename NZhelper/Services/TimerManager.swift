import Foundation
import Combine

class TimerManager: ObservableObject {
    @Published var elapsedTime: TimeInterval = 0
    @Published var isRunning = false
    @Published var isPaused = false

    private var startDate: Date?
    private var accumulatedTime: TimeInterval = 0
    private var timer: Timer?

    var totalElapsedSeconds: Int {
        if let start = startDate {
            return Int(accumulatedTime + Date().timeIntervalSince(start))
        }
        return Int(accumulatedTime)
    }

    func start() {
        if isPaused {
            startDate = Date()
            isPaused = false
        } else {
            accumulatedTime = 0
            startDate = Date()
        }
        isRunning = true
        startTick()
    }

    func pause() {
        guard let start = startDate else { return }
        accumulatedTime += Date().timeIntervalSince(start)
        startDate = nil
        isPaused = true
        stopTick()
    }

    func stop() {
        if let start = startDate {
            accumulatedTime += Date().timeIntervalSince(start)
        }
        startDate = nil
        isRunning = false
        isPaused = false
        stopTick()
        elapsedTime = accumulatedTime
    }

    func reset() {
        stopTick()
        accumulatedTime = 0
        elapsedTime = 0
        isRunning = false
        isPaused = false
        startDate = nil
    }

    func handleEnterBackground() {
        guard isRunning, !isPaused, let start = startDate else { return }
        accumulatedTime += Date().timeIntervalSince(start)
        startDate = nil
        stopTick()
    }

    func handleEnterForeground() {
        guard isRunning, !isPaused else { return }
        startDate = Date()
        startTick()
    }

    private func startTick() {
        stopTick()
        timer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] _ in
            guard let self, let start = self.startDate else { return }
            self.elapsedTime = self.accumulatedTime + Date().timeIntervalSince(start)
        }
    }

    private func stopTick() {
        timer?.invalidate()
        timer = nil
    }
}
