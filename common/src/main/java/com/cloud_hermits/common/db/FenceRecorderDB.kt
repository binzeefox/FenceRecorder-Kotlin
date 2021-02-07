package com.cloud_hermits.common.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cloud_hermits.common.db.tables.Match
import com.cloud_hermits.common.db.tables.MatchDao
import com.cloud_hermits.common.db.tables.Member
import com.cloud_hermits.common.db.tables.MemberDao

/**
 * 数据库
 *
 * @author tong.xw
 * 2021/02/01 11:25
 */
@Database(entities = [Match::class, Member::class], version = 3, exportSchema = false)
abstract class FenceRecorderDB: RoomDatabase() {
    abstract fun matchDao(): MatchDao
    abstract fun memberDao(): MemberDao
}