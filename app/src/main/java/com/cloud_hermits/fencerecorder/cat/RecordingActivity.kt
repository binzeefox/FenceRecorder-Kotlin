package com.cloud_hermits.fencerecorder.cat

import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.appcompat.widget.Toolbar
import androidx.core.text.isDigitsOnly
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.binzeefox.foxdevframe_kotlin.ui.utils.NoticeUtil
import com.binzeefox.foxdevframe_kotlin.utils.ThreadUtils
import com.cloud_hermits.common.BaseActivity
import com.cloud_hermits.fencerecorder.MyApplication.Companion.database
import com.cloud_hermits.fencerecorder.R
import com.cloud_hermits.fencerecorder.db.tables.MatchCondition
import java.text.SimpleDateFormat
import java.util.*

/**
 * 记录页
 *
 * - 开始计时后比赛开始，此时无法更改双方选手名称
 * - 为防止错误操作，只有计时器暂停时才能修改分数
 * - 计时暂停的状态下，为防止错误操作，需要2秒内两次点击返回键才能终止比赛
 * - 比赛终止后，自动保存数据到数据库，同时若绑定了传感器设备，则一同保存传感器数据。然后尝试上传至服务器。再次点击返回键则返回列表
 * @author tong.xw
 * 2021/02/02 10:09
 */
class RecordingActivity: BaseActivity() {
    // 当前状态
    private var state: State = IdleState()

    // 计时区
    private val timerField: TextView? get() = findViewById(R.id.timer_field)
    // 红方名称
    private val redNameField: AppCompatAutoCompleteTextView? get() = findViewById(R.id.red_side_field)
    // 蓝方名称
    private val blueNameField: AppCompatAutoCompleteTextView? get() = findViewById(R.id.blue_side_field)
    // 红方分数
    private val redScoreField: TextView? get() = findViewById(R.id.red_score_field)
    // 蓝方分数
    private val blueScoreField: TextView? get() = findViewById(R.id.blue_score_field)
    // 大悬浮按钮
    private val fabIcon: ImageView? get() = findViewById(R.id.fab_icon)
    // 红方
    private val redPlayer = Player("红方", 0)
    // 蓝方
    private val bluePlayer = Player("蓝方", 0)

    // 计时器
    private var timer: Timer? = null

    private data class Player(
        var nickname: String,
        var score: Int
    )

    /**
     * 状态
     */
    private interface State {
        fun onPressAction() //点击动作键
        fun onChangeScore(player: Player, score: Int)   //改变分数
        fun onBackPressed()
    }

    override fun getContentViewResource(): Int = R.layout.activity_recording

    override fun onCreate() {
        super.onCreate()
        findViewById<Toolbar>(R.id.toolbar)?.apply {
            setSupportActionBar(this)
            this@RecordingActivity.title = ""
        }

        ThreadUtils.executeIO {
            val list = getNameList()
            arrayOf(redNameField, blueNameField).forEach {
                runOnUiThread {
                    it?.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, list))
                }
            }
        }

        fabIcon?.setOnClickListener { state.onPressAction() }
        findViewById<View>(R.id.btn_plus_red).setOnClickListener{state.onChangeScore(redPlayer, 1)}
        findViewById<View>(R.id.btn_plus_blue).setOnClickListener{state.onChangeScore(bluePlayer, 1)}
        findViewById<View>(R.id.btn_sub_red).setOnClickListener{state.onChangeScore(redPlayer, -1)}
        findViewById<View>(R.id.btn_sub_blue).setOnClickListener{state.onChangeScore(bluePlayer, -1)}

        timerField?.text = longToTimeString(MatchConfig.matchPeriod)
        timerField?.setOnClickListener {
            val layout: View =
                layoutInflater.inflate(R.layout.dialog_main_period_setter, null, false)
            val minField = layout.findViewById<TextView>(R.id.period_minute)
            val secField = layout.findViewById<TextView>(R.id.period_second)
            val time = longToTimeString(timer?.maxTime?:MatchConfig.matchPeriod)
            val split = time.split(":")
            minField?.text = split[0]
            secField?.text = split[1]

            AlertDialog.Builder(this)
                .setTitle("请输入自定义时长")
                .setCancelable(true)
                .setView(layout)
                .setPositiveButton("确定") { _, _ ->

                    arrayOf(minField, secField).forEach {
                        if (it?.text.isNullOrBlank()) {
                            it?.text = 0.toString()
                            return@setPositiveButton
                        }
                        if (it?.text?.isDigitsOnly() != false) {
                            it?.error = "请输入自然数"
                        }
                    }

                    val min = minField?.text.toString().toLong()
                    val sec = secField?.text.toString().toLong()

                    val period = min * 60 * 1000 + sec * 1000
                    timerField?.text = longToTimeString(period)
                }.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.about -> {
                if (state !is RecordingState) {
                    AlertDialog.Builder(this)
                        .setTitle("使用说明")
                        .setCancelable(true)
                        .setMessage(getIntro())
                        .show()
                } else NoticeUtil.toast("计时状态屏蔽按键").showNow()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu?.findItem(R.id.match_settings)?.isVisible = false
        menu?.findItem(R.id.member_settings)?.isVisible = false
        menu?.findItem(R.id.about)?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onBackPressed() = state.onBackPressed()

    ///////////////////////////////////////////////////////////////////////////
    // 私有方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 开始计时
     */
    private fun startTiming() {
        timer?: run {
            timer = Timer(timeStringToLong(timerField?.text.toString()))
        }
        timer?.pausing = false
    }

    /**
     * 暂停计时
     */
    private fun pauseTiming() {
        timer?.pausing = true
    }

    /**
     * 修改时间区
     */
    private fun changeTimerField(remainTime: Long) {
        timerField?.text = longToTimeString(remainTime)
    }

    /**
     * 通过Long获取时间值
     */
    private fun longToTimeString(time: Long) =
        SimpleDateFormat("mm:ss", Locale.getDefault()).format(time)

    private fun getNameList(): List<String> {
        return FoxCore.database.memberDao().queryNickNames()
    }

    /**
     * 通过时间字符串获取Long
     */
    private fun timeStringToLong(time: String): Long {
        val split = time.split(":")
        val minute = split[0].toLong()
        val second = split[1].toLong()
        return minute * 60 * 1000 + second * 1000
    }

    /**
     * 本地化记录并退出
     */
    private fun localizeAndFinish() {
        ThreadUtils.executeIO {
            var period: Long = 0
            timer?.let {
                period = it.maxTime - it.remainTime
            }
            MatchCondition(
                period = period,
                redName = redPlayer.nickname,
                blueName = bluePlayer.nickname,
                redScore = redPlayer.score,
                blueScore = bluePlayer.score
            ).let {
                FoxCore.database.matchDao().insert(it)
            }
            runOnUiThread {
                finish()
            }
        }
    }

    /**
     * 父类返回键
     */
    private fun superOnBackPressed() = super.onBackPressed()

    /**
     * 获取使用说明
     */
    private fun getIntro(): String {
        return """
             1. 输入双方选手姓名
             2. （若需要）点击比赛时长修改比赛时长
             3. 点击开始按钮进行计时
             4. 若出现得分，暂停计时并修改分数
             5. 时间到，裁判修改最终分数并按返回键退出
             6. 若提前结束，需先暂停计时，并按两次返回键结束比赛，按第三次返回键退出
             
             P.S. 
             为防止误触，计时状态下将屏蔽除暂停外所有操作。如需修改分数，请先暂停计时。
             比赛开始后将不能修改双方选手名称和比赛时长。
             比赛结束后可在列表中添加备注。
             """.trimIndent()
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部类
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 计时器
     */
    private inner class Timer(var remainTime: Long) {
        val maxTime = remainTime
        var pausing = true

        init {
            ThreadUtils.executeComputation {
                while (remainTime > 0) {
                    if (pausing) continue
                    remainTime -= 1000
                    changeTimerField(remainTime)
                    Thread.sleep(1000)
                }
                // 时间到
                runOnUiThread {
                    state = EndState()
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    VibrationEffect.createOneShot(
                        3000, VibrationEffect.DEFAULT_AMPLITUDE //振幅
                    ) .let {
                        (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(it)
                    }
                } else {
                    (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(longArrayOf(3000), -1)
                }
            }
        }
    }

    /**
     * 等待状态
     */
    private inner class IdleState: State {

        override fun onPressAction() {
            state = RecordingState()
        }

        override fun onChangeScore(player: Player, score: Int) {
            NoticeUtil.toast("比赛尚未开始").showNow()
        }

        override fun onBackPressed() {
            superOnBackPressed()
        }
    }

    /**
     * 记录状态
     */
    private inner class RecordingState: State {

        init {
            changeUI()
            startTiming()
        }

        private fun changeUI() {
            arrayOf(redNameField, blueNameField).forEach {
                it?.isEnabled = false
                it?.clearFocus()
                if (it?.text.isNullOrBlank()) it?.setText(it.hint)
            }

            redPlayer.nickname = redNameField?.text.toString()
            bluePlayer.nickname = blueNameField?.text.toString()
            timerField?.isEnabled = false
            fabIcon?.setImageResource(R.drawable.ic_pause)
        }

        override fun onPressAction() {
            state = PausingState()
        }

        override fun onChangeScore(player: Player, score: Int) {
            NoticeUtil.toast("为防误触，计时状态下禁止修改分数").showNow()
        }

        override fun onBackPressed() {
            NoticeUtil.toast("为防误触，计时状态下屏蔽返回键").showNow()
        }
    }

    /**
     * 暂停状态
     */
    private inner class PausingState: State {
        private var backFlag = false

        init {
            pauseTiming()
            fabIcon?.setImageResource(R.drawable.ic_play)
        }

        override fun onPressAction() {
            state = RecordingState()
        }

        override fun onChangeScore(player: Player, score: Int) {
            player.score += score
            if (player == redPlayer) redScoreField?.text = player.score.toString()
            if (player == bluePlayer) blueScoreField?.text = player.score.toString()
        }

        override fun onBackPressed() {
            if (backFlag) {
                state = EndState()
            } else {
                NoticeUtil.toast("再次点击返回键结束比赛").showNow()
                backFlag = true
                Handler(Looper.getMainLooper()).postDelayed({
                    backFlag = false
                }, 2000)
            }
        }
    }

    /**
     * 结束状态
     */
    private inner class EndState: State {

        init {
            pauseTiming()
            fabIcon?.setImageResource(R.drawable.ic_complete)
            fabIcon?.isEnabled = false
            NoticeUtil.toast("比赛结束，点击返回键保存并返回列表")
        }

        override fun onPressAction() {
            // do nothing...
        }

        override fun onChangeScore(player: Player, score: Int) {
            player.score += score
            if (player == redPlayer) redScoreField?.text = player.score.toString()
            if (player == bluePlayer) blueScoreField?.text = player.score.toString()
        }

        override fun onBackPressed() {
            localizeAndFinish()
        }
    }
}