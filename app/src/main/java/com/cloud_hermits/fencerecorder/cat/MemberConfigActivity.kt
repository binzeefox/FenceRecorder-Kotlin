package com.cloud_hermits.fencerecorder.cat

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.launcher.Launcher
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.MyApplication.Companion.database
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.fencerecorder.db.tables.Member
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 人员管理Activity
 *
 * @author tong.xw
 * 2021/02/02 15:01
 */
class MemberConfigActivity : BaseActivity() {
    private val memberListView: RecyclerView? get() = findViewById(R.id.list_member)
    private val memberList: ArrayList<Member> = ArrayList()
    private val listAdapter: ListAdapter by lazy { ListAdapter() }
    private val genderMap: Map<Int, String> = mapOf(
        Pair(0, "女"),
        Pair(1, "男"),
        Pair(9, "未知"),
        Pair(8, "其它")
    )

    override fun getContentViewResource(): Int = R.layout.activity_member_config

    override fun onCreate() {
        super.onCreate()
        findViewById<Toolbar>(R.id.toolbar).apply {
            setSupportActionBar(this)
            this@MemberConfigActivity.title = "成员设置"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        memberListView?.apply {
            layoutManager = LinearLayoutManager(this@MemberConfigActivity, LinearLayoutManager.VERTICAL, false)
            adapter = listAdapter
        }
        findViewById<View>(R.id.fab_add).setOnClickListener {
            Launcher(this).getActivityTarget(AddMemberActivity::class.java).commit()
            listAdapter.isRequested = false
        }
    }

    override fun onResume() {
        super.onResume()
        listAdapter.request()
    }

    override fun onDestroy() {
        super.onDestroy()
        listAdapter.isRequested = false
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 列表适配器
     */
    private inner class ListAdapter : RecyclerView.Adapter<ViewHolder>() {
        private val renderPool: ExecutorService = Executors.newSingleThreadExecutor()
        @Volatile var isRequested = false

        fun request(){
            if (isRequested) return
            ThreadUtils.executeIO {
                memberList.clear()
                memberList.addAll(FoxCore.database.memberDao().getAll())
                runOnUiThread {
                    this.notifyItemRangeChanged(0, memberList.size)
                    isRequested = true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val item = layoutInflater.inflate(R.layout.item_member_card, parent, false)
            return ViewHolder(item)
        }

        override fun getItemId(position: Int): Long {
            return memberList[position].id
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.apply {
                renderPool.execute {
                    val member = memberList[position]
                    val name = member.nickname
                    val gender = genderMap[member.gender]
                    val total = FoxCore.database.memberDao().queryTotalMatch(name).size
                    val win = FoxCore.database.memberDao().queryWinByMember(name).size
                    runOnUiThread {
                        nickNameField?.text = name
                        genderField?.text = "性别: $gender"
                        totalCountField?.text = "总场次: $total"
                        winCountField?.text = "获胜场次: $win"
                        itemView.setOnClickListener {
//                            TODO("卡片点击事件")
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int = memberList.size
    }

}

/**
 * 容器
 */
private class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
    val nickNameField: TextView? get() = view.findViewById(R.id.nickname_field)
    val genderField: TextView? get() = view.findViewById(R.id.gender_field)
    val totalCountField: TextView? get() = view.findViewById(R.id.total_count_field)
    val winCountField: TextView? get() = view.findViewById(R.id.win_count_field)
}

