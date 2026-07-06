package me.neko.nzhelper.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.neko.nzhelper.core.database.RecycleRepository

class RecycleBinWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            RecycleRepository.cleanExpiredRecycleBinItems(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "recycle_bin_cleanup"

        fun schedulePeriodicCleanup(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecycleBinWorker>(
                24,
                java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder().setRequiresDeviceIdle(true).build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}