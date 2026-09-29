package id.linuwih.sholat.qibla.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** azimuthDegrees: 0 = utara magnetik, searah jarum jam. accuracy: SensorManager.SENSOR_STATUS_*. */
data class CompassReading(val azimuthDegrees: Float, val accuracy: Int)

/**
 * Kompas dari sensor bawaan HP. Prioritas TYPE_ROTATION_VECTOR (lebih stabil, tersedia di
 * hampir semua hp modern); fallback ke accelerometer+magnetometer untuk hp jadul yang belum
 * punya rotation vector sensor.
 */
class CompassSensor(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    fun hasSensor(): Boolean = rotationVectorSensor != null || (accelerometer != null && magnetometer != null)

    fun readings(): Flow<CompassReading> = callbackFlow {
        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var lastAccel: FloatArray? = null
        var lastMagnetic: FloatArray? = null
        var lastAccuracy = SensorManager.SENSOR_STATUS_UNRELIABLE

        fun emitFromMatrix() {
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
            trySend(CompassReading(azimuth, lastAccuracy))
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        emitFromMatrix()
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        lastAccel = event.values.clone()
                        val accel = lastAccel
                        val magnetic = lastMagnetic
                        if (accel != null && magnetic != null &&
                            SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnetic)
                        ) {
                            emitFromMatrix()
                        }
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lastMagnetic = event.values.clone()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    lastAccuracy = accuracy
                }
            }
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
