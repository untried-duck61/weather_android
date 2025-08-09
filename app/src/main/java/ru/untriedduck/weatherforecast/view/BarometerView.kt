package ru.untriedduck.weatherforecast.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import androidx.core.graphics.withScale

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
            paint.color = 0x66ffffff
            paint.style = Paint.Style.STROKE

            drawCircle(0f, 0f, 1f, paint)

        }
    }
}