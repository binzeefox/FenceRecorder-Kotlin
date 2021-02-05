package com.cloud_hermits.fencerecorder.cat

import android.app.DatePickerDialog
import android.database.sqlite.SQLiteConstraintException
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.utils.LogUtil
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.MyApplication.Companion.database
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.fencerecorder.db.tables.MemberCondition
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加成员
 *
 * @author tong.xw
 * 2021/02/02 17:12
 */
class AddMemberActivity : BaseActivity() {
    private val genderMap: Map<String, Int> = mapOf(
        Pair("女", 0),
        Pair("男", 1),
        Pair("未知", 9),
        Pair("其它", 8)
    )
    private val nicknameField: TextInputEditText? get() = findViewById(R.id.nickname_field)
    private val birthdayField: TextInputEditText? get() = findViewById(R.id.birthday_field)
    private val genderField: TextInputEditText? get() = findViewById(R.id.gender_field)
    private val commentField: EditText? get() = findViewById(R.id.comment_field)
    private val condition = MemberCondition(
        nickname = "",
        birthday = Date().time,
        comment = null
    )

    override fun getContentViewResource(): Int = R.layout.activity_member_add

    override fun onCreate() {
        super.onCreate()
        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@AddMemberActivity.title = "添加成员"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        arrayOf(birthdayField, genderField).forEach {
            it?.isFocusable = false
        }

        birthdayField?.setOnClickListener {
            DatePickerDialog(this).apply {
                setOnDateSetListener { _, year, month, dayOfMonth ->
                    val time = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }.time.time
                    birthdayField?.setText(
                        SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
                            .format(time)
                    )
                    condition.birthday = time
                }
                title = "选择出生日期"
            }.show()
        }

        genderField?.setOnClickListener {
            val list = arrayOf("男", "女", "其它", "未知")
            AlertDialog.Builder(this)
                .setItems(list) { _, position ->
                    condition.gender = genderMap[list[position]] ?: 9
                    genderField?.setText(list[position])
                }.show()
        }

        findViewById<View>(R.id.fab_add).setOnClickListener {
            condition.comment = commentField?.text.toString()
            condition.nickname = nicknameField?.text.toString()
            nicknameField?.run {
                if (text.isNullOrBlank()) {
                    error = "昵称不能为空"
                    return@setOnClickListener
                }
            }
            birthdayField?.run {
                if (text.isNullOrBlank()) {
                    error = "出生日期用来计算年龄，请选择"
                    return@setOnClickListener
                }
            }
            ThreadUtils.executeIO {
                try {
                    FoxCore.database.memberDao().insert(condition)
                } catch (e: SQLiteConstraintException) {
                    LogUtil(AddMemberActivity::class.java.simpleName).setThrowable(e).e()
                    runOnUiThread {
                        nicknameField?.error = "昵称冲突，请尝试其它昵称"
                    }
                }
                runOnUiThread {
                    NoticeUtil.toast("添加成功! 欢迎新成员 ${condition.nickname}").showNow()
                    finish()
                }
            }
        }
    }
}