package com.cloud_hermits.common

import com.binzeefox.foxdevframe_kotlin.ui.FoxActivity

/**
 * 活动基类
 *
 * @author tong.xw
 * 2021/02/01 10:58
 */
abstract class BaseActivity: FoxActivity() {

    override fun onCreate() {
        setFullScreen()
    }
}