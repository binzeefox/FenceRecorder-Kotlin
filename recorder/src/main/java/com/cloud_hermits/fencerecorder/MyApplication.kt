package com.cloud_hermits.fencerecorder

import com.cloud_hermits.common.BaseApplication
import com.cloud_hermits.common.BuildConfig
import com.tencent.bugly.Bugly

/**
 * 计分器App
 *
 * @author tong.xw
 * 2021/02/01 15:13
 */
class MyApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        Bugly.init(this, BuildConfig.BUGLY_ID, BuildConfig.DEBUG)
    }
}