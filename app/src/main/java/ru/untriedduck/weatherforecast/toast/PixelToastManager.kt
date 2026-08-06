package ru.untriedduck.weatherforecast.toast

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import java.util.LinkedList
import java.util.Queue

object PixelToastManager {

    private val toastQueue: Queue<ToastData> = LinkedList()
    private var isShowing = false

    private data class ToastData(
        val anchorView: View,
        val message: String,
        val duration: Int
    )

    fun show(anchorView: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        toastQueue.add(ToastData(anchorView, message, duration))
        if (!isShowing) {
            showNext()
        }
    }

    private fun showNext() {
        val nextToast = toastQueue.poll()
        if (nextToast == null) {
            isShowing = false
            return
        }

        isShowing = true
        val context = nextToast.anchorView.context

        // 1. Создаем Snackbar
        val snackbar = Snackbar.make(nextToast.anchorView, nextToast.message, nextToast.duration)
        val snackbarView = snackbar.view

        // 2. Отключаем стандартную анимацию появления Snackbar (выезд снизу)
        snackbar.animationMode = Snackbar.ANIMATION_MODE_NONE

        // 3. Автоматически определяем тему устройства (Светлая / Темная)
        val isDarkMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        // Цвета строго по гайдлайнам Material You для тостов
        val backgroundColor = if (isDarkMode) Color.parseColor("#232323") else Color.parseColor("#F5F5F5")
        val textColor = if (isDarkMode) Color.WHITE else Color.parseColor("#1C1B1F")

        // 4. Применяем идеальную форму "пилюли"
        val shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes(ShapeAppearanceModel.PILL)
            .build()
        
        snackbarView.background = MaterialShapeDrawable(shapeAppearanceModel).apply {
            setTint(backgroundColor)
            if (!isDarkMode) {
                shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
                initializeElevationOverlay(context)
                elevation = 4f
                setStroke(1f, Color.parseColor("#E0E0E0")) // Легкий контур для светлой темы
            }
        }

        // 5. Настраиваем текст
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(textColor)
        textView.textSize = 14f

        // 6. Автоматически получаем иконку САМОГО приложения
        try {
            val appIcon = context.packageManager.getApplicationIcon(context.packageName)
            val metrics = context.resources.displayMetrics
            val size = (20 * (metrics.densityDpi / 160f)).toInt() // Размер 20dp
            appIcon.setBounds(0, 0, size, size)
            
            // На чистом Android иконка приложения в тосте обычно сохраняет свои цвета, 
            // но если хотите сделать её монохромной под цвет текста, раскомментируйте строку ниже:
            // appIcon.setTint(textColor)

            textView.setCompoundDrawables(appIcon, null, null, null)
            textView.compoundDrawablePadding = (10 * (metrics.densityDpi / 160f)).toInt() // Отступ 10dp
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 7. Геометрия окна (размер по контенту, центрирование и отступ снизу)
        val layoutParams = snackbarView.layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            layoutParams.setMargins(140, 0, 140, 220) 
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT 
            
            if (layoutParams is FrameLayout.LayoutParams) {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            } else if (layoutParams is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            snackbarView.layoutParams = layoutParams
        }

        // 8. Кастомная анимация «как на Pixel» (Плавное проявление + легкое масштабирование)
        snackbarView.alpha = 0f
        snackbarView.scaleX = 0.9f
        snackbarView.scaleY = 0.9f

        // Слушатель закрытия для очереди
        snackbar.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                snackbarView.postDelayed({ showNext() }, 100)
            }
        })

        snackbar.show()

        // Запуск анимации появления после вызова show()
        val fadeIn = ObjectAnimator.ofFloat(snackbarView, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(snackbarView, View.SCALE_X, 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(snackbarView, View.SCALE_Y, 0.9f, 1f)

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 250 // Длительность анимации в мс
            start()
        }
    }
}
