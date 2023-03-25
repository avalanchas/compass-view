package edu.arbelkilani.compass

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import kotlin.math.roundToLong

private const val NEEDLE_PADDING = 0.17f
private const val DEGREE = "\u00b0"
private const val DATA_PADDING = 0.35f
private const val TEXT_SIZE_FACTOR = 0.014f
private const val DEFAULT_DEGREES_STEP = 15
private const val DEFAULT_SHOW_ORIENTATION_LABEL = false
private const val DEFAULT_SHOW_DEGREE_VALUE = false
private const val DEFAULT_ORIENTATION_LABEL_COLOR = Color.BLACK
private const val DEFAULT_SHOW_BORDER = false
private const val DEFAULT_BORDER_COLOR = Color.BLACK

class Compass : RelativeLayout, SensorEventListener {
    private lateinit var mNeedleImageView: ImageView
    private lateinit var mDegreeTextView: TextView
    private var mCurrentDegree = 0f
    private var mShowBorder = false
    private var mBorderColor = 0
    private var mDegreesColor = 0
    private var mShowOrientationLabels = false
    private var mOrientationLabelsColor = 0
    private var mDegreeValueColor = 0
    private var mShowDegreeValue = false
    private var mDegreesStep = 0
    private var mNeedle: Drawable? = null
    private var mCompassListener: CompassListener? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (widthMeasureSpec < heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec)
        } else {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec)
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.compass_layout, this, true)
        val manager = getContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        manager.registerListener(
            this,
            manager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME
        )
        initValues(context, attrs)
        updateLayout()
        updateNeedle()
    }

    private fun initValues(context: Context, attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.Compass, 0, 0)
        mShowBorder = typedArray.getBoolean(R.styleable.Compass_show_border, DEFAULT_SHOW_BORDER)
        mBorderColor = typedArray.getColor(R.styleable.Compass_border_color, DEFAULT_BORDER_COLOR)
        mDegreesColor = typedArray.getColor(R.styleable.Compass_degree_color, Color.BLACK)
        mShowOrientationLabels = typedArray.getBoolean(
            R.styleable.Compass_show_orientation_labels,
            DEFAULT_SHOW_ORIENTATION_LABEL
        )
        mOrientationLabelsColor = typedArray.getColor(
            R.styleable.Compass_orientation_labels_color,
            DEFAULT_ORIENTATION_LABEL_COLOR
        )
        mDegreeValueColor = typedArray.getColor(R.styleable.Compass_degree_value_color, Color.BLACK)
        mShowDegreeValue =
            typedArray.getBoolean(R.styleable.Compass_show_degree_value, DEFAULT_SHOW_DEGREE_VALUE)
        mDegreesStep = typedArray.getInt(R.styleable.Compass_degrees_step, DEFAULT_DEGREES_STEP)
        require(mDegreesStep in 1..359 && 360 % mDegreesStep == 0) {
            "Invalid degree step {$mDegreesStep}"
        }
        mNeedle = typedArray.getDrawable(R.styleable.Compass_needle)
        typedArray.recycle()
    }

    private fun updateLayout() {
        mDegreeTextView = findViewById(R.id.tv_degree)
        val compassSkeleton = findViewById<CompassSkeleton>(R.id.compass_skeleton)
        compassSkeleton.setDegreesColor(mDegreesColor)
        compassSkeleton.setShowOrientationLabel(mShowOrientationLabels)
        compassSkeleton.setShowBorder(mShowBorder)
        compassSkeleton.setBorderColor(mBorderColor)
        compassSkeleton.setDegreesStep(mDegreesStep)
        compassSkeleton.setOrientationLabelsColor(mOrientationLabelsColor)
        val dataLayout = findViewById<View>(R.id.data_layout)
        compassSkeleton.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                compassSkeleton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = compassSkeleton.measuredWidth
                val needlePadding = (width * NEEDLE_PADDING).toInt()
                compassSkeleton.setPadding(
                    needlePadding,
                    needlePadding,
                    needlePadding,
                    needlePadding
                )
                val dataPaddingTop = (width * DATA_PADDING).toInt()
                dataLayout.setPadding(0, dataPaddingTop, 0, 0)
                val degreeTextSize = width * TEXT_SIZE_FACTOR
                mDegreeTextView.textSize = degreeTextSize
            }
        })
        mDegreeTextView.setTextColor(mDegreeValueColor)
        if (mShowDegreeValue) {
            mDegreeTextView.visibility = VISIBLE
        } else {
            mDegreeTextView.visibility = GONE
        }
    }

    private fun updateNeedle() {
        if (mNeedle == null) {
            mNeedle = ContextCompat.getDrawable(context, R.drawable.ic_needle)
        }
        mNeedleImageView = findViewById(R.id.iv_needle)
        mNeedleImageView.setImageDrawable(mNeedle)
    }

    override fun onSensorChanged(event: SensorEvent) {
        mCompassListener?.onSensorChanged(event)
        val degree = event.values[0].roundToLong().toFloat()
        val rotateAnimation = RotateAnimation(
            mCurrentDegree,
            -degree,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnimation.duration = 210
        rotateAnimation.fillAfter = true
        mNeedleImageView.startAnimation(rotateAnimation)
        updateTextDirection(mCurrentDegree.toDouble())
        mCurrentDegree = -degree
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        mCompassListener?.onAccuracyChanged(sensor, accuracy)
    }

    private fun updateTextDirection(degree: Double) {
        val deg = 360 + degree
        val decimalFormat = DecimalFormat("###.#")
        val value: String = if (deg > 0 && deg <= 90) {
            String.format("%s%s NE", decimalFormat.format(-degree), DEGREE)
        } else if (deg > 90 && deg <= 180) {
            String.format("%s%s ES", decimalFormat.format(-degree), DEGREE)
        } else if (deg > 180 && deg <= 270) {
            String.format("%s%s SW", decimalFormat.format(-degree), DEGREE)
        } else {
            String.format("%s%s WN", decimalFormat.format(-degree), DEGREE)
        }
        mDegreeTextView.text = value
    }

    fun setListener(compassListener: CompassListener?) {
        mCompassListener = compassListener
    }

}
