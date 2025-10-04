package ru.untriedduck.weatherforecast.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withScale
import ru.untriedduck.weatherforecast.R
import kotlin.math.cos
import kotlin.math.sin

class BarometerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.withScale(.5f * width, -1f * height) {

            translate(1f, -1f)
            paint.color = 0x40ffffff //context.getString(R.string.barometer_bg).toColorInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.05f

            drawCircle(0f, 0f, 0.9f, paint)

            //paint.color = 0x20000000

            //drawCircle(0f, 0f, 0.8f, paint)

            paint.color = context.getString(R.string.barometer_color_3).toColorInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.001f

            val maxValue = 1083
            val value = 0

            val scale = 0.9f

            val step = Math.PI / maxValue
            for (i in 0..maxValue){
                var x1 = cos(Math.PI - step * i).toFloat()
                var y1 = sin(Math.PI - step * i).toFloat()

                var x2 = x1 * scale
                var y2 = y1 * scale

                drawLine(x1, y1, x2, y2, paint)
            }

        }
    }
}