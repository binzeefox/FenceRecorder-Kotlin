package com.cloud_hermits.fencerecorder

import androidx.room.Room
import androidx.room.migration.Migration
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.cloud_hermits.common.BaseApplication
import com.cloud_hermits.common.BuildConfig
import com.cloud_hermits.fencerecorder.db.FenceRecorderDB
import com.tencent.bugly.Bugly

/**
 * App
 *
 * @author tong.xw
 * 2021/02/01 15:13
 */
class MyApplication : BaseApplication() {
    companion object {
        private const val DB_NAME = "FenceRecorderDB"

        /**
         * 数据库, 拓展到FoxCore
         */
        val FoxCore.database: FenceRecorderDB by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            Room.databaseBuilder(FoxCore.appContext, FenceRecorderDB::class.java, DB_NAME)
                .build()
        }

        /**
         * 清除所有表
         */
        fun FoxCore.clearTables() {
            database.clearAllTables()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Bugly.init(this, BuildConfig.BUGLY_ID, BuildConfig.DEBUG)
    }
}