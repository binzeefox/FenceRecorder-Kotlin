package com.cloud_hermits.fencerecorder.db.tables

import androidx.room.*

/**
 * 对抗者
 *
 * @author tong.xw
 * 2021/02/01 14:10
 */
@Entity(indices = [Index(value = ["nickname"], unique = true)])
data class Member(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Int,

    // 名称ID 唯一
    var nickname: String,
    // 生日
    var birthday: Long,
    // 入库时间
    var dbTimestamp: Long,
    // 关联场次id   // 不保存场次，直接本地通过昵称查询
//    var matches: List<Long>,
    // 备注
    val comment: String?
)

/**
 * 成员操作
 */
@Dao
interface MemberDao {

    @Query("SELECT * FROM `Member` WHERE id = :id")
    fun query(id: Int): Member

    /**
     * 获取全部nickname
     */
    @Query("SELECT nickname FROM member")
    fun queryNickNames(): List<String>

    /**
     * 获取全部成员
     */
    @Query("SELECT * FROM `Member`")
    fun getAll(): List<Member>

    /**
     * 根据名称获取成员
     */
    @Query("SELECT * FROM `Member` WHERE nickname = :nickname")
    fun queryByName(nickname: String): Member

    /**
     * 更新
     */
    @Update(onConflict = OnConflictStrategy.ABORT, entity = Member::class)
    fun update(member: Member): Int

    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.ABORT, entity = Member::class)
    fun insertAll(vararg conditions: MemberCondition): List<Long>

    /**
     * 插入
     */
    @Insert(onConflict = OnConflictStrategy.ABORT, entity = Member::class)
    fun insert(condition: MemberCondition)

    @Delete(entity = Member::class)
    fun delete(member: Member)
}

/**
 * 条件
 */
data class MemberCondition(
    // 名称ID 唯一
    var nickname: String,
    // 生日
    var birthday: Long,
    // 入库时间
    var dbTimestamp: Long,
    // 关联场次id
//    var matches: List<Long> = emptyList(),
    // 备注
    val comment: String?
)
