package com.cloud_hermits.fencerecorder.db.tables

import androidx.room.*
import java.util.*

const val GENDER_MALE = 1   // 性别男
const val GENDER_FEMALE = 0 // 性别女
const val GENDER_UNKNOWN = 9    // 未知性别
const val GENDER_OTHER = 8  // 其它性别

/**
 * 对抗者
 *
 * @author tong.xw
 * 2021/02/01 14:10
 */
@Entity(indices = [Index(value = ["nickname"], unique = true)])
data class Member(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long,

    // 名称ID 唯一
    var nickname: String,
    var gender: Int,
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

    /**
     * 通过昵称获取比赛场次ID
     */
    @Query("SELECT id FROM `match` WHERE redName = :nickname OR blueName = :nickname")
    fun queryTotalMatch(nickname: String): List<Long>

    /**
     * 通过昵称获取胜场ID
     */
    @Query("SELECT id From `match` WHERE (redName = :nickname AND redScore > blueScore) OR (blueName = :nickname AND blueScore > redScore)")
    fun queryWinByMember(nickname: String): List<Long>

    /**
     * 通过昵称获取败场ID
     */
    @Query("SELECT id From `match` WHERE (redName = :nickname AND redScore <= blueScore) OR (blueName = :nickname AND blueScore <= redScore)")
    fun queryLoseByMember(nickname: String): List<Long>

    @Query("SELECT * FROM `Member` WHERE id = :id")
    fun query(id: Long): Member

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
    // 性别
    var gender: Int = GENDER_UNKNOWN,
    // 生日
    var birthday: Long,
    // 入库时间
    var dbTimestamp: Long = Date().time,
    // 关联场次id
//    var matches: List<Long> = emptyList(),
    // 备注
    var comment: String?
)
