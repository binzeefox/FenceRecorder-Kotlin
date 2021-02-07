package com.cloud_hermits.fencerecorder.cat

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.widget.*
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
import com.cloud_hermits.fencerecorder.utils.JxlUtils
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 人员详情页
 *
 * 导出个人Excel
 * @author tong.xw
 * 2021/02/05 14:32
 */
class MemberDetailActivity : BaseActivity() {

    companion object {
        private const val PARAMS_MEMBER_ID = "params_member_id"

        /**
         * 跳转
         */
        fun launch(context: Context, memberId: Long) {
            Launcher(context)
                .getActivityTarget(MemberDetailActivity::class.java)
                .intentInterceptor {
                    it.apply {
                        putExtra(PARAMS_MEMBER_ID, memberId)
                    }
                }.commit()
        }
    }

    private val memberDao: MemberDao get() = FoxCore.database.memberDao()
    private val data = PageData()

    private val nicknameField: TextView? get() = findViewById(R.id.nickname_field)
    private val totalCountField: TextView? get() = findViewById(R.id.total_count_field)
    private val winCountField: TextView? get() = findViewById(R.id.win_count_field)
    private val genderField: TextView? get() = findViewById(R.id.gender_field)
    private val birthdayField: TextView? get() = findViewById(R.id.birthday_field)
    private val dbDateField: TextView? get() = findViewById(R.id.db_date_field)
    private val commentField: TextView? get() = findViewById(R.id.comment_field)

    private var loaded = false
    private lateinit var member: Member
    private val listAdapter: ListAdapter
            by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { ListAdapter() }

    override fun getContentViewResource(): Int = R.layout.activity_member_detail

    override fun onCreate() {
        super.onCreate()

        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@MemberDetailActivity.title = "人员详情"
        }

        findViewById<ListView>(R.id.list_match)?.apply {
            adapter = listAdapter
            setOnItemClickListener(this@MemberDetailActivity::onItemClick)
        }

        findViewById<View>(R.id.fab_config)?.setOnClickListener {
            // 跳转修改页
            MemberConfigActivity.launch(this, member.id)
            loaded = false
        }
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_export -> {   // 导出
                ThreadUtils.executeIO {
                    var success = false
                    try {
                        JxlUtils.exportMemberExcelWithDetail(member)
                        success = true
                    } catch (e: Exception) {
                        LogUtil("MemberDetailActivity").setMessage("onOptionsItemSelected: ")
                            .setThrowable(e).e()
                    } finally {
                        runOnUiThread {
                            if (success)
                                NoticeUtil.toast("导出成功").showNow()
                            else
                                NoticeUtil.toast("导出失败").showNow()
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_single_export, menu)
        return true
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 初始化Member
     */
    private fun initData() {
        if (loaded) return
        intent.getLongExtra(PARAMS_MEMBER_ID, -1L).let {
            if (it == -1L) {
                finish()
                NoticeUtil.toast("信息异常").setDuration(Toast.LENGTH_LONG).showNow()
            } else {
                ThreadUtils.executeIO {
                    val genderMap: Map<Int, String> = mapOf(
                        Pair(GENDER_FEMALE, "女"),
                        Pair(GENDER_MALE, "男"),
                        Pair(GENDER_UNKNOWN, "未知"),
                        Pair(GENDER_OTHER, "其它")
                    )

                    // 初始化member
                    member = memberDao.query(it)
//                    mata()
                    data.matchList.apply {
                        clear()
                        addAll(memberDao.queryTotalMatch(member.nickname))
                    }
                    data.nickname = member.nickname
                    data.winCount = memberDao.queryWinByMember(member.nickname).size
                    data.totalCount = data.matchList.size
                    data.gender = genderMap[member.gender] ?: "未知"
                    data.birthday = SimpleDateFormat("生日：yyyy年MM月dd日", Locale.CHINA)
                        .format(member.birthday)
                    data.dbDate = SimpleDateFormat("入库日期：yyyy年MM月dd日  HH:mm:ss", Locale.CHINA)
                        .format(member.dbTimestamp)
                    data.comment = "备注：${member.comment}"
                    // 初始化列表
                    runOnUiThread {
                        listAdapter.notifyDataSetChanged()
                        loaded = true
                    }
                }
            }
        }
    }

    /**
     * 子项点击事件
     */
    private fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val match = data.matchList[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_match_detail, null)

        dialogView.findViewById<TextView>(R.id.period_field).text =
            SimpleDateFormat("用时 mm:ss", Locale.CHINA).format(match.period)
        dialogView.findViewById<TextView>(R.id.date_field).text =
            SimpleDateFormat("yyyy年MM月dd日-HH:mm:ss", Locale.CHINA).format(match.timestamp)
        dialogView.findViewById<TextView>(R.id.red_name_field).text = match.redName
        dialogView.findViewById<TextView>(R.id.blue_name_field).text = match.blueName
        dialogView.findViewById<TextView>(R.id.red_score_field).text = match.redScore.toString()
        dialogView.findViewById<TextView>(R.id.blue_score_field).text = match.blueScore.toString()
        val commentField = dialogView.findViewById<TextView>(R.id.comment_field).apply {
            text = match.comment
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .setPositiveButton("更新备注") { _, _ ->
                match.comment = commentField.text.toString()
                ThreadUtils.callIO {
                    FoxCore.database.matchDao().update(match)
                    runOnUiThread {
                        data.matchList[position] = match
                        listAdapter.notifyDataSetChanged()
                    }
                }
            }.show()
    }

    private fun mata() {
        member = Member(
            0,
            "狐彻",
            1,
            Date().time,
            Date().time,
            "狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁狐族万岁"
        )
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 内部数据类
     */
    @SuppressLint("SetTextI18n")
    private inner class PageData {
        val matchList = ArrayList<Match>()

        var nickname: String = ""
            set(value) {
                runOnUiThread {
                    nicknameField?.text = value
                    this@MemberDetailActivity.title = value
                }
                field = value
            }

        var winCount: Int = 0
            set(value) {
                runOnUiThread {
                    winCountField?.text = "获胜场次：$value"
                }
                field = value
            }

        var totalCount: Int = 0
            set(value) {
                runOnUiThread {
                    totalCountField?.text = "总场次：$value"
                }
                field = value
            }

        var gender: String = "未知"
            set(value) {
                runOnUiThread {
                    genderField?.text = value
                }
                field = value
            }

        var birthday: String = ""
            set(value) {
                runOnUiThread {
                    if (value.isBlank()) birthdayField?.visibility = View.GONE
                    else birthdayField?.visibility = View.VISIBLE
                    birthdayField?.text = value
                }
                field = value
            }

        var dbDate: String = ""
            set(value) {
                runOnUiThread {
                    if (value.isBlank()) dbDateField?.visibility = View.GONE
                    else dbDateField?.visibility = View.VISIBLE
                    dbDateField?.text = value
                }
                field = value
            }

        var comment: String? = null
            set(value) {
                runOnUiThread {
                    if (value.isNullOrBlank()) commentField?.visibility = View.GONE
                    else commentField?.visibility = View.VISIBLE
                    commentField?.text = value
                }
                field = value
            }
    }

    /**
     * 适配器
     */
    private inner class ListAdapter : BaseAdapter() {
        override fun getCount(): Int = data.matchList.size

        override fun getItem(position: Int): Any = data.matchList[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            convertView?.run {
                convertView(position, this)
                return this
            } ?: let {
                val view =
                    layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)
                convertView(position, view)
                return view
            }
        }

        private fun convertView(position: Int, view: View) {
            val match = data.matchList[position]
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            val title = String.format(
                Locale.CHINA, "%s vs %s 比分 %d : %d",
                match.redName, match.blueName, match.redScore, match.blueScore
            )
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(Date(match.timestamp))

            text1.text = title
            text2.text = date
        }
    }
}