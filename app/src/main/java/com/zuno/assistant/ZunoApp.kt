package com.zuno.assistant

import android.app.Application

class ZunoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ZunoGraph.init(this)
    }
}
