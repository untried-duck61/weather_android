package ru.untriedduck.weatherforecast.view

import ru.untriedduck.weatherforecast.R
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import androidx.annotation.AttrRes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class SunDayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sunriseTime: Long = 0
    private var sunsetTime: Long = 0
    private var currentTime: Long = 0

    private var animatedAngle: Float = 210f
    private var animator: ValueAnimator? = null

    // Цвета Material 3
    private val colorPrimary = resolveThemeColor(android.R.attr.colorPrimary)
    private val colorOutlineVariant = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
    private val colorOnSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
    private val colorTertiary = resolveThemeColor(com.google.android.material.R.attr.colorTertiary)

    // Траектория 24 часа (Ночная/сумеречная основа — тусклый пунктир)
    private val nightArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorOutlineVariant
        style = Paint.Style.STROKE
        strokeWidth = 5f
        alpha = 100 // Делаем ночную зону полупрозрачной
        pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
    }

    // Дневная арка (Светлая сплошная линия между восходом и закатом)
    private val dayArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorOnSurface
        style = Paint.Style.STROKE
        strokeWidth = 6f
        alpha = 180
    }

    // Закрашенный пройденный путь солнца
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorPrimary
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
    }

    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTertiary
        style = Paint.Style.FILL
    }

    // Красивое градиентное размытие под дневной аркой
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorOnSurface
        textSize = 34f
    }

    private val arcRect = RectF()
    private val gradientPath = Path()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Углы сумерек (динамически рассчитываются)
    private var sunriseAngle = 210f // Примерное дефолтное положение (чуть выше левого края)
    private var sunsetAngle = 330f  // Примерное дефолтное положение (чуть выше правого края)

    fun setData(sunrise: Long, sunset: Long, current: Long) {
        this.sunriseTime = sunrise
        this.sunsetTime = sunset
        this.currentTime = current

        // Находим пропорцию дня внутри 24 часов
        val dayDuration = sunsetTime - sunriseTime
        val totalDay = 24 * 60 * 60
        val dayScale = (dayDuration.toFloat() / totalDay) * 180f

        // Восход и закат теперь «сдвинуты» внутрь дуги, образуя сумеречные хвосты по краям
        sunriseAngle = 180f + (180f - dayScale) / 2f
        sunsetAngle = 360f - (180f - dayScale) / 2f

        startArcAnimation()
    }

    private fun startArcAnimation() {
        animator?.cancel()
        val targetAngle = calculateAngleForTime()

        animator = ValueAnimator.ofFloat(180f, targetAngle).apply {
            interpolator = AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)
            duration = 1100
            addUpdateListener { animation ->
                animatedAngle = animation.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val paddingBottom = 90
        val idealHeight = (width / 2) + paddingBottom
        setMeasuredDimension(width, idealHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paddingX = 80f
        val textPaddingBottom = 60f
        val width = width.toFloat()
        val height = height.toFloat()

        val centerX = width / 2f
        val radius = (width - paddingX * 2) / 2f
        val centerY = height - textPaddingBottom - 20f

        arcRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        // 1. Рисуем всю 24-часовую базовую арку (Ночь и сумерки)
        canvas.drawArc(arcRect, 180f, 180f, false, nightArcPaint)

        // 2. Рисуем выделенную ДНЕВНУЮ часть (Светлая область между восходом и закатом)
        val daySweep = sunsetAngle - sunriseAngle
        canvas.drawArc(arcRect, sunriseAngle, daySweep, false, dayArcPaint)

        // 3. Создаем и рисуем мягкий градиент под пройденным путем солнца
        if (animatedAngle > sunriseAngle) {
            gradientPath.reset()
            // Ограничиваем градиент точкой заката, если солнце уже ушло в ночь
            val endProgressAngle = animatedAngle.coerceAtMost(sunsetAngle)

            gradientPath.arcTo(arcRect, sunriseAngle, endProgressAngle - sunriseAngle)
            gradientPath.lineTo(centerX + radius * cos(Math.toRadians(endProgressAngle.toDouble())).toFloat(), centerY)
            gradientPath.lineTo(centerX + radius * cos(Math.toRadians(sunriseAngle.toDouble())).toFloat(), centerY)
            gradientPath.close()

            // Плавное угасание цвета сверху вниз
            gradientPaint.shader = LinearGradient(
                centerX, centerY - radius, centerX, centerY,
                colorPrimary and 0x40FFFFFF, // 25% прозрачности вашего Primary цвета
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(gradientPath, gradientPaint)
        }

        // 4. Рисуем закрашенный прогресс пройденного пути солнца
        val progressSweep = (animatedAngle - 180f).coerceIn(0f, 180f)
        if (progressSweep > 0f) {
            canvas.drawArc(arcRect, 180f, progressSweep, false, progressPaint)
        }

        // 5. Вычисляем координаты солнца и рисуем его
        val sunAngleRad = Math.toRadians(animatedAngle.toDouble())
        val sunX = centerX + radius * cos(sunAngleRad).toFloat()
        val sunY = centerY + radius * sin(sunAngleRad).toFloat()
        canvas.drawCircle(sunX, sunY, 22f, sunPaint)

        // 6. Текст текущего времени над/под солнцем
        textPaint.textAlign = Paint.Align.CENTER
        val offsetSign = if (animatedAngle in 180f..360f) -40f else 50f
        canvas.drawText(formatTime(currentTime), sunX, sunY + offsetSign, textPaint)

        // 7. Подписи Восхода и Заката строго под точками их пересечения на арке
        if (sunriseTime > 0 && sunsetTime > 0) {
            val sunriseStr = context.getString(R.string.sun_chart_sunrise, formatTime(sunriseTime))
            val sunsetStr = context.getString(R.string.sun_chart_sunset, formatTime(sunsetTime))

            // Координаты точек восхода и заката для выравнивания текста
            val sunriseX = centerX + radius * cos(Math.toRadians(sunriseAngle.toDouble())).toFloat()
            val sunsetX = centerX + radius * cos(Math.toRadians(sunsetAngle.toDouble())).toFloat()

            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(sunriseStr, sunriseX, centerY + 55f, textPaint)
            canvas.drawText(sunsetStr, sunsetX, centerY + 55f, textPaint)
        }
    }

    /**
     * Полноценная 24-часовая математика с учетом кастомных смещений сумерек
     */
    private fun calculateAngleForTime(): Float {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentTime * 1000 }
        val secondsSinceMidnight = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND)
        val totalSeconds = 24 * 3600
        val globalProgress = secondsSinceMidnight.toFloat() / totalSeconds

        // Карта 24 часов на полукруг 180° -> 360°
        return 180f + (globalProgress * 180f)
    }

    private fun formatTime(unixTime: Long): String {
        if (unixTime == 0L) return "--:--"
        return timeFormatter.format(Date(unixTime * 1000))
    }

    private fun resolveThemeColor(@AttrRes attrRes: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attrRes, typedValue, true)
        return typedValue.data
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }
}