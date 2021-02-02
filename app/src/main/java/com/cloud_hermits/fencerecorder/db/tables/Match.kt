package com.cloud_hermits.fencerecorder.db.tables

import androidx.room.*
import java.util.*

/**
 * 对抗表
 *
 * @author tong.xw
 * 2021/02/01 11:26
 */
@Entity(indices = [Index(value = ["timestamp"], unique = true)])
data class Match(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") var id: Long,

    // 时间戳 唯一
    var timestamp: Long = Date().time,
    // 用时
    var period: Long = 0,
    // 红方名称
    var redName: String,
    // 蓝方名称
    var blueName: String,
    // 红方分数
    var redScore: Int,
    // 蓝方分数
    var blueScore: Int,
    // 备注
    var comment: String? = null
)

/**
 * 对抗表操作
 */
@Dao
interface MatchDao {

    @Query("SELECT * FROM `Match` WHERE id = :id")
    fun query(id: Long): Match

    /**
     * 获取全部
     */
    @Query("SELECT * FROM `match`")
    fun getAll(): List<Match>

    /**
     * 搜索时间范围内的全部值
     *
     * @param timestampRange [开始时间, 结束时间] 单位毫秒
     */
    @Query("SELECT * FROM `Match` WHERE timestamp IN (:timestampRange)")
    fun queryByTimeRange(timestampRange: IntArray): Match

    /**
     * 通过时间段查询
     */
    @Query("SELECT * FROM `match` WHERE timestamp >= :start AND timestamp <= :end")
    fun queryAllByTimeRange(start: Long, end: Long): Match

    /**
     * 更新
     */
    @Update(onConflict = OnConflictStrategy.REPLACE, entity = Match::class)
    fun update(match: Match): Int

    /**
     * 批量添加
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Match::class)
    fun insertAll(vararg conditions: MatchCondition): List<Long>

    /**
     * 添加
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE, entity = Match::class)
    fun insert(condition: MatchCondition)

    /**
     * 删除
     */
    @Delete(entity = Match::class)
    fun delete(match: Match)
}

/**
 * 挑选条件
 */
data class MatchCondition(
    // 时间戳 唯一
    var timestamp: Long = Date().time,
    // 用时
    var period: Long = 0,
    // 红方名称
    var redName: String,
    // 蓝方名称
    var blueName: String,
    // 红方分数
    var redScore: Int,
    // 蓝方分数
    var blueScore: Int,
    // 备注
    var comment: String? = null
)