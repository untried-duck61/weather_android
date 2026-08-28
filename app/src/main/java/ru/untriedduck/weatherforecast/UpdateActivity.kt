package ru.untriedduck.weatherforecast

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.color.MaterialColors
import org.json.JSONObject
import ru.untriedduck.weatherforecast.databinding.ActivityUpdateBinding
import io.noties.markwon.Markwon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.untriedduck.weatherforecast.updates.ApkDownloader
import ru.untriedduck.weatherforecast.updates.ApkInstaller
import java.io.File

class UpdateActivity : AppCompatActivity() {

    enum class UpdateState {
        READY_TO_DOWNLOAD, // Готово к скачиванию
        DOWNLOADING,       // В процессе скачивания
        READY_TO_INSTALL   // Скачано, готово к установке
    }

    private var currentUpdateState = UpdateState.READY_TO_DOWNLOAD
    private var downloadedApkFile: File? = null

    private var downloadJob: Job? = null

    companion object {
        const val CHANNEL_ID = "updates_channel"
        const val NOTIFICATION_ID = 1001
    }
    private lateinit var binding : ActivityUpdateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }
        fetchUpdateInfo()
    }

    fun fetchUpdateInfo() {
        val latestUrl =
            "https://api.github.com/repos/untried-duck61/weather_android/releases/latest"
        val queue = Volley.newRequestQueue(this)

        binding.changelogProgress.visibility = View.VISIBLE

        binding.tvWhatsNewBlockTitle.visibility = View.GONE
        binding.tvChangelog.visibility = View.GONE

        val stringRequest = StringRequest(Request.Method.GET, latestUrl, { response ->
            Log.d("UpdateActivity", ">>> УСПЕХ! Ответ от сервера: $response")
            try {
                val root = JSONObject(response)
                val latestVersion = root.getString("tag_name")

                val currentVersion =
                    packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
                        ?: "0.0.0"

                binding.tvInstalledAppVersion.text = currentVersion
                binding.tvAvailableAppVersion.text = latestVersion

                val changelogMarkdown = root.optString("body", "Список изменений пуст.")

                binding.changelogProgress.visibility = View.GONE
                binding.tvWhatsNewBlockTitle.visibility = View.VISIBLE
                binding.tvChangelog.visibility = View.VISIBLE

                val markwon = Markwon.create(this@UpdateActivity)
                markwon.setMarkdown(binding.tvChangelog, changelogMarkdown)

                if (isUpdateNeeded(currentVersion, latestVersion)) {
                    val asset = root.getJSONArray("assets").getJSONObject(0)
                    val downloadUrl = asset.getString("browser_download_url")

                    val apkDownloader = ApkDownloader(this@UpdateActivity)
                    val apkInstaller = ApkInstaller(this@UpdateActivity)

                    binding.btnAction.text = getString(R.string.update_activity_btn_action_download_an_update)
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener {
                        when (currentUpdateState) {
                            UpdateState.READY_TO_DOWNLOAD -> {
                                keepScreenOn(true)

                                setUpdateState(UpdateState.DOWNLOADING)

                                downloadJob = lifecycleScope.launch {
                                    try {
                                        val apkFile = apkDownloader.downloadApk(downloadUrl) { downloaded, total ->
                                            val progressPercent = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                                            val downloadedMb = downloaded.toDouble() / (1024 * 1024)
                                            val totalMb = total.toDouble() / (1024 * 1024)

                                            runOnUiThread {
                                                binding.downloadProgressBar.progress = progressPercent
                                                binding.tvDownloadStatus.text = getString(
                                                    R.string.download_progress_placeholder,
                                                    progressPercent,
                                                    downloadedMb,
                                                    totalMb
                                                )
                                            }
                                        }

                                        if (apkFile != null && apkFile.exists()) {
                                            downloadedApkFile = apkFile
                                            setUpdateState(UpdateState.READY_TO_INSTALL)

                                            if (!apkInstaller.checkInstallPermission()) {
                                                apkInstaller.openInstallSettings()
                                            } else {
                                                apkInstaller.installApk(apkFile)
                                            }
                                        } else {
                                            setUpdateState(UpdateState.READY_TO_DOWNLOAD)
                                            showToast(getString(R.string.update_download_failed))
                                        }
                                    } catch (e: CancellationException) {

                                    } catch (e: Exception) {
                                        setUpdateState(UpdateState.READY_TO_DOWNLOAD)
                                        showToast(getString(R.string.update_download_failed))
                                    } finally {
                                        keepScreenOn(false)
                                    }
                                }
                            }

                            UpdateState.DOWNLOADING -> {
                                // ИСПРАВЛЕНО: Логика отмены скачивания в один клик!
                                downloadJob?.cancel() // Отменяем корутину скачивания
                                setUpdateState(UpdateState.READY_TO_DOWNLOAD) // Возвращаем кнопку в исходный вид
                                showToast(getString(R.string.update_cancelled_info))
                            }

                            UpdateState.READY_TO_INSTALL -> {
                                // Твоя готовая логика повторного запуска установки файла
                                downloadedApkFile?.let { apkFile ->
                                    if (apkFile.exists()) {
                                        if (!apkInstaller.checkInstallPermission()) {
                                            apkInstaller.openInstallSettings()
                                        } else {
                                            apkInstaller.installApk(apkFile)
                                        }
                                    } else {
                                        setUpdateState(UpdateState.READY_TO_DOWNLOAD)
                                        showToast(getString(R.string.update_file_not_found_error))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    animateViewToGone(binding.cardVersionsLeftColumn, binding.cardVersionsDivider, binding.cardVersionsRightColumn)
                    binding.btnAction.text =
                        getString(R.string.update_activity_btn_action_up_to_date)
                    binding.btnAction.isEnabled = false
                }

            } catch (e: Exception) {
                binding.changelogProgress.visibility = View.GONE
            }
        }, { error ->
            binding.changelogProgress.visibility = View.GONE
            showToast(getString(R.string.update_error))
        })

        queue.add(stringRequest)
    }

    private fun setUpdateState(state: UpdateState) {
        currentUpdateState = state

        // Вытаскиваем цвета темы M3 динамически
        val primaryColor = MaterialColors.getColor(binding.btnAction, android.R.attr.colorPrimary)
        val errorColor = MaterialColors.getColor(binding.btnAction, android.R.attr.colorError)

        when (state) {
            UpdateState.READY_TO_DOWNLOAD -> {
                binding.downloadProgressBar.visibility = View.GONE
                binding.tvDownloadStatus.visibility = View.GONE

                binding.btnAction.text = getString(R.string.update_activity_btn_action_download_an_update)
                binding.btnAction.setIconResource(R.drawable.ic_sync)
                binding.btnAction.backgroundTintList = ColorStateList.valueOf(primaryColor)
                binding.btnAction.isEnabled = true
            }

            UpdateState.DOWNLOADING -> {
                binding.downloadProgressBar.visibility = View.VISIBLE
                binding.tvDownloadStatus.visibility = View.VISIBLE

                // В Expressive-стиле превращаем кнопку в красную кнопку отмены
                binding.btnAction.text =
                    getString(R.string.update_activity_btn_action_cancel_download)
                binding.btnAction.setIconResource(R.drawable.ic_cancel)
                binding.btnAction.backgroundTintList = ColorStateList.valueOf(errorColor)
                binding.btnAction.isEnabled = true
            }

            UpdateState.READY_TO_INSTALL -> {
                binding.downloadProgressBar.visibility = View.GONE
                binding.tvDownloadStatus.visibility = View.GONE

                binding.btnAction.text =
                    getString(R.string.update_activity_btn_action_install_an_update)
                binding.btnAction.setIconResource(R.drawable.ic_install_mobile)
                binding.btnAction.backgroundTintList = ColorStateList.valueOf(primaryColor)
                binding.btnAction.isEnabled = true
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isUpdateNeeded(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val maxParts = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxParts) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (c > l) return false
        }
        return false
    }

    private fun keepScreenOn(enabled: Boolean) {
        if (enabled) {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun animateViewToGone(leftView: View, divider: View, rightView: View) {
        val initialHeight = rightView.height

        (rightView.layoutParams as LinearLayout.LayoutParams).height = initialHeight
        (divider.layoutParams as LinearLayout.LayoutParams).height = initialHeight

        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 350
            interpolator = FastOutSlowInInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                (rightView.layoutParams as LinearLayout.LayoutParams).weight = progress
                (divider.layoutParams as LinearLayout.LayoutParams).weight = progress
                (leftView.layoutParams as LinearLayout.LayoutParams).weight = 1f + (1f - progress) * 2f

                rightView.requestLayout()
                divider.requestLayout()
                leftView.requestLayout()
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {

                    rightView.visibility = View.GONE
                    divider.visibility = View.GONE

                    (rightView.layoutParams as LinearLayout.LayoutParams).height = LinearLayout.LayoutParams.MATCH_PARENT
                    (divider.layoutParams as LinearLayout.LayoutParams).height = LinearLayout.LayoutParams.MATCH_PARENT
                }
            })
        }

        animator.start()
    }
}
