package com.github.laelluo.importcourses

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import cn.bmob.v3.Bmob

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Bmob.initialize(this, "b00cf504af53163ec08de10867bbddbc")
        context = applicationContext
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
}