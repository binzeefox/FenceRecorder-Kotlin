package com.cloud_hermits.fencerecorder.cat

import android.Manifest
import android.os.Handler
import android.os.Looper
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.ui.utils.launcher.Launcher
import com.binzeefox.foxdevframe_kotlin.ui.utils.requester.permission.PermissionUtil
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.R

/**
 * 闪屏页
 *
 * @author tong.xw
 * 2021/02/01 16:29
 */
class SplashActivity : BaseActivity() {

    override fun getContentViewResource(): Int = R.layout.activity_splash

    override fun onCreate() {
        super.onCreate()

        PermissionUtil(this)
            .addPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .addPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            .checkAndRequest(0x01) { _, failedList, _ ->
                if (failedList.isNotEmpty())
                    NoticeUtil.toast("有权限尚未通过，可能会影响该APP正常使用").showNow()
                startApp()
            }
    }

    /**
     * 倒计时并进入主页
     */
    private fun startApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            Launcher(this).getActivityTarget(MatchListActivity::class.java)
                .commit()
            finish()
        }, 1500)
    }
}