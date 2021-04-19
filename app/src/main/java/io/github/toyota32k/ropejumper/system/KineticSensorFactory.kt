package io.github.toyota32k.ropejumper.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.lang.IllegalArgumentException

/**
 * https://developer.android.com/guide/topics/sensors/sensors_motion?hl=ja
 */
class KineticSensorFactory(context:Context) {
    enum class SensorType(val id:Int, val dimension:Int) {
        Gravity(Sensor.TYPE_GRAVITY, 3),                        // 重力センサー
        LinearAcceleration(Sensor.TYPE_LINEAR_ACCELERATION, 3), // 線形加速度計
        RotationVector(Sensor.TYPE_ROTATION_VECTOR,4),          // 回転ベクトル センサー
        SignificantMotion(Sensor.TYPE_SIGNIFICANT_MOTION,0),    // 移動検出センサー
        StepCount(Sensor.TYPE_STEP_COUNTER,1),                  // 歩数計センサー
        StepDetector(Sensor.TYPE_STEP_DETECTOR,0),              // 歩行検出カウンター

        Accelerometer(Sensor.TYPE_ACCELEROMETER,3),             // 加速度センサー
        Gyroscope(Sensor.TYPE_GYROSCOPE,3),                     // ジャイロスコープ
    }

    enum class SamplingRate(val delay:Int) {
        Fastest(SensorManager.SENSOR_DELAY_FASTEST),
        Normal(SensorManager.SENSOR_DELAY_NORMAL),
        Game(SensorManager.SENSOR_DELAY_GAME),
        UI(SensorManager.SENSOR_DELAY_UI)
    }

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    data class Vector3(val x:Float, val y:Float, val z:Float)
    data class Vector3_1(val x:Float, val y:Float, val z:Float, val s:Float)    // for TYPE_ROTATION_VECTOR

    abstract inner class AbsSensorObserver<T>(val sensor:Sensor, val samplingDelay:Int) : SensorEventListener {
        val subject = PublishSubject.create<T>()
        val flowable:Flowable<T>
            get() = subject.toFlowable(BackpressureStrategy.BUFFER)
        val observable:Observable<T>
            get() = subject
        var isActive:Boolean = false
            private set(v) {field=v}

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }

        override abstract fun onSensorChanged(event: SensorEvent?)

        fun start() {
            isActive = true
            sensorManager.registerListener(this, sensor, samplingDelay)
        }
        fun stop() {
            if(isActive) {
                sensorManager.unregisterListener(this, sensor)
                isActive = false
            }
        }
    }

    interface ISensor {
        fun start()
        fun stop()
    }
    interface ISensorVector3:ISensor {
        val observable : Observable<Vector3>
    }
    interface ISensorVector3_1:ISensor {
        val observable : Observable<Vector3_1>
    }
    interface ISensorScalar:ISensor {
        val observable : Observable<Float>
    }
    interface ISensorUnit:ISensor {
        val observable : Observable<Unit>
    }

    inner class SensorObserverVector3(sensor:Sensor, samplingDelay:Int) : AbsSensorObserver<Vector3>(sensor,samplingDelay), ISensorVector3 {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                subject.onNext(Vector3(event.values[0],event.values[1],event.values[2]))
            }
        }
    }
    inner class SensorObserverVector3_1(sensor:Sensor, samplingDelay:Int) : AbsSensorObserver<Vector3_1>(sensor,samplingDelay), ISensorVector3_1 {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                subject.onNext(Vector3_1(event.values[0],event.values[1],event.values[2],event.values[3]))
            }
        }
    }
    inner class SensorObserverScalar(sensor:Sensor, samplingDelay:Int) : AbsSensorObserver<Float>(sensor,samplingDelay), ISensorScalar {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                subject.onNext(event.values[0])
            }
        }
    }
    inner class SensorObserverUnit(sensor:Sensor, samplingDelay:Int) : AbsSensorObserver<Unit>(sensor,samplingDelay), ISensorUnit {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                subject.onNext(Unit)
            }
        }
    }

    fun createSensor(type:SensorType, samplingDelay:Int) : ISensor {
        val sensor = sensorManager.getDefaultSensor(type.id)
        return when(type.dimension) {
            0-> SensorObserverUnit(sensor,samplingDelay)
            1-> SensorObserverScalar(sensor,samplingDelay)
            3-> SensorObserverVector3(sensor,samplingDelay)
            4-> SensorObserverVector3_1(sensor,samplingDelay)
            else-> throw IllegalArgumentException("unknown type $type")
        }
    }

    fun createGravitySensor(samplingDelay:Int):ISensorVector3 {
        return createSensor(SensorType.Gravity,samplingDelay) as ISensorVector3
    }
    fun createLinearAccelerationSensor(samplingDelay:Int):ISensorVector3 {
        return createSensor(SensorType.LinearAcceleration,samplingDelay) as ISensorVector3
    }
    fun createRotationVectorSensor(samplingDelay:Int):ISensorVector3_1 {
        return createSensor(SensorType.RotationVector,samplingDelay) as ISensorVector3_1
    }
    fun createSignificantMotionSensor(samplingDelay:Int):ISensorUnit {
        return createSensor(SensorType.SignificantMotion,samplingDelay) as ISensorUnit
    }
    fun createStepCounterSensor(samplingDelay:Int):ISensorScalar {
        return createSensor(SensorType.StepCount,samplingDelay) as ISensorScalar
    }
    fun createStepDetectorSensor(samplingDelay:Int):ISensorUnit {
        return createSensor(SensorType.StepDetector,samplingDelay) as ISensorUnit
    }
    fun createGravityAccelerometer(samplingDelay:Int):ISensorVector3 {
        return createSensor(SensorType.Accelerometer,samplingDelay) as ISensorVector3
    }
    fun createGyroscopeSensor(samplingDelay:Int):ISensorVector3 {
        return createSensor(SensorType.Gyroscope,samplingDelay) as ISensorVector3
    }

}