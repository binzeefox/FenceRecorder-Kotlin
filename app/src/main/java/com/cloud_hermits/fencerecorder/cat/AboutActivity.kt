package com.cloud_hermits.fencerecorder.cat

import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.R

/**
 * 关于页
 *
 * @author tong.xw
 * 2021/02/01 17:51
 */
class AboutActivity : BaseActivity() {

    private val aboutText: CharSequence
        get() = "${getString(R.string.app_name_offline)} 版本：v${FoxCore.versionName}" +
                "\n\n是由 杭州云栖剑社 成员 狐彻开源的丙级活动计分工具。" +
                "该软件包含计分、计时、暂停、到时提醒和保存比赛记录等功能。" +
                "并允许本地保存成员信息和导出Excel功能，以便社团统计成员活动" +
                "\n\n该软件尚在开发中，当前版本非最终版本，可能导致记录丢失等问题。若出现该情况或其它异常情况" +
                "，请联系 云栖剑社 进行反馈" +
                "\n\n对剑术感兴趣、或希望了解云栖剑社者，云栖剑社欢迎各地友好者前来交流" +
                "\n\n"

    override fun getContentViewResource(): Int = R.layout.activity_about

    override fun onCreate() {
        super.onCreate()

        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@AboutActivity.title = "关于该软件"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        findViewById<TextView>(R.id.tv_about).text = aboutText
    }
}