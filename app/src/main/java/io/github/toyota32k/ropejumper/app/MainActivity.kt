package io.github.toyota32k.ropejumper.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.michael.ropejumper.R
import io.github.toyota32k.ropejumper.calibration.CalibrationFragment

class MainActivity : AppCompatActivity() {
    lateinit var viewModel:MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = MainViewModel.instanceFor(this)

        if(savedInstanceState==null) {
            val root = CalibrationFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, root)
                .commit()
        }

    }
}