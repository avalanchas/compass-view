package com.elliecoding.compass

import android.content.Context
import android.graphics.*
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

internal class CompassSkeleton : RelativeLayout {
    private var labelEast: String? = null
    private var labelNorth: String? = null
    private var labelWest: String? = null
    private var labelSouth: String? = null
    private var mWidth = 0
    private var mCenterX = 0
    private var mCenterY = 0
    private var mDegreesColor = DEGREES_COLOR
    private var mShowOrientationLabel = SHOW_ORIENTATION_LABEL
    private var mDegreesStep = DEFAULT_DEGREES_STEP
    private var mOrientationLabelsColor = DEFAULT_ORIENTATION_LABELS_COLOR
    private var mShowBorder = DEFAULT_SHOW_BORDER
    private var mBorderColor = DEFAULT_BORDER_COLOR

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
        mWidth = if (height > width) width else height
        mCenterX = mWidth / 2
        mCenterY = mWidth / 2
        drawCompassSkeleton(canvas)
        drawOuterCircle(canvas)
    }

    private fun drawOuterCircle(canvas: Canvas) {
        val mStrokeWidth = (mWidth * 0.01f).toInt()
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mStrokeWidth.toFloat()
        paint.color = mBorderColor
        val radius = (mWidth / 2 - mStrokeWidth / 2).toFloat()
        val rectF = RectF()
        rectF[mCenterX - radius, mCenterY - radius, mCenterX + radius] = mCenterY + radius
        if (mShowBorder) canvas.drawArc(rectF, 0f, 360f, false, paint)
    }

    private fun drawCompassSkeleton(canvas: Canvas) {
        val paint = Paint()
        paint.style = Paint.Style.FILL_AND_STROKE
        paint.strokeCap = Paint.Cap.ROUND
        val textPaint = TextPaint()
        textPaint.textSize = mWidth * 0.06f
        textPaint.color = mOrientationLabelsColor
        val rect = Rect()
        val rPadded = mCenterX - (mWidth * 0.01f).toInt()
        var degree = 0
        while (degree <= 360) {
            var rEnd: Int
            var rText: Int
            if (degree % 90 == 0) {
                rEnd = mCenterX - (mWidth * 0.08f).toInt()
                rText = mCenterX - (mWidth * 0.15f).toInt()
                paint.color = mDegreesColor
                paint.strokeWidth = mWidth * 0.02f
                showOrientationLabel(canvas, textPaint, rect, degree, rText)
            } else if (degree % 45 == 0) {
                rEnd = mCenterX - (mWidth * 0.06f).toInt()
                paint.color = mDegreesColor
                paint.strokeWidth = mWidth * 0.02f
            } else {
                rEnd = mCenterX - (mWidth * 0.04f).toInt()
                paint.color = mDegreesColor
                paint.strokeWidth = mWidth * 0.015f
                paint.alpha = DEFAULT_MINIMIZED_ALPHA
            }
            val startX = (mCenterX + rPadded * cos(Math.toRadians(degree.toDouble()))).toInt()
            val startY = (mCenterX - rPadded * sin(Math.toRadians(degree.toDouble()))).toInt()
            val stopX = (mCenterX + rEnd * cos(Math.toRadians(degree.toDouble()))).toInt()
            val stopY = (mCenterX - rEnd * sin(Math.toRadians(degree.toDouble()))).toInt()

            canvas.drawLine(
                startX.toFloat(),
                startY.toFloat(),
                stopX.toFloat(),
                stopY.toFloat(),
                paint
            )
            degree += mDegreesStep
        }
    }

    private fun showOrientationLabel(
        canvas: Canvas,
        textPaint: TextPaint,
        rect: Rect,
        i: Int,
        rText: Int
    ) {
        if (mShowOrientationLabel) {
            val textX = (mCenterX + rText * cos(Math.toRadians(i.toDouble()))).toInt()
            val textY = (mCenterX - rText * sin(Math.toRadians(i.toDouble()))).toInt()
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
        mDegreesColor = degreesColor
        invalidate()
    }

    fun setShowOrientationLabel(showOrientationLabel: Boolean) {
        mShowOrientationLabel = showOrientationLabel
        invalidate()
    }

    fun setDegreesStep(degreesStep: Int) {
        mDegreesStep = degreesStep
        invalidate()
    }

    fun setOrientationLabelsColor(orientationLabelsColor: Int) {
        mOrientationLabelsColor = orientationLabelsColor
        invalidate()
    }

    fun setShowBorder(showBorder: Boolean) {
        mShowBorder = showBorder
        invalidate()
    }

    fun setBorderColor(borderColor: Int) {
        mBorderColor = borderColor
        invalidate()
    }
}
