package com.elliecoding.compass

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
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.elliecoding.compass.databinding.CompassLayoutBinding
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.absoluteValue
import kotlin.math.round

private const val DEFAULT_NEEDLE_PADDING = 0.01f
private const val DEGREE = "\u00b0"
private const val DATA_PADDING = 0.35f
private const val TEXT_SIZE_FACTOR = 0.014f
private const val DEFAULT_DEGREES_STEP = 15
private const val DEFAULT_PRECISION = 0
private const val DEFAULT_SHOW_ORIENTATION_LABEL = false
private const val DEFAULT_SHOW_DEGREE_VALUE = false
private const val DEFAULT_ORIENTATION_LABEL_COLOR = Color.BLACK
private const val DEFAULT_SHOW_BORDER = false
private const val DEFAULT_BORDER_COLOR = Color.BLACK

class Compass : RelativeLayout {
    private lateinit var binding: CompassLayoutBinding
    private val decimalFormat = DecimalFormat("###.#")
    private var currentDegree = 0f
    private var showBorder = false
    private var borderColor = 0
    private var degreesColor = 0
    private var showOrientationLabels = false
    private var orientationLabelsColor = 0
    private var degreeValueColor = 0
    private var showDegreeValue = false
    private var degreesStep = DEFAULT_DEGREES_STEP
    private var precision = DEFAULT_PRECISION
    private var needlePadding = DEFAULT_NEEDLE_PADDING
    private var needle: Drawable? = null
    private var compassListener: CompassListener? = null
    private var targetBearing: Int = 0

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

    // The listener is an internal object so that the public API isn't polluted by the event methods
    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val newDegree = -(event.values[0].round(precision) + targetBearing)
            if (currentDegree.absoluteValue == newDegree.absoluteValue) {
                return
            }
            Timber.v(
                "onSensorChanged called with: currentDegree = %s, degree = %s",
                currentDegree,
                newDegree
            )
            compassListener?.onSensorChanged(event)
            onDegreeChanged(newDegree)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            compassListener?.onAccuracyChanged(sensor, accuracy)
        }
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        binding = CompassLayoutBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        val manager = getContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        manager.registerListener(
            sensorEventListener,
            manager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_UI
        )
        initValues(context, attrs)
        updateLayout()
        updateNeedle()
    }

    private fun initValues(context: Context, attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.Compass, 0, 0)
        showBorder = typedArray.getBoolean(R.styleable.Compass_show_border, DEFAULT_SHOW_BORDER)
        borderColor = typedArray.getColor(R.styleable.Compass_border_color, DEFAULT_BORDER_COLOR)
        degreesColor = typedArray.getColor(R.styleable.Compass_degree_color, Color.BLACK)
        showOrientationLabels = typedArray.getBoolean(
            R.styleable.Compass_show_orientation_labels,
            DEFAULT_SHOW_ORIENTATION_LABEL
        )
        orientationLabelsColor = typedArray.getColor(
            R.styleable.Compass_orientation_labels_color,
            DEFAULT_ORIENTATION_LABEL_COLOR
        )
        degreeValueColor = typedArray.getColor(R.styleable.Compass_degree_value_color, Color.BLACK)
        showDegreeValue =
            typedArray.getBoolean(R.styleable.Compass_show_degree_value, DEFAULT_SHOW_DEGREE_VALUE)
        degreesStep = typedArray.getInt(R.styleable.Compass_degrees_step, DEFAULT_DEGREES_STEP)
        require(degreesStep == NO_STEPS || (degreesStep in 1..359 && 360 % degreesStep == 0)) {
            "Invalid degree step {$degreesStep}"
        }
        needle = typedArray.getDrawable(R.styleable.Compass_needle)
        precision = typedArray.getInt(R.styleable.Compass_precision, DEFAULT_PRECISION)
        require(precision in 0..4) { "Invalid precision {$precision}" }
        needlePadding =
            typedArray.getFloat(R.styleable.Compass_needle_padding, DEFAULT_NEEDLE_PADDING)
        typedArray.recycle()
    }

    private fun updateLayout() {
        val compassSkeleton = findViewById<CompassSkeleton>(R.id.compass_skeleton)
        compassSkeleton.setDegreesColor(degreesColor)
        compassSkeleton.setShowOrientationLabel(showOrientationLabels)
        compassSkeleton.setShowBorder(showBorder)
        compassSkeleton.setBorderColor(borderColor)
        compassSkeleton.setDegreesStep(degreesStep)
        compassSkeleton.setOrientationLabelsColor(orientationLabelsColor)
        val dataLayout = findViewById<View>(R.id.compass_data)
        compassSkeleton.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                compassSkeleton.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = compassSkeleton.measuredWidth
                val needlePadding = (width * DEFAULT_NEEDLE_PADDING).toInt()
                compassSkeleton.setPadding(
                    needlePadding,
                    needlePadding,
                    needlePadding,
                    needlePadding
                )
                val dataPaddingTop = (width * DATA_PADDING).toInt()
                dataLayout.setPadding(0, dataPaddingTop, 0, 0)
                val degreeTextSize = width * TEXT_SIZE_FACTOR
                binding.compassDegreeText.textSize = degreeTextSize
            }
        })
        binding.compassDegreeText.setTextColor(degreeValueColor)
        if (showDegreeValue) {
            binding.compassDegreeText.visibility = VISIBLE
        } else {
            binding.compassDegreeText.visibility = GONE
        }
    }

    private fun updateNeedle() {
        if (needle == null) {
            needle = ContextCompat.getDrawable(context, R.drawable.ic_needle)
        }
        binding.compassNeedle.setImageDrawable(needle)
    }

    private fun Float.round(decimals: Int): Float {
        var multiplier = 1.0f
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    fun setTargetBearing(bearing: Int) {
        targetBearing = -bearing
        // Force a re-calc on set
        onDegreeChanged(currentDegree + targetBearing)
    }

    private fun onDegreeChanged(newDegree: Float) {
        // TODO needle does a 360 when passing through 0°N, avoid animation
        val rotateAnimation = RotateAnimation(
            currentDegree,
            newDegree,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnimation.duration = 210
        rotateAnimation.fillAfter = true
        binding.compassNeedle.startAnimation(rotateAnimation)
        updateTextDirection(currentDegree)
        currentDegree = newDegree
    }

    private fun updateTextDirection(degree: Float) {
        val deg = 360 + degree
        val value: String = if (deg > 0 && deg <= 90) {
            String.format("%s%s NE", decimalFormat.format(-degree), DEGREE)
        } else if (deg > 90 && deg <= 180) {
            String.format("%s%s ES", decimalFormat.format(-degree), DEGREE)
        } else if (deg > 180 && deg <= 270) {
            String.format("%s%s SW", decimalFormat.format(-degree), DEGREE)
        } else {
            String.format("%s%s WN", decimalFormat.format(-degree), DEGREE)
        }
        binding.compassDegreeText.text = value
    }

    fun setListener(listener: CompassListener?) {
        compassListener = listener
    }

}
