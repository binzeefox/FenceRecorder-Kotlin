package com.cloud_hermits.fencerecorder.cat

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.ui.utils.ViewUtil
import com.binzeefox.foxdevframe_kotlin.utils.LogUtil
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.MyApplication.Companion.clearTables
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.fencerecorder.utils.JxlUtils
import com.tencent.bugly.beta.Beta
import org.w3c.dom.Text
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp

/**
 * 比赛设置Activity
 *
 * @author tong.xw
 * 2021/02/01 18:03
 */
class MatchConfigActivity : BaseActivity() {
    private val exportHint = "*导出内容位于${JxlUtils.cacheDir.absolutePath}"

    override fun getContentViewResource(): Int = R.layout.activity_match_config

    override fun onCreate() {
        super.onCreate()

        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@MatchConfigActivity.title = "比赛设置"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        findViewById<TextView>(R.id.tv_export_hint)?.text = exportHint

        ViewUtil(findViewById(R.id.clear_data))
            .setOnDeBounceClickListener(500) {
                AlertDialog.Builder(this)
                    .setTitle("警告")
                    .setMessage("该操作将清除所有记录，是否继续")
                    .setCancelable(true)
                    .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton("清除") { _, _ ->
                        ThreadUtils.callIO {
                            FoxCore.clearTables()
                            ThreadUtils.runOnUiThread {
                                NoticeUtil.toast("清除数据成功").showNow()
                            }
                        }
                    }.show()
            }

        ViewUtil(findViewById(R.id.btn_update))
            .setOnDeBounceClickListener(500) {
                Beta.checkUpgrade(true, false)  //检查更新
            }

        // 导出人员表
        ViewUtil(findViewById(R.id.btn_export_members))
            .setOnDeBounceClickListener(500) {
                ThreadUtils.executeIO {
                    try {
                        JxlUtils.exportMemberExcel()
                        runOnUiThread {
                            NoticeUtil.toast("导出成功").showNow()
                        }
                    } catch (e: Exception) {
                        LogUtil(javaClass.name).setMessage("导出人员表失败").setThrowable(e).e()
                        runOnUiThread {
                            NoticeUtil.toast("导出失败").showNow()
                        }
                    }
                }
            }

        // 导出总表
        ViewUtil(findViewById(R.id.btn_export_all))
            .setOnDeBounceClickListener(500) {
                ThreadUtils.executeIO {
                    try {
                        JxlUtils.exportFullExcel()
                        runOnUiThread {
                            NoticeUtil.toast("导出成功").showNow()
                        }
                    } catch (e: Exception) {
                        LogUtil(javaClass.name).setMessage("导出总表失败").setThrowable(e).e()
                        runOnUiThread {
                            NoticeUtil.toast("导出失败").showNow()
                        }
                    }
                }
            }

        val period = MatchConfig.matchPeriod
        findViewById<TextView>(R.id.period_minute).text =
            SimpleDateFormat("m", Locale.CHINA).format(period)
        findViewById<TextView>(R.id.period_second).text =
            SimpleDateFormat("ss", Locale.CHINA).format(period)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_save) saveConfig()
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_match_config, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun saveConfig() {
        // 比赛时长
        val minField = findViewById<EditText>(R.id.period_minute)
        val secField = findViewById<EditText>(R.id.period_second)
        arrayOf(minField, secField).forEach {
            if (it.text.isNullOrBlank()) it.setText(0.toString())
        }
        val minute = minField?.text.toString().toInt()
        val second = secField?.text.toString().toInt()
        val period = minute * 60 * 1000 + second * 1000

        // 同步
        MatchConfig.apply {
            matchPeriod = period.toLong()
            applyChanges()
            NoticeUtil.toast("保存成功").showNow()
        }
    }
}

/**
 * 比赛设置
 *
 * 调用setter后必须调用applyChanges或abandonChanges方法，否则会造成泄露\
 */
@SuppressLint("CommitPrefEdits")
object MatchConfig {
    private const val FILE_NAME = "MatchConfig"
    private const val DEFAULT_PERIOD: Long = 3 * 60 * 1000    //单局默认时长毫秒数
    private const val KEY_MATCH_PERIOD = "key_match_period" //比赛时长

    private val sp: SharedPreferences
        get() = FoxCore.appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private var editor: SharedPreferences.Editor? = null

    /**
     * 比赛时长
     */
    var matchPeriod: Long = DEFAULT_PERIOD
        get() = sp.getLong(KEY_MATCH_PERIOD, DEFAULT_PERIOD)
        set(value) {
            editor ?: kotlin.run { editor = sp.edit() }
            editor?.also {
                it.putLong(KEY_MATCH_PERIOD, value).apply()
            }
            field = value
        }

    fun applyChanges() = editor?.apply()
    fun abandonChanges() {
        editor = null
    }
}