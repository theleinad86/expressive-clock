package app.expressiveclock.alarm

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt

/**
 * Counts accelerometer peaks above [thresholdG] and fires [onShake] once [requiredShakes] of them
 * land inside [windowMillis]. Peaks closer than [debounceMillis] count as one shake, so a single
 * jolt that rings for several samples is not mistaken for a series. Resets itself after firing.
 */
class ShakeDetector(
    private val context: Context,
    private val thresholdG: Float,
    private val requiredShakes: Int,
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val onShake: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val shakes = ArrayDeque<Long>()
    private var lastShakeAt = 0L
    private var running = false

    /** Returns false when the device has no accelerometer. */
    fun start(): Boolean {
        if (running) return true
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        reset()
        running = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        return running
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
        reset()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val (x, y, z) = Triple(event.values[0], event.values[1], event.values[2])
        onAcceleration(sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH, SystemClock.elapsedRealtime())
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** Pure counting logic, separated from the sensor so it can be reasoned about on its own. */
    internal fun onAcceleration(gForce: Float, now: Long) {
        if (gForce < thresholdG) return
        if (now - lastShakeAt < debounceMillis) return
        lastShakeAt = now
        shakes.addLast(now)
        while (shakes.isNotEmpty() && now - shakes.first() > windowMillis) shakes.removeFirst()
        if (shakes.size >= requiredShakes) {
            reset()
            onShake()
        }
    }

    private fun reset() {
        shakes.clear()
        lastShakeAt = 0L
    }

    companion object {
        const val DEFAULT_WINDOW_MILLIS = 3_000L
        const val DEFAULT_DEBOUNCE_MILLIS = 150L
    }
}
