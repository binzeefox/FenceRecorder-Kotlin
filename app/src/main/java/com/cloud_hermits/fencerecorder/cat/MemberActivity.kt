package com.cloud_hermits.fencerecorder.cat

import android.content.Context
import com.binzeefox.foxdevframe_kotlin.ui.utils.launcher.Launcher
import com.cloud_hermits.common.BaseActivity

/**
 * TODO 成员详情
 *
 * - 暂时不能修改昵称
 * - 修改生日
 * - 修改性别
 * - 修改备注
 * @author tong.xw
 * 2021/02/03 10:08
 */
class MemberActivity: BaseActivity() {
    companion object {
        private const val PARAMS_MEMBER_ID = "params_member_id"

        /**
         * 跳转
         */
        fun launch(from: Context, id: Long) {
            Launcher(from).getActivityTarget(MemberActivity::class.java).intentInterceptor {
                it.apply { putExtra(PARAMS_MEMBER_ID, id) }
            }.commit()
        }
    }
}