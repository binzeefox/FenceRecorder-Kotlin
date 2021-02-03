package com.cloud_hermits.fencerecorder.utils

import com.binzeefox.foxdevframe_kotlin.FoxCore
import com.cloud_hermits.fencerecorder.MyApplication.Companion.database
import com.cloud_hermits.fencerecorder.db.tables.Match
import com.cloud_hermits.fencerecorder.db.tables.Member
import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Excel工具
 *
 * @author tong.xw
 * 2021/02/01 15:27
 */
object JxlUtils {

    // 缓存文件夹
    val cacheDir = File(FoxCore.appContext.externalCacheDir, "excels").apply {
        if (!exists()) mkdir()
        if (!isDirectory) {
            delete()
            mkdir()
        }
    }

    // 用于文件名的当前时间
    private val currentTimeForFile: String
        get() = SimpleDateFormat("yyyy-MM-dd_HH-MM-SS", Locale.CHINA).format(Date().time)

    /**
     * 导出全部数据
     */
    fun exportFullExcel() {
        val excel = File(cacheDir, "兵击计分器记录_${currentTimeForFile}.xls").apply {
            if (exists()) delete()
            createNewFile()
        }
        Workbook.createWorkbook(excel).let { workbook ->
            try {
                var index = 0
                createMatchSheet(index++, workbook)
                createMemberSheet(index++, workbook)
                val memberList = FoxCore.database.memberDao().getAll()
                for (member in memberList)
                    createMemberSheetWithDetail(index++, member, workbook)
                workbook.write()
            } finally {
                workbook.close()
            }
        }
    }

    /**
     * 导出Excel
     */
    fun exportMatchExcel() {
        val excel = File(cacheDir, "对战记录_${currentTimeForFile}.xls").apply {
            if (exists()) delete()
            createNewFile()
        }
        Workbook.createWorkbook(excel).let { workbook ->
            try {
                createMatchSheet(0, workbook)
                workbook.write()
            } finally {
                workbook.close()
            }
        }
    }

    /**
     * 导出人员表
     */
    fun exportMemberExcel() {
        val excel = File(cacheDir, "人员表_${currentTimeForFile}.xls").apply {
            if (exists()) delete()
            createNewFile()
        }
        Workbook.createWorkbook(excel).let { workbook ->
            try {
                createMemberSheet(0, workbook)
                workbook.write()
            } finally {
                workbook.close()
            }
        }
    }

    /**
     * 导出人员表（带场次详情）
     *
     * @param members 需要导出的人员
     */
    fun exportMemberExcelWithDetail(vararg members: Member) {
        val fileName = if (members.size == 1) {
            "${members[0].nickname}对战记录_${currentTimeForFile}.xls"
        } else {
            "人员对战记录_${currentTimeForFile}.xls"
        }

        val excel = File(cacheDir, fileName).apply {
            if (exists()) delete()
            createNewFile()
        }

        Workbook.createWorkbook(excel).let { workbook ->
            try {
                for ((index, member) in members.withIndex()) {
                    createMemberSheetWithDetail(index, member, workbook)
                }
                workbook.write()
            } finally {
                workbook.close()
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // 内部方法
    ///////////////////////////////////////////////////////////////////////////

    /**
     * 创建人员表
     */
    private fun createMemberSheet(index: Int, workbook: WritableWorkbook) {
        val sheet = workbook.createSheet("人员表", index)

        val memberList = FoxCore.database.memberDao().getAll()
        val memberHeads = arrayOf("昵称", "性别", "生日", "入库时间", "总场次", "胜场场次", "败场场次", "备注")

        var row = 0
        // 第一行表头
        for ((i, head) in memberHeads.withIndex()) {
            sheet.addCell(Label(i, row, head))
        }
        for (member in memberList) {
            row++
            val winCount = FoxCore.database.memberDao().queryWinByMember(member.nickname).size
            val loseCount = FoxCore.database.memberDao().queryLoseByMember(member.nickname).size
            createMemberRow(row, member, winCount, loseCount, sheet)
        }
    }

    /**
     * 创建人员表（带场次详情）
     *
     * @param index 表内序号
     * @param member 成员
     */
    private fun createMemberSheetWithDetail(
        index: Int,
        member: Member,
        workbook: WritableWorkbook
    ) {
        val sheet = workbook.createSheet("${member.nickname}表", index)
        val winMatchIdList = FoxCore.database.memberDao().queryWinByMember(member.nickname)
        val loseMatchIdList = FoxCore.database.memberDao().queryLoseByMember(member.nickname)

        val memberHeads = arrayOf("昵称", "性别", "生日", "入库时间", "总场次", "胜场场次", "败场场次", "备注")
        val matchHeads = arrayOf("日期", "用时(s)", "红方昵称", "蓝方昵称", "红方得分", "蓝方得分", "备注")

        var row = 0 //行计数器

        // 第一行表头
        for ((i, head) in memberHeads.withIndex()) {
            sheet.addCell(Label(i, row, head))
        }
        row++
        createMemberRow(row, member, winMatchIdList.size, loseMatchIdList.size, sheet)

        // 胜场
        row++
        sheet.addCell(Label(0, row, "胜场统计"))
        row++
        for ((i, head) in matchHeads.withIndex()) {
            sheet.addCell(Label(i, row, head))
        }
        for (matchId in winMatchIdList) {
            row++
            val match = FoxCore.database.matchDao().query(matchId)
            createMatchRow(row, match, sheet)
        }
        // 败场
        row++
        sheet.addCell(Label(0, row, "败场统计"))
        row++
        for ((i, head) in matchHeads.withIndex()) {
            sheet.addCell(Label(i, row, head))
        }
        for (matchId in loseMatchIdList) {
            row++
            val match = FoxCore.database.matchDao().query(matchId)
            createMatchRow(row, match, sheet)
        }
    }

    /**
     * 创建对战总表
     *
     * @param index 表内序号
     */
    private fun createMatchSheet(index: Int, workbook: WritableWorkbook) {
        val sheet = workbook.createSheet("对战表", index)
        val matchList = FoxCore.database.matchDao().getAll()
        val heads = arrayOf("日期", "用时(s)", "红方昵称", "蓝方昵称", "红方得分", "蓝方得分", "备注")

        // 第一行表头
        var row = 0
        for ((i, head) in heads.withIndex()) {
            sheet.addCell(Label(i, row, head))
        }
        for (match in matchList) {
            row++
            createMatchRow(row, match, sheet)
        }
    }

    /**
     * 战局行
     *
     * "日期", "用时(s)", "红方昵称", "蓝方昵称", "红方得分", "蓝方得分", "备注"
     */
    private fun createMatchRow(row: Int, match: Match, sheet: WritableSheet) {
        val date =
            SimpleDateFormat("yyyy年MM月dd日\nHH:mm:ss", Locale.CHINA).format(match.timestamp)
        var col = 0
        sheet.addCell(Label(col, row, date))
        col++
        sheet.addCell(Label(col, row, "${match.period / 1000}"))
        col++
        sheet.addCell(Label(col, row, match.redName))
        col++
        sheet.addCell(Label(col, row, match.blueName))
        col++
        sheet.addCell(Label(col, row, "${match.redScore}"))
        col++
        sheet.addCell(Label(col, row, "${match.blueScore}"))
        col++
        sheet.addCell(Label(col, row, match.comment))
    }

    /**
     * 人物行
     *
     * "昵称", "性别", "生日", "入库时间", "总场次", "胜场场次", "败场场次", "备注"
     */
    private fun createMemberRow(
        row: Int,
        member: Member,
        winCount: Int,
        loseCount: Int,
        sheet: WritableSheet
    ) {
        val genderMap: Map<Int, String> = mapOf(
            Pair(0, "女"),
            Pair(1, "男"),
            Pair(9, "未知"),
            Pair(8, "其它")
        )
        val dbDate =
            SimpleDateFormat("yyyy年MM月dd日\nHH:mm:ss", Locale.CHINA).format(member.dbTimestamp)
        val birthday = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA).format(member.birthday)
        var col = 0
        sheet.addCell(Label(col, row, member.nickname))
        col++
        sheet.addCell(Label(col, row, genderMap[member.gender]))
        col++
        sheet.addCell(Label(col, row, birthday))
        col++
        sheet.addCell(Label(col, row, dbDate))
        col++
        sheet.addCell(Label(col, row, "${winCount + loseCount}"))
        col++
        sheet.addCell(Label(col, row, "$winCount"))
        col++
        sheet.addCell(Label(col, row, "$loseCount"))
        col++
        sheet.addCell(Label(col, row, member.comment))
    }
}