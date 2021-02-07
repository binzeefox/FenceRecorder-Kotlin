package com.cloud_hermits.common

import android.app.Application
import androidx.room.Room
import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.cloud_hermits.common.db.FenceRecorderDB

//import com.tencent.bugly.Bugly

/**
 * App基类
 *
 * @author tong.xw
 * 2021/02/01 10:59
 */
abstract class BaseApplication: Application() {

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
//        Bugly.init(this, BuildConfig.BUGLY_ID, BuildConfig.DEBUG)
    }
}