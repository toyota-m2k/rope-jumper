package io.github.toyota32k.ropejumper.app

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.toyota32k.ropejumper.calibration.CalibrationViewModel
import io.github.toyota32k.ropejumper.system.KineticSensorFactory
import java.lang.ref.WeakReference

class MainViewModel : ViewModel() {
    companion object {
        fun instanceFor(activity: FragmentActivity): MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java).apply {
            }
        }
    }
    var activityRef:WeakReference<FragmentActivity>? = null
    val activity:FragmentActivity?
        get() = activityRef?.get()

    val sensorFactory: KineticSensorFactory by lazy {
        KineticSensorFactory(RJApplication.instance)
    }

    val calibrationModel: CalibrationViewModel by lazy { CalibrationViewModel(this) }

    override fun onCleared() {
        super.onCleared()
        calibrationModel.dispose()
    }
}