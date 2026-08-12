package ru.untriedduck.weatherforecast.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_START_DOWNLOAD") {
            val url = intent.getStringExtra("DOWNLOAD_URL") ?: ""

            val serviceIntent = Intent(context, DownloadService::class.java).apply {
                putExtra("DOWNLOAD_URL", url)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}