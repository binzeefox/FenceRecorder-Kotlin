package com.cloud_hermits.fencerecorder.cat

import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.ui.utils.launcher.Launcher
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.MyApplication.Companion.database
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.fencerecorder.db.tables.Match
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 对战列表页
 *
 * - TODO 搜索框
 * @author tong.xw
 * 2021/02/01 16:37
 */
class MatchListActivity : BaseActivity() {
    private val data = ArrayList<Match>()   // 对战列表
    private val adapter = ListAdapter() // 列表适配器
    private var backFlag = false    // 返回键点击标志

    override fun getContentViewResource(): Int = R.layout.activity_match_list

    override fun onCreate() {
        super.onCreate()
        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
        }
        findViewById<ListView>(R.id.list_match).apply {
            adapter = this@MatchListActivity.adapter
            setOnItemClickListener(this@MatchListActivity::onItemClick)
            setOnItemLongClickListener(this@MatchListActivity::onHoldItem)
        }
        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener(this::onFabClick)
//        Beta.checkUpgrade(false, false) // 检查更新
    }

    override fun onResume() {
        super.onResume()
        // 获取数据
        ThreadUtils.callIO {
            val tempList = FoxCore.database.matchDao().getAll()
            runOnUiThread {
                data.clear()
                data.addAll(tempList)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onBackPressed() {
        if (backFlag) super.onBackPressed()
        else {
            NoticeUtil.toast("再次点击返回键退出App").showNow()
            backFlag = true
            Handler(Looper.getMainLooper()).postDelayed({
                backFlag = false
            }, 2000)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.member_settings -> {   // 人员设置
                TODO("跳转至人员设置页")
            }
            R.id.match_settings -> {   // 比赛设置
                Launcher(this).getActivityTarget(MatchConfigActivity::class.java).commit()
                true
            }
            R.id.about -> { // 关于页，宣传页
                Launcher(this).getActivityTarget(AboutActivity::class.java).commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 子项点击事件
     */
    private fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val match = data[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_match_detail, null)

        dialogView.findViewById<TextView>(R.id.period_field).text =
            SimpleDateFormat("用时 mm:ss", Locale.CHINA).format(match.period)
        dialogView.findViewById<TextView>(R.id.date_field).text =
            SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA).format(match.timestamp)
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
                        data[position] = match
                        adapter.notifyDataSetChanged()
                    }
                }
            }.show()
    }

    /**
     * 子项长安事件
     */
    private fun onHoldItem(
        adapterView: AdapterView<*>,
        view: View,
        position: Int,
        id: Long
    ): Boolean {
        val menu = PopupMenu(this, view, Gravity.END or Gravity.TOP)
        menu.menu.add(R.id.menu_group_list_popup, R.id.menu_delete, 0, "删除")
        menu.setOnMenuItemClickListener {
            onDeleteItem(position)
            menu.dismiss()
            true
        }
        menu.show()
        return true
    }

    /**
     * 悬浮按钮点击事件
     */
    private fun onFabClick(view: View) {
        Launcher(this).getActivityTarget(RecordingActivity::class.java).commit()
    }

    private fun onDeleteItem(position: Int) {
        val match = data[position]
        ThreadUtils.callIO {
            FoxCore.database.matchDao().delete(match)
            runOnUiThread {
                data.remove(match)
                adapter.notifyDataSetChanged()
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 适配器
     */
    private inner class ListAdapter : BaseAdapter() {
        override fun getCount(): Int = data.size

        override fun getItem(position: Int): Any = data[position]

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
            val match = data[position]
            val text1 = view.findViewById<TextView>(android.R.id.text1)
            val text2 = view.findViewById<TextView>(android.R.id.text2)

            val title = String.format(
                Locale.CHINA, "%s vs %s 比分 %d : %d",
                match.redName, match.blueName, match.redScore, match.blueScore
            )
            val date = SimpleDateFormat("yyyy-MM-dd HH:mmss", Locale.CHINA)
                .format(Date(match.timestamp))

            text1.text = title
            text2.text = date
        }
    }
}