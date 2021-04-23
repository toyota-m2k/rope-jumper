package io.github.toyota32k.ropejumper.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.michael.ropejumper.R
import io.github.toyota32k.bindit.Binder
import io.github.toyota32k.ropejumper.calibration.CalibrationFragment
import io.github.toyota32k.utils.disposableObserve

class MainActivity : AppCompatActivity() {
    lateinit var viewModel:MainViewModel
    val binder = Binder()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = MainViewModel.instanceFor(this).also { model->
            binder.register(model.currentFragment.disposableObserve(this, this::changePanel))
        }

        if(savedInstanceState==null) {
            val root = CalibrationFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, root)
                .commit()
        }

    }

    fun changePanel(panel:MainViewModel.FragmentIndex) {

    }
}