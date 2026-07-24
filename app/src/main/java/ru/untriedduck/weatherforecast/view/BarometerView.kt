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

    var minPressure: Float = 950f
    var maxPressure: Float = 1050f

    // 1. ИСПРАВЛЕНО: Теперь дефолтное значение (1013) используется, если в XML ничего не задано
    var currentPressure: Float = 1013f
        set(value) {
            field = value.coerceIn(minPressure, maxPressure)
            invalidate()
        }

    private val startAngle = 135f
    private val sweepAngle = 270f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
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
        val colorPrimary = MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, Color.BLUE)
        val colorSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, Color.GRAY)
        val colorOnSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)

        trackPaint.color = colorSurfaceVariant
        progressPaint.color = colorPrimary
        indicatorPaint.color = colorPrimary
        textPaint.color = colorOnSurface

        val typedArray: TypedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.BarometerView, 0, 0
        )
        try {
            // 2. ИСПРАВЛЕНО: Передаем currentPressure в качестве дефолтного значения для getFloat
            currentPressure = typedArray.getFloat(R.styleable.BarometerView_currentPressure, currentPressure)
        } finally {
            typedArray.recycle()
        }
    }

    // 3. ОПТИМИЗАЦИЯ: Расчет размеров перенесен из onDraw сюда.
    // onDraw вызывается очень часто, там нельзя делать лишние вычисления и выделять память.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val size = min(w, h).toFloat()
        val padding = size * 0.1f
        val strokeWidth = size * 0.06f
        val radius = (size - padding * 2) / 2
        val centerX = w / 2f
        val centerY = h / 2f

        trackPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth
        textPaint.textSize = size * 0.12f

        oval.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = oval.width() / 2f
        val strokeWidth = trackPaint.strokeWidth

        // 1. ФLayout-трек
        canvas.drawArc(oval, startAngle, sweepAngle, false, trackPaint)

        // 2. Прогресс
        val progressRatio = (currentPressure - minPressure) / (maxPressure - minPressure)
        val currentSweepAngle = sweepAngle * progressRatio
        canvas.drawArc(oval, startAngle, currentSweepAngle, false, progressPaint)

        // 3. Точка-указатель
        val targetAngleRad = Math.toRadians((startAngle + currentSweepAngle).toDouble())
        val indicatorX = centerX + radius * cos(targetAngleRad).toFloat()
        val indicatorY = centerY + radius * sin(targetAngleRad).toFloat()
        canvas.drawCircle(indicatorX, indicatorY, strokeWidth * 0.4f, indicatorPaint)

        // 4. Текст
        // ИСПРАВЛЕНО: Для точного центрирования текста по вертикали лучше использовать fontMetrics
        val textY = centerY - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(context.getString(R.string.barometer_label, currentPressure.toInt()), centerX, textY, textPaint)
    }
}