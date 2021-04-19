package io.github.toyota32k.ropejumper.app

import android.app.Application

class RJApplication : Application() {
    companion object {
        private var sInstance:RJApplication?=null
        val instance:RJApplication
            get() = sInstance!!
    }
    override fun onCreate() {
        super.onCreate()
        sInstance = this
    }
}