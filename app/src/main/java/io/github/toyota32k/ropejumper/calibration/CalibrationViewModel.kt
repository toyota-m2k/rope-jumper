package io.github.toyota32k.ropejumper.calibration

import androidx.preference.PreferenceManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.github.toyota32k.bindit.Command
import io.github.toyota32k.ropejumper.app.MainViewModel
import io.github.toyota32k.ropejumper.app.ThresholdParams
import io.github.toyota32k.ropejumper.system.DisposablePool
import io.github.toyota32k.ropejumper.system.KineticSensorFactory
import io.github.toyota32k.utils.IDisposable
import io.github.toyota32k.utils.UtLogger
import io.reactivex.rxjava3.disposables.Disposable
import java.lang.Float.max
import java.lang.Float.min
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.min

fun Float.toString(numOfDec: Int): String {
    if (numOfDec < 0) {
        return this.toString()
    }
    return String.format(("%.${numOfDec}f").format(this))
}
class CalibrationViewModel(parentModel:MainViewModel) : IDisposable {
    val rootModelRef = WeakReference<MainViewModel>(parentModel)
    val rootModel:MainViewModel
        get() = rootModelRef.get()!!
    val disposablePool = DisposablePool()

    val startStopCommand = Command()
    val resetAnalyzerCommand = Command()
    val resetTrialCommand = Command()
    val updateTrialCommand = Command()
    val registerCommand = Command()

    val sensor = rootModel.sensorFactory.createLinearAccelerationSensor(KineticSensorFactory.SamplingRate.Fastest.delay)

    data class Range(var min:Float=0f, var max:Float=0f) {
        fun update(v:Float) {
            min = Math.min(min,v)
            max = Math.max(max,v)
        }

        override fun toString(): String {
            return "${min.toString(4)} ... ${max.toString(4)}"
        }
    }
    data class Peek(val value:Float, val step:Int) {
        val dv:Float
            get() = value/step
    }

    class Trial {
        val enabled = MutableLiveData(false)
        val countByNegHit = MutableLiveData(0)
        val countByNegMed = MutableLiveData(0)
        val countByPosHit = MutableLiveData(0)
        val countByPosMed = MutableLiveData(0)

        var thresholds:ThresholdParams = ThresholdParams.empty

        fun reset() {
            countByNegHit.value = 0
            countByNegMed.value = 0
            countByPosHit.value = 0
            countByPosMed.value = 0
        }

        fun setThreshold(s:Statistics) {
            enabled.value = true
            thresholds = s.thresholds
        }

        fun updatePositive(v:Float) {
            if (enabled.value == true) {
                if (v > thresholds.thPosHit) {
                    countByPosHit.value = (countByPosHit.value ?: 0) + 1
                }
            }
            if (v > thresholds.thPosMed) {
                countByPosMed.value = (countByPosMed.value ?: 0) + 1
            }
        }
        fun updateNegative(v:Float) {
            if (enabled.value == true) {
                if (v < thresholds.thNegHit) {
                    countByNegHit.value = (countByNegHit.value ?: 0) + 1
                }
                if (v < thresholds.thNegMed) {
                    countByNegMed.value = (countByNegMed.value ?: 0) + 1
                }
            }
        }
    }

    class Analyzer {
        companion object {
            const val TH_VALUE = 0.5
            const val TH_DELTA = 0.01
        }

        val trial = Trial()
        val range = Range()
        var prevValue:Float = 0f
        var prevDelta:Float = 0f
        var step:Int = 0
        var totalCount = 0

        val peekPositive = mutableListOf<Peek>()
        val peekNegative = mutableListOf<Peek>()

        private fun sign(v:Float):Int {
            return if(v<0) -1 else 1
        }

        fun reset() {
            totalCount = 0
            step = 0
            prevValue = 0f
            prevDelta = 0f
            peekNegative.clear()
            peekPositive.clear()
        }

        fun update(v:Float) {
            range.update(v)
            if(totalCount==0) {
                prevValue = v
                step = 0
            } else {
                step++
                val delta = v - prevValue
                if(delta.absoluteValue>TH_DELTA && totalCount>2 && sign(delta)!=sign(prevDelta)) {
                    if(sign(prevDelta)>0) {
                        if(prevValue> TH_VALUE) {
                            peekPositive.add(Peek(v, step))
                            trial.updatePositive(v)
                        }
                    } else {
                        if(prevValue< TH_VALUE) {
                            peekNegative.add(Peek(v, step))
                            trial.updateNegative(v)
                        }
                    }
                    step = 0
                }
                prevValue = v
                prevDelta = delta
            }
            totalCount++
        }
    }

    class Result(analyzer:Analyzer) {
        val peekNegative:List<Peek> = analyzer.peekNegative.sortedByDescending { -it.value }
        val peekPositive:List<Peek> = analyzer.peekPositive.sortedByDescending { it.value }
        val totalCount:Int = analyzer.totalCount
        val range:Range = analyzer.range
        val available:Boolean
            get() = peekNegative.size>=5 && peekPositive.size>5

        val efficientSampleCount:Int
            get() = min(peekNegative.size, peekPositive.size)-1

        data class Detail(val min:Float, val max:Float, val hit:Float, val range:Range) {
            val median:Float
                get() = (max+min)/2
        }

        fun getDetail(actualCount:Int, positive:Boolean):Detail {
            val peeks = if(positive) peekPositive else peekNegative
            if(peeks.size<actualCount+1) {
                return Detail(0f,0f,0f, Range())
            }
            return Detail(peeks[actualCount-2].value, peeks[actualCount].value, peeks[actualCount-1].value, Range(peeks.last().value, peeks.first().value))
        }

        fun trialCount(threshold:Float, positive:Boolean) : Int {
            return if(positive) {
                peekPositive.count {
                    it.value > threshold
                }
            } else {
                peekNegative.count {
                    it.value < threshold
                }
            }
        }
    }

    class Statistics() {
        enum class FieldType(val label:String, val order:Int) {
            TOTAL_COUNT("Total Samples", 10),
            RANGE("Value Range", 20),
            NEG_COUNT("- Peek Count", 110),
            NEG_RANGE("- Range", 120),
            NEG_MIN("- Min", 130),
            NEG_MAX("- Max", 140),
            NEG_HIT("- Hit", 150),
            NEG_MEDIAN("- Median", 160),
            POS_COUNT("+ Peek Count", 210),
            POS_RANGE("+ Range", 220),
            POS_MIN("+ Min", 230),
            POS_MAX("+ Max", 240),
            POS_HIT("+ Hit", 250),
            POS_MEDIAN("+ Median", 260),
        }

        var thresholds:ThresholdParams = ThresholdParams.empty
//        var thNegHit:Float = 0f
//        var thNegMed:Float = 0f
//        var thPosHit:Float = 0f
//        var thPosMed:Float = 0f

        data class Field(val type:FieldType, val value:String, val rawData:Any?=null) {
            val label = type.label
        }

        private val fields = mutableMapOf<FieldType,Field>()

        val toList:List<Field>
            get() = fields.values.sortedBy { it.type.order }

        fun add(field:Field) {
            fields[field.type] = field
        }
        fun clear() {
            fields.clear()
            thresholds = ThresholdParams.empty
        }


        fun setResult(result:Result, actualCount: Int) {
            clear()
            if(!result.available) return

            val neg = result.getDetail(actualCount, false)
            val pos = result.getDetail(actualCount, true)

            thresholds = ThresholdParams(neg.hit, neg.median, pos.hit, pos.median)

            add(Field(FieldType.TOTAL_COUNT, result.totalCount.toString(4)))
            add(Field(FieldType.RANGE, result.range.toString()))

            add(Field(FieldType.NEG_COUNT, "${result.peekNegative.size}"))
            add(Field(FieldType.NEG_RANGE, neg.range.toString()))
            add(Field(FieldType.NEG_MAX, neg.max.toString(4)))
            add(Field(FieldType.NEG_MIN, neg.min.toString(4)))
            add(Field(FieldType.NEG_HIT, neg.hit.toString(4)))
            add(Field(FieldType.NEG_MEDIAN, neg.median.toString(4)))

            add(Field(FieldType.POS_COUNT, "${result.peekPositive.size}"))
            add(Field(FieldType.POS_RANGE, pos.range.toString()))
            add(Field(FieldType.POS_MAX, pos.max.toString(4)))
            add(Field(FieldType.POS_MIN, pos.min.toString(4)))
            add(Field(FieldType.POS_HIT, pos.hit.toString(4)))
            add(Field(FieldType.POS_MEDIAN, pos.median.toString(4)))
        }
    }

    val analyzer = Analyzer()
    var result: Result? = null
    val statistics = Statistics()
    val actualCount = MutableLiveData<Int>(10)

    val observing = MutableLiveData(false)
    val hasResult = MutableLiveData(false)
    val hasError = MutableLiveData(false)

    val statisticsList = MutableLiveData<List<Statistics.Field>>()

    var task : Disposable? = null



//    val sampleCount = MutableLiveData<Int>(0)
//    val minValue = MutableLiveData<Float>(0f)
//    val maxValue = MutableLiveData<Float>(0f)
//    val recommendedThreshold = MutableLiveData<Float>(0f)
//    val readyToAnalyze = sampleCount.map { it>0 }
//    val readyToRegister = MutableLiveData<Boolean>(false)
//    val actualCount = MutableLiveData<Int>(10)

    fun resetAnalizer() {
        if(observing.value!=true) {
            analyzer.reset()
            statistics.clear()
            hasResult.value = false
            hasError.value = false
        }
    }

    fun start() {
        UtLogger.debug("start")
        if(task!=null) return
        hasError.value = false
        observing.value = true
        result = null

        task = sensor.observable.subscribe {
            analyzer.update(it.y)
        }
        sensor.start()
    }

    fun stop() {
        UtLogger.debug("stop")
        task?.dispose()
        task = null
        sensor.stop()
        observing.value = false
        analyze()
    }

    fun toggle() {
        UtLogger.debug("toggle")
        if(task!=null) {
            stop()
        } else {
            start()
        }
    }

//    val split = 10

    fun analyze() {
        UtLogger.debug("analyze")
        val r = Result(analyzer)
            if(!r.available) {
                result = null
                hasError.value = true
            } else {
                result = r
                hasResult.value = true
                updateByResult()
            }
    }

    fun updateByResult() {
        UtLogger.debug("updateByResult")
        val result = this.result?:return
        val count = min(actualCount.value?:10, result.efficientSampleCount)
        statistics.setResult(result, count)
        statisticsList.value = statistics.toList
    }

    private fun observeActualCount(count:Int) {
        result?.also { result ->
            UtLogger.debug("actualCount - observer")
            if (result.available && count<result.efficientSampleCount) {
                updateByResult()
            }
        }
    }

    fun updateTrialParameters() {
        if(hasResult.value==true) {
            analyzer.trial.setThreshold(statistics)
        }
    }

    fun resetTrialResult() {
        analyzer.trial.reset()
    }

    init {
        disposablePool
                .observeForever(actualCount, this::observeActualCount)
                .bindForever(startStopCommand, this::toggle)
                .bindForever(resetAnalyzerCommand, this::resetAnalizer)
                .bindForever(resetTrialCommand, this::resetTrialResult)
                .bindForever(updateTrialCommand, this::updateTrialParameters)
    }

    override fun dispose() {
        disposablePool.dispose()
    }

    override fun isDisposed(): Boolean {
        return disposablePool.isDisposed()
    }

    companion object {
    }
}