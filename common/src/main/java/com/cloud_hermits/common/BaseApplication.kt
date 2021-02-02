package com.cloud_hermits.common

import android.app.Application
//import com.tencent.bugly.Bugly

/**
 * App基类
 *
 * @author tong.xw
 * 2021/02/01 10:59
 */
abstract class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()
//        Bugly.init(this, BuildConfig.BUGLY_ID, BuildConfig.DEBUG)
    }
}