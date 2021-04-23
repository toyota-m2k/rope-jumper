package io.github.toyota32k.ropejumper.app

import android.content.Context
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import io.github.toyota32k.ropejumper.calibration.CalibrationViewModel
import io.github.toyota32k.ropejumper.system.KineticSensorFactory
import io.github.toyota32k.utils.IDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class DisposableLazy<T>(val fn:()->T) : IDisposable {
    var _value:T? = null
    val value:T
        get() {
            return _value ?: fn().apply { _value=this }
        }
    override fun dispose() {
        val v = _value
        if(v!=null) {
            _value = null
            if(v is IDisposable) {
                v.dispose()
            } else if(v is Disposable) {
                v.dispose()
            }
        }
    }

    override fun isDisposed(): Boolean {
        return _value == null
    }

//    override fun getValue(thisRef: R, property: KProperty<*>): T {
//        return _value ?: fn().apply { _value=this }
//    }
}

data class ThresholdParams(
    val thNegHit:Float,
    val thNegMed:Float,
    val thPosHit:Float,
    val thPosMed:Float) {
    companion object {
        val empty = ThresholdParams(0f,0f,0f,0f)
    }
    val isEmpty
        get() = thNegHit==0f && thNegMed==0f && thPosHit==0f && thPosMed == 0f
}


class MainViewModel : ViewModel() {
    companion object {
        fun instanceFor(activity: FragmentActivity): MainViewModel {
            return ViewModelProvider(activity, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java).apply {
                thresholds = loadParams(activity)
            }
        }
        fun saveParams(context: Context, p:ThresholdParams?) {
            if(p==null || p.isEmpty) return
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            pref.edit {
                putBoolean("HasData", true)
                putFloat(ThresholdParams::thNegHit.name, p.thNegHit)
                putFloat(ThresholdParams::thNegMed.name, p.thNegMed)
                putFloat(ThresholdParams::thPosHit.name, p.thPosHit)
                putFloat(ThresholdParams::thPosMed.name, p.thPosMed)
            }
        }
        fun loadParams(context: Context) : ThresholdParams? {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            if(!pref.getBoolean("HasData", false)) {
                return null
            }
            return ThresholdParams(
                pref.getFloat(ThresholdParams::thNegHit.name,0f),
                pref.getFloat(ThresholdParams::thNegMed.name, 0f),
                pref.getFloat(ThresholdParams::thPosHit.name, 0f),
                pref.getFloat(ThresholdParams::thPosMed.name,0f)
            )
        }

    }

    enum class FragmentIndex {
        None,
        Counter,
        Calibration,
    }

    var thresholds:ThresholdParams? = null
    var activityRef:WeakReference<FragmentActivity>? = null
    val activity:FragmentActivity?
        get() = activityRef?.get()

    val sensorFactory: KineticSensorFactory by lazy {
        KineticSensorFactory(RJApplication.instance)
    }

    val currentFragment = MutableLiveData<FragmentIndex>(FragmentIndex.None)

    val calibrationModelSource = DisposableLazy { CalibrationViewModel(this) }
    val calibrationModel by calibrationModelSource::value

//    var _calibrationModel: CalibrationViewModel? = null
//    val calibrationModel:CalibrationViewModel
//        get() {
//            if(_calibrationModel==null) {
//                _calibrationModel = CalibrationViewModel(this)
//            }
//            return _calibrationModel!!
//        }
//    fun disposeCalibrationModel() {
//        _calibrationModel?.dispose()
//        _calibrationModel = null
//    }

    override fun onCleared() {
        super.onCleared()
        calibrationModelSource.dispose()
    }




}