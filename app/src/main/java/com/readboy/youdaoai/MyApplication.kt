package com.readboy.youdaoai

import android.app.Application
import com.youdao.sdk.app.YouDaoApplication

class MyApplication : Application() {
    companion object {
        lateinit var INSTANCE: MyApplication
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
        YouDaoApplication.init(this, "4c28111a748ee0b1")
    }

}
