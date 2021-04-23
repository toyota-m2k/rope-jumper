package io.github.toyota32k.ropejumper.calibration

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.lifecycle.map
import com.michael.ropejumper.R
import com.michael.ropejumper.databinding.FragmentCalibrationBinding
import io.github.toyota32k.bindit.*
import io.github.toyota32k.ropejumper.app.MainViewModel
import io.github.toyota32k.utils.ConvertLiveData
import io.github.toyota32k.utils.UtLogger
import io.github.toyota32k.utils.disposableObserve

/**
 */
class CalibrationFragment : Fragment() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    lateinit var viewModel: MainViewModel
    val subModel : CalibrationViewModel
        get() = viewModel.calibrationModel
    val binder = Binder()
    lateinit var controls: FragmentCalibrationBinding
    lateinit var adapter:FieldListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        controls = FragmentCalibrationBinding.inflate(layoutInflater)
        adapter = FieldListAdapter(requireActivity())
        // inflater.inflate(R.layout.fragment_calibration, container, false)
        return controls.root.also { view ->
            viewModel = MainViewModel.instanceFor(requireActivity()).also { model->
                binder.register(
                    model.calibrationModel.statisticsList.disposableObserve(viewLifecycleOwner) {
                        UtLogger.debug("calibrationModel.statisticsList observer")
                        adapter.clear()
                        if(it!=null) {
                            adapter.addAll(it)
                        }
                    },
                    model.calibrationModel.startStopCommand.connectViewEx(controls.startStopButton),
                    model.calibrationModel.resetAnalyzerCommand.connectViewEx(controls.resetAnalyzerButton),
                    model.calibrationModel.resetTrialCommand.connectViewEx(controls.resetTrialButton),
                    model.calibrationModel.updateTrialCommand.connectViewEx(controls.trialButton),
                    model.calibrationModel.registerCommand.connectAndBind(this, controls.registerButton, this::register),

                    TextBinding.create(viewLifecycleOwner, controls.startStopButton, model.calibrationModel.observing.map { if(it) resources.getString(R.string.stop) else resources.getString(R.string.start)} ),
//                    VisibilityBinding.create( viewLifecycleOwner, controls.actualCountInput, model.calibrationModel.hasResult, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByInvisible),
//                    VisibilityBinding.create(viewLifecycleOwner, controls.actualCountSlider, model.calibrationModel.hasResult, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByInvisible),
                    VisibilityBinding.create(viewLifecycleOwner, controls.resultListView, model.calibrationModel.hasError, BoolConvert.Inverse, VisibilityBinding.HiddenMode.HideByInvisible),
//                    VisibilityBinding.create(viewLifecycleOwner, controls.registerButton, model.calibrationModel.hasResult, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByInvisible),
                    VisibilityBinding.create(viewLifecycleOwner, controls.errorMessage, model.calibrationModel.hasError, BoolConvert.Straight, VisibilityBinding.HiddenMode.HideByInvisible),
                    EditNumberBinding.create(viewLifecycleOwner, controls.actualCount, model.calibrationModel.actualCount, BindingMode.TwoWay),
                    SliderBinding.create(viewLifecycleOwner,controls.actualCountSlider, ConvertLiveData<Int,Float>(model.calibrationModel.actualCount,{it?.toFloat()?:0f}, {it?.toInt()?:0}), BindingMode.TwoWay),
                        TextBinding.create(viewLifecycleOwner, controls.trialResultNegHit, model.calibrationModel.analyzer.trial.countByNegHit.map {"HIT: $it"}),
                        TextBinding.create(viewLifecycleOwner, controls.trialResultNegMed, model.calibrationModel.analyzer.trial.countByNegMed.map {"MED: $it"}),
                        TextBinding.create(viewLifecycleOwner, controls.trialResultPosHit, model.calibrationModel.analyzer.trial.countByPosHit.map {"HIT: $it"}),
                        TextBinding.create(viewLifecycleOwner, controls.trialResultPosMed, model.calibrationModel.analyzer.trial.countByPosMed.map {"MED: $it"}),
                )
            }
            controls.resultListView.adapter = adapter
        }
    }

    fun register(view:View?) {
        val p = if(!viewModel.calibrationModel.analyzer.trial.thresholds.isEmpty) {
           viewModel.calibrationModel.analyzer.trial.thresholds
        } else if(!viewModel.calibrationModel.statistics.thresholds.isEmpty) {
           viewModel.calibrationModel.statistics.thresholds
        } else null
        MainViewModel.saveParams(requireContext(), p)
        viewModel.thresholds = p
    }

    companion object {
    }

    class FieldListAdapter(context: Context): ArrayAdapter<CalibrationViewModel.Statistics.Field>(context, 0) {
        override fun isEnabled(index: Int): Boolean {
            return true
        }

        private fun createView(parent: ViewGroup): View {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            return inflater.inflate(R.layout.result_field_layout, parent, false)
        }

        override fun getView(index: Int, convertView: View?, parent: ViewGroup): View {
            return (convertView ?: createView(parent)).apply {
                val item = getItem(index)
                findViewById<TextView>(R.id.field_label).text = item?.label
                findViewById<TextView>(R.id.field_value).text = item?.value
            }
        }

    }
}