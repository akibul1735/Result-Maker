package com.kgps.result

enum class SeatPlanOrientation(val labelBn: String) {
    PORTRAIT("পোর্ট্রেট (লম্বা)"),
    LANDSCAPE("ল্যান্ডস্কেপ (আড়াআড়ি)")
}

enum class SeatPlanRollOrder(val labelBn: String) {
    DESCENDING("বড় → ছোট"),
    ASCENDING("ছোট → বড়")
}

enum class SeatPlanPairMode(val labelBn: String, val descBn: String) {
    SEQUENTIAL("ধারাবাহিক", "নির্বাচিত শ্রেণির সব শিক্ষার্থী পরপর"),
    SIDE_BY_SIDE("পাশাপাশি ২ শ্রেণি", "বাম কলাম ও ডান কলামে আলাদা শ্রেণি (বেঞ্চ জোড়া)")
}

enum class SeatPlanNumeralMode(val labelBn: String) {
    BENGALI("বাংলা (০১, ৫ম)"),
    ENGLISH("English (01)")
}

enum class SeatPlanFontScale(val labelBn: String, val factor: Float) {
    SMALL("ছোট", 0.88f),
    NORMAL("স্বাভাবিক", 1.0f),
    LARGE("বড়", 1.12f)
}

data class SeatPlanConfig(
    val selectedClasses: List<Int> = listOf(5),
    val pairMode: SeatPlanPairMode = SeatPlanPairMode.SEQUENTIAL,
    val leftClass: Int? = 4,
    val rightClass: Int? = 5,
    val rollOrder: SeatPlanRollOrder = SeatPlanRollOrder.DESCENDING,
    val rollFrom: Int? = null,
    val rollTo: Int? = null,
    val term: Int = 3,
    val customExamTitle: String = "",
    val subtitle: String = "আসন বিন্যাস",
    val roomLabel: String = "",
    val customSchoolName: String = "",
    val customSchoolAddress: String = "",
    val showSchoolAddress: Boolean = true,
    val columns: Int = 2,
    val rows: Int = 6,
    val orientation: SeatPlanOrientation = SeatPlanOrientation.PORTRAIT,
    val showCutLines: Boolean = true,
    val roundedCorners: Boolean = true,
    val boldStudentName: Boolean = false,
    val padRollTwoDigits: Boolean = true,
    val numeralMode: SeatPlanNumeralMode = SeatPlanNumeralMode.BENGALI,
    val fontScale: SeatPlanFontScale = SeatPlanFontScale.NORMAL,
    val extraBlankSlips: Int = 0
) {
    val safeColumns: Int get() = columns.coerceIn(1, 3)
    val safeRows: Int get() = rows.coerceIn(3, 10)
    val slipsPerPage: Int get() = safeColumns * safeRows
}

data class SeatPlanSlip(
    val studentName: String,
    val cls: Int,
    val roll: Int?,
    val isBlank: Boolean = false
)

data class SeatPlanPageSpec(
    val pageNumber: Int,
    val totalPages: Int,
    val slips: List<SeatPlanSlip?>,
    val config: SeatPlanConfig
)

object SeatPlanPlanner {

    fun formatDigits(raw: String, mode: SeatPlanNumeralMode): String = when (mode) {
        SeatPlanNumeralMode.BENGALI -> bn(raw)
        SeatPlanNumeralMode.ENGLISH -> fromBn(raw)
    }

    fun formatRoll(roll: Int?, config: SeatPlanConfig): String {
        if (roll == null) return ".........."
        val base = if (config.padRollTwoDigits && roll in 0..9) {
            roll.toString().padStart(2, '0')
        } else {
            roll.toString()
        }
        return formatDigits(base, config.numeralMode)
    }

    fun resolvedSchoolName(config: SeatPlanConfig, settings: Settings): String =
        config.customSchoolName.trim().ifBlank { settings.school.ifBlank { "প্রাথমিক বিদ্যালয়" } }

    fun resolvedSchoolAddress(config: SeatPlanConfig, settings: Settings): String =
        if (!config.showSchoolAddress) ""
        else config.customSchoolAddress.trim().ifBlank { settings.schoolAddress.trim() }

    fun resolvedExamTitle(config: SeatPlanConfig, settings: Settings): String {
        val custom = config.customExamTitle.trim()
        if (custom.isNotEmpty()) return custom
        val yearStr = formatDigits(settings.year.ifBlank { "2026" }, config.numeralMode)
        return "${termNameBn(config.term)} মূল্যায়ন - $yearStr"
    }

    fun resolvedSubtitle(config: SeatPlanConfig): String {
        val sub = config.subtitle.trim().ifBlank { "আসন বিন্যাস" }
        val room = config.roomLabel.trim()
        return if (room.isEmpty()) sub else "$sub · $room"
    }

    fun buildPages(
        students: List<Student>,
        settings: Settings,
        config: SeatPlanConfig,
        allowedClassIds: List<Int> = activeClassIds(settings)
    ): List<SeatPlanPageSpec> {
        val cols = config.safeColumns
        val rows = config.safeRows
        val perPage = cols * rows
        val allowedSet = allowedClassIds.toSet()

        fun slipsForClass(clsId: Int): List<SeatPlanSlip> {
            val filtered = students.filter { s ->
                s.cls == clsId &&
                    (config.rollFrom == null || s.roll >= config.rollFrom) &&
                    (config.rollTo == null || s.roll <= config.rollTo)
            }
            val sorted = when (config.rollOrder) {
                SeatPlanRollOrder.ASCENDING -> filtered.sortedBy { it.roll }
                SeatPlanRollOrder.DESCENDING -> filtered.sortedByDescending { it.roll }
            }
            return sorted.map { s ->
                SeatPlanSlip(
                    studentName = s.name.trim(),
                    cls = s.cls,
                    roll = s.roll,
                    isBlank = false
                )
            }
        }

        val blankCount = config.extraBlankSlips.coerceIn(0, 48)

        if (config.pairMode == SeatPlanPairMode.SIDE_BY_SIDE && cols >= 2) {
            val leftCls = config.leftClass?.takeIf { it in allowedSet }
            val rightCls = config.rightClass?.takeIf { it in allowedSet }
            val leftList = leftCls?.let(::slipsForClass).orEmpty()
            val rightList = rightCls?.let(::slipsForClass).orEmpty()
            val extraEachCol = (blankCount + cols - 1) / cols
            val totalRows = maxOf(leftList.size, rightList.size) + extraEachCol
            if (totalRows <= 0 || (leftList.isEmpty() && rightList.isEmpty() && blankCount == 0)) {
                return emptyList()
            }
            val totalPages = ((totalRows + rows - 1) / rows).coerceAtLeast(1)
            return (0 until totalPages).map { p ->
                val grid = ArrayList<SeatPlanSlip?>(perPage)
                for (r in 0 until rows) {
                    val globalRow = p * rows + r
                    for (c in 0 until cols) {
                        val isLeftCol = (c % 2 == 0)
                        val sourceList = if (isLeftCol) leftList else rightList
                        val sourceCls = if (isLeftCol) (leftCls ?: rightCls ?: 1) else (rightCls ?: leftCls ?: 1)
                        val slip = when {
                            globalRow < sourceList.size -> sourceList[globalRow]
                            globalRow < totalRows -> SeatPlanSlip("", sourceCls, null, isBlank = true)
                            else -> null
                        }
                        grid.add(slip)
                    }
                }
                SeatPlanPageSpec(
                    pageNumber = p + 1,
                    totalPages = totalPages,
                    slips = grid,
                    config = config
                )
            }
        }

        val targetClasses = config.selectedClasses.filter { it in allowedSet }.distinct()
        val orderedSlips = targetClasses.flatMap(::slipsForClass).toMutableList()
        val fallbackCls = targetClasses.firstOrNull() ?: allowedClassIds.firstOrNull() ?: 1
        repeat(blankCount) {
            orderedSlips.add(SeatPlanSlip("", fallbackCls, null, isBlank = true))
        }
        if (orderedSlips.isEmpty()) return emptyList()

        val totalPages = ((orderedSlips.size + perPage - 1) / perPage).coerceAtLeast(1)
        return (0 until totalPages).map { p ->
            val start = p * perPage
            val grid = (0 until perPage).map { offset ->
                orderedSlips.getOrNull(start + offset)
            }
            SeatPlanPageSpec(
                pageNumber = p + 1,
                totalPages = totalPages,
                slips = grid,
                config = config
            )
        }
    }
}
