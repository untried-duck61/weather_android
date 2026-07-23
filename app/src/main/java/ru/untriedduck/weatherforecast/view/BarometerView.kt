package ru.untriedduck.weatherforecast.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.google.android.material.color.MaterialColors
import ru.untriedduck.weatherforecast.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class BarometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Текущее значение давления и диапазон (в гПа / hPa или мм рт. ст.)
    var minPressure: Float = 950f
    var maxPressure: Float = 1050f
    var currentPressure: Float = 1013f
        set(value) {
            field = value.coerceIn(minPressure, maxPressure)
            invalidate()
        }

    // Настройки разметки шкалы (в градусах)
    private val startAngle = 135f // Старт снизу слева
    private val sweepAngle = 270f // Дуга на 3/4 круга

    // Краски
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND // Скругленные края в стиле M3
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val oval = RectF()

    init {
        // Извлекаем цвета из Material 3 темы устройства
        val colorPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
        val colorSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY)
        val colorOnSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)

        trackPaint.color = colorSurfaceVariant
        progressPaint.color = colorPrimary
        indicatorPaint.color = colorPrimary
        textPaint.color = colorOnSurface

        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BarometerView,
            0, 0
        )

        try {
            currentPressure = typedArray.getFloat(R.styleable.BarometerView_currentPressure, 0.0f)
        } finally {
            // ALWAYS recycle the TypedArray
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Вычисляем размеры и центр воpx пикселях (гарантирует четкость)
        val size = min(width, height).toFloat()
        val padding = size * 0.1f
        val strokeWidth = size * 0.06f // Пропорциональная толщина дуги
        val radius = (size - padding * 2) / 2
        val centerX = width / 2f
        val centerY = height / 2f

        // Настраиваем кисти под актуальный размер
        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        textPaint.textSize = size * 0.12f

        // Границы для дуги
        oval.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 1. Рисуем фоновую дугу (трек)
        canvas.drawArc(oval, startAngle, sweepAngle, false, trackPaint)

        // 2. Считаем процент заполнения шкалы
        val progressRatio = (currentPressure - minPressure) / (maxPressure - minPressure)
        val currentSweepAngle = sweepAngle * progressRatio

        // 3. Рисуем дугу прогресса
        canvas.drawArc(oval, startAngle, currentSweepAngle, false, progressPaint)

        // 4. Рисуем аккуратную точку-указатель на конце линии прогресса
        val targetAngleRad = Math.toRadians((startAngle + currentSweepAngle).toDouble())
        val indicatorX = centerX + radius * cos(targetAngleRad).toFloat()
        val indicatorY = centerY + radius * sin(targetAngleRad).toFloat()
        canvas.drawCircle(indicatorX, indicatorY, strokeWidth * 0.4f, indicatorPaint)

        // 5. Выводим текст по центру (значение давления)
        canvas.drawText(context.getString(R.string.barometer_label, currentPressure.toInt()), centerX, centerY + (textPaint.textSize / 3), textPaint)
    }
}