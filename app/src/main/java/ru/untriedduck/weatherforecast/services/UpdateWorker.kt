package ru.untriedduck.weatherforecast.services

import android.content.Context
import android.content.Intent
import androidx.work.Worker
import androidx.work.WorkerParameters

class UpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Запускаем наш сервис проверки в тихом режиме
        val intent = Intent(context, UpdateCheckService::class.java).apply {
            putExtra("IS_MANUAL_CHECK", false)
        }
        context.startService(intent)

        return Result.success()
    }
}