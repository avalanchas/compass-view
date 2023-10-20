package com.elliecoding.compass

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.widget.RelativeLayout
import kotlin.math.cos
import kotlin.math.sin

private const val DEGREES_COLOR = Color.BLACK
private const val SHOW_ORIENTATION_LABEL = false
private const val DEFAULT_DEGREES_STEP = 15
private const val DEFAULT_BORDER_COLOR = Color.BLACK
private const val DEFAULT_MINIMIZED_ALPHA = 180
private const val DEFAULT_ORIENTATION_LABELS_COLOR = Color.BLACK
private const val DEFAULT_SHOW_BORDER = false

const val NO_STEPS = -1

internal class CompassSkeleton : RelativeLayout {
    private var labelEast: String? = null
    private var labelNorth: String? = null
    private var labelWest: String? = null
    private var labelSouth: String? = null
    private var width = 0
    private var centerX = 0
    private var centerY = 0
    private var degreesColor = DEGREES_COLOR
    private var showOrientationLabel = SHOW_ORIENTATION_LABEL
    private var degreesStep = DEFAULT_DEGREES_STEP
    private var orientationLabelsColor = DEFAULT_ORIENTATION_LABELS_COLOR
    private var showBorder = DEFAULT_SHOW_BORDER
    private var borderColor = DEFAULT_BORDER_COLOR

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        labelEast = context.getString(R.string.label_east)
        labelNorth = context.getString(R.string.label_north)
        labelWest = context.getString(R.string.label_west)
        labelSouth = context.getString(R.string.label_south)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (widthMeasureSpec < heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        } else {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        width = if (height > width) width else height
        centerX = width / 2
        centerY = width / 2
        if (degreesStep != NO_STEPS) {
            drawCompassSkeleton(canvas)
        }
        drawOuterCircle(canvas)
    }

    private fun drawOuterCircle(canvas: Canvas) {
        val mStrokeWidth = (width * 0.01f).toInt()
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mStrokeWidth.toFloat()
        paint.color = borderColor
        val radius = (width / 2 - mStrokeWidth / 2).toFloat()
        val rectF = RectF()
        rectF[centerX - radius, centerY - radius, centerX + radius] = centerY + radius
        if (showBorder) canvas.drawArc(rectF, 0f, 360f, false, paint)
    }

    private fun drawCompassSkeleton(canvas: Canvas) {
        val paint = Paint()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val textPaint = TextPaint()
        textPaint.textSize = width * 0.06f
        textPaint.color = orientationLabelsColor
        val rect = Rect()
        val rPadded = centerX - (width * 0.01f).toInt()
        var degree = 0
        while (degree <= 360) {
            var rEnd: Int
            var rText: Int
            if (degree % 90 == 0) {
                rEnd = centerX - (width * 0.08f).toInt()
                rText = centerX - (width * 0.15f).toInt()
                paint.color = degreesColor
                paint.strokeWidth = width * 0.02f
                showOrientationLabel(canvas, textPaint, rect, degree, rText)
            } else if (degree % 45 == 0) {
                rEnd = centerX - (width * 0.06f).toInt()
                paint.color = degreesColor
                paint.strokeWidth = width * 0.02f
            } else {
                rEnd = centerX - (width * 0.04f).toInt()
                paint.color = degreesColor
                paint.strokeWidth = width * 0.015f
                paint.alpha = DEFAULT_MINIMIZED_ALPHA
            }
            val startX = (centerX + rPadded * cos(Math.toRadians(degree.toDouble()))).toInt()
            val startY = (centerX - rPadded * sin(Math.toRadians(degree.toDouble()))).toInt()
            val stopX = (centerX + rEnd * cos(Math.toRadians(degree.toDouble()))).toInt()
            val stopY = (centerX - rEnd * sin(Math.toRadians(degree.toDouble()))).toInt()

            canvas.drawLine(
                startX.toFloat(),
                startY.toFloat(),
                stopX.toFloat(),
                stopY.toFloat(),
                paint
            )
            degree += degreesStep
        }
    }

    private fun showOrientationLabel(
        canvas: Canvas,
        textPaint: TextPaint,
        rect: Rect,
        i: Int,
        rText: Int
    ) {
        if (showOrientationLabel) {
            val textX = (centerX + rText * cos(Math.toRadians(i.toDouble()))).toInt()
            val textY = (centerX - rText * sin(Math.toRadians(i.toDouble()))).toInt()
            var direction = labelEast
            when (i) {
                90 -> {
                    direction = labelNorth
                }

                180 -> {
                    direction = labelWest
                }

                270 -> {
                    direction = labelSouth
                }
            }
            textPaint.getTextBounds(direction, 0, 1, rect)
            canvas.drawText(
                direction!!,
                (textX - rect.width() / 2).toFloat(),
                (textY + rect.height() / 2).toFloat(),
                textPaint
            )
        }
    }

    fun setDegreesColor(degreesColor: Int) {
        this.degreesColor = degreesColor
        invalidate()
    }

    fun setShowOrientationLabel(showOrientationLabel: Boolean) {
        this.showOrientationLabel = showOrientationLabel
        invalidate()
    }

    fun setDegreesStep(degreesStep: Int) {
        this.degreesStep = degreesStep
        invalidate()
    }

    fun setOrientationLabelsColor(orientationLabelsColor: Int) {
        this.orientationLabelsColor = orientationLabelsColor
        invalidate()
    }

    fun setShowBorder(showBorder: Boolean) {
        this.showBorder = showBorder
        invalidate()
    }

    fun setBorderColor(borderColor: Int) {
        this.borderColor = borderColor
        invalidate()
    }
}
