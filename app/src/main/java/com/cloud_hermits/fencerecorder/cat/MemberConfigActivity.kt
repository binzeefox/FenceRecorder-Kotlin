package com.cloud_hermits.fencerecorder.cat

import android.app.DatePickerDialog
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.ui.utils.launcher.Launcher
import com.binzeefox.foxdevframe_kotlin.utils.LogUtil
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.common.BaseApplication.Companion.database
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.common.db.tables.*
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加成员
 *
 * @author tong.xw
 * 2021/02/02 17:12
 */
class MemberConfigActivity : BaseActivity() {
    companion object {
        private const val PARAMS_MEMBER_ID = "params_member_id"

        fun launch(ctx: Context, memberId: Long = -1) {
            val launcher = Launcher(ctx).getActivityTarget(MemberConfigActivity::class.java)
            launcher.intentInterceptor {
                it.apply { putExtra(PARAMS_MEMBER_ID, memberId) }
            }
            launcher.commit()
        }
    }

    private val genderMap: Map<String, Int> = mapOf(
        Pair("女", GENDER_FEMALE),
        Pair("男", GENDER_MALE),
        Pair("未知", GENDER_UNKNOWN),
        Pair("其它", GENDER_OTHER)
    )

    private val genderEntry: Map<Int, String> = mapOf(
        Pair(GENDER_FEMALE, "女"),
        Pair(GENDER_MALE, "男"),
        Pair(GENDER_UNKNOWN, "未知"),
        Pair(GENDER_OTHER, "其它")
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

    private var member: Member? = null

    override fun getContentViewResource(): Int = R.layout.activity_member_add

    override fun onCreate() {
        super.onCreate()
        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@MemberConfigActivity.title = "添加成员"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        arrayOf(birthdayField, genderField).forEach {
            it?.isFocusable = false
        }

        checkEdit()

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
                    if (member == null)     // 新增
                        FoxCore.database.memberDao().insert(condition)
                    else {  // 修改
                        member?.apply {
                            nickname = condition.nickname
                            birthday = condition.birthday
                            gender = condition.gender
                            comment = condition.comment
                            FoxCore.database.memberDao().update(this)
                        }
                    }
                } catch (e: SQLiteConstraintException) {
                    LogUtil(MemberConfigActivity::class.java.simpleName).setThrowable(e).e()
                    runOnUiThread {
                        nicknameField?.error = "昵称冲突，请尝试其它昵称"
                    }
                }
                runOnUiThread {
                    val text =
                        if (member == null) "添加成功! 欢迎新成员 ${condition.nickname}"
                        else "修改成功!"
                    NoticeUtil.toast(text).showNow()
                    finish()
                }
            }
        }
    }

    /**
     * 检查新增还是修改
     */
    private fun checkEdit() {
        val id = intent.getLongExtra(PARAMS_MEMBER_ID, -1)
        if (id == -1L) return

        // id不为空，是修改项
        ThreadUtils.executeIO {
            member = FoxCore.database.memberDao().query(id).also {
                condition.apply {
                    nickname = it.nickname
                    birthday = it.birthday
                    gender = it.gender
                    comment = it.comment
                }
                this@MemberConfigActivity.title = "编辑成员 ${it.nickname}"

                val birthdayStr = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
                    .format(it.birthday)
                ThreadUtils.runOnUiThread {
                    nicknameField?.setText(it.nickname)
                    birthdayField?.setText(birthdayStr)
                    genderField?.setText(genderEntry[it.gender])
                    commentField?.setText(it.comment)
                }
            }
        }
    }
}