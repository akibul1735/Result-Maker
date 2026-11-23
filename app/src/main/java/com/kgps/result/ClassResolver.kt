package com.kgps.result

import java.text.Normalizer

object ClassResolver {

    data class PlannedCustomClass(
        val id: Int,
        val nameBn: String,
        val shortBn: String,
        val syllabusTier: Int,
        val canonicalKey: String,
        val rawVariants: List<String>
    ) {
        fun toCustomClassDef(): CustomClassDef = CustomClassDef(
            id = id,
            nameBn = nameBn,
            shortBn = shortBn,
            syllabusTier = syllabusTier
        )
    }

    sealed class ClassOverride {
        data class MapToExisting(val classId: Int) : ClassOverride()
        data class CreateCustom(
            val nameBn: String,
            val shortBn: String,
            val syllabusTier: Int
        ) : ClassOverride()
        data object Reject : ClassOverride()
    }

    data class DetectedUnregisteredClass(
        val canonicalKey: String,
        val suggestedNameBn: String,
        val suggestedShortBn: String,
        val suggestedTier: Int,
        val rawVariants: List<String>,
        val rowCount: Int
    )

    data class ExcludedNonClassGroup(
        val key: String,
        val displayLabel: String,
        val rawVariants: List<String>,
        val rowCount: Int,
        val isAutoStatus: Boolean
    )

    data class ClassResolutionPlan(
        val resolvedByNormalizedRaw: Map<String, Int>,
        val plannedClasses: List<PlannedCustomClass>,
        val autoCreatedClassIds: Set<Int>,
        val rejectedRaws: Set<String>,
        val autoExcludedStatusRaws: Set<String> = emptySet(),
        val limitReachedRaws: Set<String>
    ) {
        fun resolve(raw: String): Int? = resolvedByNormalizedRaw[normalizeLookupKey(raw)]
    }

    private data class ClassFamilySpec(
        val canonicalKey: String,
        val fixedPrimaryId: Int?, // 1..5 for core primary classes, null for custom/extendable
        val defaultNameBn: String,
        val defaultShortBn: String,
        val defaultTier: Int,
        val aliases: Set<String>
    )

    private val CORE_AND_KNOWN_FAMILIES: List<ClassFamilySpec> = listOf(
        // Put Pre-Primary / Early childhood BEFORE Class 1 so "প্রাক-প্রাথমিক" never collides with "প্রাথমিক"
        ClassFamilySpec(
            canonicalKey = "PRE_PRIMARY",
            fixedPrimaryId = null,
            defaultNameBn = "প্রাক-প্রাথমিক",
            defaultShortBn = "প্রাক",
            defaultTier = 1,
            aliases = setOf(
                "প্রাক প্রাথমিক", "প্রাকপ্রাথমিক", "প্রাক", "শিশু", "শিশু শ্রেণি", "শিশু শ্রেণী",
                "প্রাক প্রাথমিক শ্রেণি", "শিশু প্রাক", "প্রাথমিক পূর্ব", "প্রাক ১",
                "pre primary", "preprimary", "pre", "pp", "baby", "baby class",
                "pre primary class", "kinder", "pre kg", "prekg"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "PLAY",
            fixedPrimaryId = null,
            defaultNameBn = "প্লে",
            defaultShortBn = "প্লে",
            defaultTier = 1,
            aliases = setOf("প্লে", "প্লে গ্রুপ", "প্লেগ্রুপ", "play", "play group", "playgroup", "pg")
        ),
        ClassFamilySpec(
            canonicalKey = "NURSERY",
            fixedPrimaryId = null,
            defaultNameBn = "নার্সারি",
            defaultShortBn = "নার্সারি",
            defaultTier = 1,
            aliases = setOf("নার্সারি", "নার্সারী", "নার্সারি শ্রেণি", "nursery", "nur")
        ),
        ClassFamilySpec(
            canonicalKey = "KG_1",
            fixedPrimaryId = null,
            defaultNameBn = "কেজি-১",
            defaultShortBn = "কেজি-১",
            defaultTier = 1,
            aliases = setOf("কেজি 1", "কেজি ১", "কেজি ওয়ান", "কেজি ওয়ান", "kg 1", "kg one", "kg i", "kg1")
        ),
        ClassFamilySpec(
            canonicalKey = "KG_2",
            fixedPrimaryId = null,
            defaultNameBn = "কেজি-২",
            defaultShortBn = "কেজি-২",
            defaultTier = 1,
            aliases = setOf("কেজি 2", "কেজি ২", "কেজি টু", "kg 2", "kg two", "kg ii", "kg2")
        ),
        ClassFamilySpec(
            canonicalKey = "KG",
            fixedPrimaryId = null,
            defaultNameBn = "কেজি",
            defaultShortBn = "কেজি",
            defaultTier = 1,
            aliases = setOf("কেজি", "কে জি", "kg", "k g", "kindergarten")
        ),
        ClassFamilySpec(
            canonicalKey = "PRIMARY_1",
            fixedPrimaryId = 1,
            defaultNameBn = "প্রথম",
            defaultShortBn = "১ম",
            defaultTier = 1,
            aliases = setOf(
                "1", "1st", "1ম", "প্রথম", "প্রথমতম", "পহেলা",
                "first", "one", "i", "c1", "class 1", "class i", "class one"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "PRIMARY_2",
            fixedPrimaryId = 2,
            defaultNameBn = "দ্বিতীয়",
            defaultShortBn = "২য়",
            defaultTier = 1,
            aliases = setOf(
                "2", "2nd", "2য়", "2য়", "দ্বিতীয়", "দ্বিতীয়",
                "second", "two", "ii", "c2", "class 2", "class ii", "class two"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "PRIMARY_3",
            fixedPrimaryId = 3,
            defaultNameBn = "তৃতীয়",
            defaultShortBn = "৩য়",
            defaultTier = 3,
            aliases = setOf(
                "3", "3rd", "3য়", "3য়", "তৃতীয়", "তৃতীয়",
                "third", "three", "iii", "c3", "class 3", "class iii", "class three"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "PRIMARY_4",
            fixedPrimaryId = 4,
            defaultNameBn = "চতুর্থ",
            defaultShortBn = "৪র্থ",
            defaultTier = 3,
            aliases = setOf(
                "4", "4th", "4র্থ", "চতুর্থ",
                "fourth", "four", "iv", "c4", "class 4", "class iv", "class four"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "PRIMARY_5",
            fixedPrimaryId = 5,
            defaultNameBn = "পঞ্চম",
            defaultShortBn = "৫ম",
            defaultTier = 3,
            aliases = setOf(
                "5", "5th", "5ম", "পঞ্চম",
                "fifth", "five", "v", "c5", "class 5", "class v", "class five"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_6",
            fixedPrimaryId = null,
            defaultNameBn = "ষষ্ঠ",
            defaultShortBn = "৬ষ্ঠ",
            defaultTier = 3,
            aliases = setOf(
                "6th", "6ষ্ঠ", "ষষ্ঠ", "ষষ্ট", "sixth", "six", "vi", "c6", "class 6", "class vi", "class six"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_7",
            fixedPrimaryId = null,
            defaultNameBn = "সপ্তম",
            defaultShortBn = "৭ম",
            defaultTier = 3,
            aliases = setOf(
                "7th", "7ম", "সপ্তম", "seventh", "seven", "vii", "c7", "class 7", "class vii", "class seven"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_8",
            fixedPrimaryId = null,
            defaultNameBn = "অষ্টম",
            defaultShortBn = "৮ম",
            defaultTier = 3,
            aliases = setOf(
                "8th", "8ম", "অষ্টম", "অস্টম", "eighth", "eight", "viii", "c8", "class 8", "class viii", "class eight"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_9",
            fixedPrimaryId = null,
            defaultNameBn = "নবম",
            defaultShortBn = "৯ম",
            defaultTier = 3,
            aliases = setOf(
                "9th", "9ম", "নবম", "ninth", "nine", "ix", "c9", "class 9", "class ix", "class nine"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_10",
            fixedPrimaryId = null,
            defaultNameBn = "দশম",
            defaultShortBn = "১০ম",
            defaultTier = 3,
            aliases = setOf(
                "10th", "10ম", "দশম", "tenth", "ten", "x", "c10", "class 10", "class x", "class ten"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_11",
            fixedPrimaryId = null,
            defaultNameBn = "একাদশ",
            defaultShortBn = "১১শ",
            defaultTier = 3,
            aliases = setOf(
                "11th", "11শ", "একাদশ", "eleventh", "eleven", "xi", "c11", "class 11", "class xi", "class eleven"
            )
        ),
        ClassFamilySpec(
            canonicalKey = "CLASS_12",
            fixedPrimaryId = null,
            defaultNameBn = "দ্বাদশ",
            defaultShortBn = "১২শ",
            defaultTier = 3,
            aliases = setOf(
                "12th", "12শ", "দ্বাদশ", "twelfth", "twelve", "xii", "c12", "class 12", "class xii", "class twelve"
            )
        )
    )

    private val GENERIC_SHEET_TABS = setOf(
        "sheet", "sheet1", "sheet 1", "sheet2", "sheet 2", "sheet3", "sheet 3",
        "data", "students", "student", "roster", "list", "all", "template",
        "তালিকা", "শিক্ষার্থী", "ডেটা", "সব", "পাতা ১", "পাতা 1"
    )

    private val CLASS_NOISE_WORDS = setOf(
        "class", "cls", "grade", "std", "standard",
        "শ্রেণি", "শ্রেণী", "শ্রেণির", "শ্রেণীর", "ক্লাস"
    )

    private val NON_CLASS_STATUS_WORDS = setOf(
        "hold", "on hold", "onhold", "hold on", "held",
        "pending", "wait", "waiting", "waitlist", "wait list",
        "drop", "dropped", "dropout", "drop out",
        "tc", "t c", "transfer", "transferred", "left", "leave",
        "inactive", "suspend", "suspended", "pause", "paused",
        "cancel", "canceled", "cancelled", "reject", "rejected",
        "absent", "draft", "skip", "skipped", "exclude", "excluded",
        "none", "null", "na", "n a", "unknown", "temp", "temporary", "old",
        "হোল্ড", "হোল্ডে", "হোল্ডিং", "স্থগিত", "অপেক্ষমাণ", "অপেক্ষমান",
        "ঝরে পড়া", "ঝরে পড়া", "টিসি", "টি সি", "বদলি", "বাতিল",
        "বাদ", "বাদ দিন", "প্রত্যাহার", "অনুপস্থিত", "অজানা", "ফাঁকা", "খালি"
    )

    private val NON_CLASS_STATUS_COMPACT = NON_CLASS_STATUS_WORDS.map { it.replace(" ", "") }.toSet()

    private val NON_CLASS_PREFIX_TOKENS = setOf(
        "hold", "onhold", "pending", "dropout", "transferred", "cancelled", "canceled",
        "হোল্ড", "হোল্ডে", "স্থগিত", "বাতিল"
    )

    private val SECTION_PREFIX_WORDS = setOf("sec", "section", "শাখা", "বিভাগ")

    fun normalizeUnicode(raw: String): String {
        val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC)
        return fromBn(nfc).trim()
    }

    fun normalizeLookupKey(raw: String): String {
        return normalizeUnicode(raw)
            .lowercase()
            .replace("\u09AF\u09BC", "\u09DF")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun stripPunctuation(raw: String): String {
        return normalizeLookupKey(raw)
            .replace(Regex("[?:*।.,;()\\[\\]{}_\\-–—/|]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun isNonClassStatus(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || Repo.isBlankOrDashPlaceholder(trimmed)) return false
        val lookup = normalizeLookupKey(trimmed)
        val punct = stripPunctuation(trimmed)
        val stripped = stripClassWordsAndSection(trimmed)
        val candidates = listOf(lookup, punct, stripped).filter { it.isNotBlank() }
        for (c in candidates) {
            if (c in NON_CLASS_STATUS_WORDS || c.replace(" ", "") in NON_CLASS_STATUS_COMPACT) {
                return true
            }
        }
        val firstToken = punct.split(" ").firstOrNull().orEmpty()
        if (firstToken in NON_CLASS_PREFIX_TOKENS) return true
        return false
    }

    private fun stripClassWordsAndSection(raw: String): String {
        var s = normalizeLookupKey(raw)
            // Remove parenthesized section markers like "(ক)", "(a)", "(section a)"
            .replace(Regex("\\((?:ক|খ|গ|ঘ|ঙ|চ|[a-z]|sec(?:tion)?\\s*[a-zক-ঙ])\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("[?:*।.,;()\\[\\]{}_\\-–—/|]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // Strip leading/trailing noise words ("class", "শ্রেণি", etc.)
        val tokens = s.split(" ").filter { it.isNotBlank() }.toMutableList()
        while (tokens.isNotEmpty() && tokens.first() in CLASS_NOISE_WORDS) {
            tokens.removeAt(0)
        }
        while (tokens.isNotEmpty() && tokens.last() in CLASS_NOISE_WORDS) {
            tokens.removeAt(tokens.lastIndex)
        }
        // Strip trailing single-letter section marker like "1 a", "১ম ক", "প্রথম শাখা ক"
        if (tokens.size >= 3 && tokens[tokens.size - 2] in SECTION_PREFIX_WORDS) {
            tokens.removeAt(tokens.lastIndex)
            tokens.removeAt(tokens.lastIndex)
        } else if (tokens.size >= 2 && tokens.last() in setOf("a", "b", "c", "d", "ক", "খ", "গ", "ঘ")) {
            val head = tokens.dropLast(1).joinToString(" ")
            if (head != "kg" && head != "কেজি" && head != "class" && head != "শ্রেণি") {
                tokens.removeAt(tokens.lastIndex)
            }
        }
        s = tokens.joinToString(" ").trim()
        // Collapse spaces around digits attached to "ম"/"য়"/"র্থ"/"ষ্ঠ"/"শ"/"st"/"nd"/"rd"/"th" (e.g., "1 ম" -> "1ম")
        s = s.replace(Regex("(\\d+)\\s+(ম|য়|য়|র্থ|ষ্ঠ|শ|st|nd|rd|th)\\b"), "$1$2")
        return s
    }

    private fun matchKnownFamily(stripped: String): ClassFamilySpec? {
        if (stripped.isBlank()) return null
        val compact = stripped.replace(" ", "")
        for (spec in CORE_AND_KNOWN_FAMILIES) {
            if (stripped in spec.aliases || spec.aliases.any { it.replace(" ", "") == compact }) {
                return spec
            }
        }
        return null
    }

    private fun matchCustomClassDirectly(
        raw: String,
        stripped: String,
        customClasses: List<CustomClassDef>
    ): CustomClassDef? {
        if (customClasses.isEmpty()) return null
        val rawNorm = stripPunctuation(raw)
        val rawStrippedCompact = stripped.replace(" ", "")

        // 1. Exact / stripped match on nameBn or shortBn
        for (cc in customClasses) {
            val nameStripped = stripClassWordsAndSection(cc.nameBn)
            val shortStripped = stripClassWordsAndSection(cc.shortBn)
            val nameRawNorm = stripPunctuation(cc.nameBn)
            val shortRawNorm = stripPunctuation(cc.shortBn)
            if (stripped.isNotBlank() && (
                    stripped == nameStripped ||
                        (shortStripped.isNotBlank() && stripped == shortStripped) ||
                        rawStrippedCompact == nameStripped.replace(" ", "") ||
                        (shortStripped.isNotBlank() && rawStrippedCompact == shortStripped.replace(" ", ""))
                    ) ||
                (rawNorm.isNotBlank() && (rawNorm == nameRawNorm || (shortRawNorm.isNotBlank() && rawNorm == shortRawNorm)))
            ) {
                return cc
            }
        }

        // 2. Canonical family match (e.g., existing custom class is "শিশু শ্রেণি", CSV says "প্রাক-প্রাথমিক")
        val inputFamily = matchKnownFamily(stripped)
        if (inputFamily != null && inputFamily.fixedPrimaryId == null) {
            for (cc in customClasses) {
                val ccNameFamily = matchKnownFamily(stripClassWordsAndSection(cc.nameBn))
                val ccShortFamily = matchKnownFamily(stripClassWordsAndSection(cc.shortBn))
                if (ccNameFamily?.canonicalKey == inputFamily.canonicalKey ||
                    ccShortFamily?.canonicalKey == inputFamily.canonicalKey
                ) {
                    return cc
                }
            }
        }

        // 3. Bare number matching an existing custom class ID (only when raw itself is a pure integer) or CLASS_6..12 family
        val isPureIntegerRaw = fromBn(raw).trim().toIntOrNull() != null
        val bareNum = stripped.toIntOrNull()
        if (bareNum != null && bareNum in 6..20) {
            if (isPureIntegerRaw) {
                customClasses.firstOrNull { it.id == bareNum }?.let { return it }
            }
            val numFamilyKey = "CLASS_$bareNum"
            for (cc in customClasses) {
                val ccFam = matchKnownFamily(stripClassWordsAndSection(cc.nameBn))
                if (ccFam?.canonicalKey == numFamilyKey) return cc
            }
        }

        return null
    }

    /**
     * Resolves [raw] against the core classes 1..5 and registered [customClasses].
     * Returns null if not registered (except if [allowLegacyFallbackIds] is true for standalone parsing of 6..20).
     */
    fun resolveExistingClassId(
        raw: String,
        customClasses: List<CustomClassDef> = SchoolClassRegistry.currentCustom(),
        allowLegacyFallbackIds: Boolean = true
    ): Int? {
        if (Repo.isBlankOrDashPlaceholder(raw) || isNonClassStatus(raw)) return null
        val stripped = stripClassWordsAndSection(raw)
        if (stripped.isBlank()) return null

        val isPureIntegerRaw = fromBn(raw).trim().toIntOrNull() != null

        // 1. Bare integer check first (handles CSV round-trip IDs and 1..5)
        stripped.toIntOrNull()?.let { n ->
            if (n in 1..5) return n
            if (n in 6..20) {
                if (isPureIntegerRaw) {
                    customClasses.firstOrNull { it.id == n }?.let { return it.id }
                }
                val numFamKey = "CLASS_$n"
                customClasses.firstOrNull {
                    matchKnownFamily(stripClassWordsAndSection(it.nameBn))?.canonicalKey == numFamKey
                }?.let { return it.id }
                if (allowLegacyFallbackIds && customClasses.isEmpty()) return n
            }
        }

        // 2. Match existing custom classes directly or via canonical family
        matchCustomClassDirectly(raw, stripped, customClasses)?.let { return it.id }

        // 3. Match known families
        val fam = matchKnownFamily(stripped)
        if (fam != null) {
            if (fam.fixedPrimaryId != null) return fam.fixedPrimaryId
            if (allowLegacyFallbackIds && customClasses.isEmpty()) {
                when (fam.canonicalKey) {
                    "CLASS_6" -> return 6
                    "CLASS_7" -> return 7
                    "CLASS_8" -> return 8
                }
            }
            return null
        }

        // 4. Token-level fallback for multi-word descriptions (never mistaking "প্রাক-প্রাথমিক" for 1)
        val tokens = stripped.split(" ").filter { it.isNotBlank() }
        if (tokens.none { it.contains("প্রাক") || it.contains("শিশু") || it == "pre" || it.contains("play") || it.contains("nursery") || it == "kg" || it == "কেজি" }) {
            for (tok in tokens) {
                val tokFam = matchKnownFamily(tok)
                if (tokFam?.fixedPrimaryId != null) return tokFam.fixedPrimaryId
            }
        }

        return null
    }

    fun looksValidAsNewCustomClass(raw: String, isSheetTabName: Boolean = false): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || Repo.isBlankOrDashPlaceholder(trimmed)) return false
        if (isNonClassStatus(trimmed)) return false
        val lookup = normalizeLookupKey(trimmed)
        if (isSheetTabName && (lookup in GENERIC_SHEET_TABS || Regex("^sheet\\s*\\d+$").matches(lookup))) {
            return false
        }
        val stripped = stripClassWordsAndSection(trimmed)
        if (stripped.isBlank()) return false
        if (stripped.length > 40) return false
        if (trimmed.contains('@')) return false
        // Reject dates like 01/01/2020 or 2020-01-01
        if (Regex("^\\d{1,4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,4}$").matches(fromBn(trimmed).trim())) return false
        // Reject phone numbers / numeric IDs / academic years (> 20)
        val digitsOnly = fromBn(trimmed).filter { it.isDigit() }
        if (digitsOnly.length >= 6) return false
        stripped.toIntOrNull()?.let { n ->
            return n in 6..20
        }
        // Known family always valid
        if (matchKnownFamily(stripped) != null) return true
        // Custom arbitrary class name must contain at least one Bengali or Latin letter
        return stripped.any { ch -> ch in 'a'..'z' || ch in '\u0980'..'\u09FF' }
    }

    fun canonicalBucketForNewClass(raw: String): DetectedUnregisteredClass? {
        if (!looksValidAsNewCustomClass(raw)) return null
        val stripped = stripClassWordsAndSection(raw)
        val fam = matchKnownFamily(stripped)
        if (fam != null) {
            if (fam.fixedPrimaryId != null) return null
            return DetectedUnregisteredClass(
                canonicalKey = fam.canonicalKey,
                suggestedNameBn = fam.defaultNameBn,
                suggestedShortBn = fam.defaultShortBn,
                suggestedTier = fam.defaultTier,
                rawVariants = listOf(raw.trim()),
                rowCount = 1
            )
        }
        val bareNum = stripped.toIntOrNull()
        if (bareNum != null && bareNum in 6..20) {
            val famKey = "CLASS_$bareNum"
            val matched = CORE_AND_KNOWN_FAMILIES.firstOrNull { it.canonicalKey == famKey }
            if (matched != null) {
                return DetectedUnregisteredClass(
                    canonicalKey = matched.canonicalKey,
                    suggestedNameBn = matched.defaultNameBn,
                    suggestedShortBn = matched.defaultShortBn,
                    suggestedTier = matched.defaultTier,
                    rawVariants = listOf(raw.trim()),
                    rowCount = 1
                )
            }
            return DetectedUnregisteredClass(
                canonicalKey = famKey,
                suggestedNameBn = ordinalBn(bareNum),
                suggestedShortBn = ordinalBn(bareNum),
                suggestedTier = 3,
                rawVariants = listOf(raw.trim()),
                rowCount = 1
            )
        }

        val cleanDisplay = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
            .replace(Regex("\\s+"), " ")
            .removeSuffix("শ্রেণি")
            .removeSuffix("শ্রেণী")
            .trim()
            .ifBlank { raw.trim() }
            .take(40)
        val bucketKey = "CUSTOM_${stripped.replace(" ", "_")}"
        val lowTierHints = listOf("প্রাক", "শিশু", "প্লে", "নার্সারি", "কেজি", "play", "nursery", "kg", "pre", "baby")
        val inferredTier = if (lowTierHints.any { stripped.contains(it) }) 1 else 3
        return DetectedUnregisteredClass(
            canonicalKey = bucketKey,
            suggestedNameBn = cleanDisplay,
            suggestedShortBn = defaultShortClassLabel(cleanDisplay, 6).take(12),
            suggestedTier = inferredTier,
            rawVariants = listOf(raw.trim()),
            rowCount = 1
        )
    }

    /**
     * Scans all raw class column values in an import sheet and finds unregistered classes
     * grouped by canonical class identity (so "প্রাক-প্রাথমিক", "প্রাক প্রাথমিক", "Pre-Primary" share one entry).
     */
    fun detectUnregisteredClasses(
        rawClassValues: List<String>,
        existingCustomClasses: List<CustomClassDef>
    ): List<DetectedUnregisteredClass> {
        val byBucket = linkedMapOf<String, DetectedUnregisteredClass>()
        rawClassValues.forEach { raw ->
            if (raw.isBlank() || Repo.isBlankOrDashPlaceholder(raw)) return@forEach
            val existingId = resolveExistingClassId(raw, existingCustomClasses, allowLegacyFallbackIds = false)
            if (existingId != null) return@forEach

            val bucket = canonicalBucketForNewClass(raw) ?: return@forEach
            val prev = byBucket[bucket.canonicalKey]
            if (prev == null) {
                byBucket[bucket.canonicalKey] = bucket
            } else {
                val nextVariants = if (raw.trim() in prev.rawVariants) {
                    prev.rawVariants
                } else {
                    prev.rawVariants + raw.trim()
                }
                byBucket[bucket.canonicalKey] = prev.copy(
                    rawVariants = nextVariants,
                    rowCount = prev.rowCount + 1
                )
            }
        }
        return byBucket.values.toList()
    }

    fun detectExcludedNonClasses(
        rawClassValues: List<String>,
        existingCustomClasses: List<CustomClassDef>,
        overrides: Map<String, ClassOverride> = emptyMap()
    ): List<ExcludedNonClassGroup> {
        val groups = linkedMapOf<String, ExcludedNonClassGroup>()
        rawClassValues.forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isBlank() || Repo.isBlankOrDashPlaceholder(trimmed)) return@forEach

            val isStatus = isNonClassStatus(trimmed)
            if (isStatus) {
                val key = "STATUS_${normalizeLookupKey(trimmed)}"
                val prev = groups[key]
                groups[key] = if (prev == null) {
                    ExcludedNonClassGroup(
                        key = key,
                        displayLabel = trimmed,
                        rawVariants = listOf(trimmed),
                        rowCount = 1,
                        isAutoStatus = true
                    )
                } else {
                    prev.copy(
                        rawVariants = if (trimmed in prev.rawVariants) prev.rawVariants else prev.rawVariants + trimmed,
                        rowCount = prev.rowCount + 1
                    )
                }
                return@forEach
            }

            if (resolveExistingClassId(trimmed, existingCustomClasses, allowLegacyFallbackIds = false) != null) {
                return@forEach
            }
            val bucket = canonicalBucketForNewClass(trimmed)
            if (bucket != null && overrides[bucket.canonicalKey] is ClassOverride.Reject) {
                val key = bucket.canonicalKey
                val prev = groups[key]
                groups[key] = if (prev == null) {
                    ExcludedNonClassGroup(
                        key = key,
                        displayLabel = bucket.suggestedNameBn,
                        rawVariants = listOf(trimmed),
                        rowCount = 1,
                        isAutoStatus = false
                    )
                } else {
                    prev.copy(
                        rawVariants = if (trimmed in prev.rawVariants) prev.rawVariants else prev.rawVariants + trimmed,
                        rowCount = prev.rowCount + 1
                    )
                }
            }
        }
        return groups.values.toList()
    }

    /**
     * Produces a complete class resolution plan for import preview & saving,
     * allocating free custom class IDs (6..20, max 15 custom classes) for valid unmatched classes.
     */
    fun planForImport(
        rawClassValues: List<String>,
        existingCustomClasses: List<CustomClassDef>,
        overrides: Map<String, ClassOverride> = emptyMap()
    ): ClassResolutionPlan {
        val resolvedMap = linkedMapOf<String, Int>()
        val plannedList = mutableListOf<PlannedCustomClass>()
        val autoCreatedIds = linkedSetOf<Int>()
        val rejected = linkedSetOf<String>()
        val autoExcludedStatuses = linkedSetOf<String>()
        val limitReached = linkedSetOf<String>()

        val workingCustom = existingCustomClasses.toMutableList()
        val bucketToId = linkedMapOf<String, Int>()

        rawClassValues.forEach { raw ->
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return@forEach
            val lookupKey = normalizeLookupKey(trimmed)
            if (resolvedMap.containsKey(lookupKey) || lookupKey in rejected || lookupKey in limitReached) {
                return@forEach
            }

            if (isNonClassStatus(trimmed)) {
                rejected.add(lookupKey)
                autoExcludedStatuses.add(lookupKey)
                return@forEach
            }

            // 1. Check against already registered classes (including those planned earlier in this pass)
            val existingId = resolveExistingClassId(trimmed, workingCustom, allowLegacyFallbackIds = false)
            if (existingId != null) {
                resolvedMap[lookupKey] = existingId
                return@forEach
            }

            val bucket = canonicalBucketForNewClass(trimmed)
            if (bucket == null) {
                rejected.add(lookupKey)
                return@forEach
            }

            // 2. Apply explicit user override (if provided for this bucket)
            when (val ov = overrides[bucket.canonicalKey]) {
                is ClassOverride.Reject -> {
                    rejected.add(lookupKey)
                    return@forEach
                }
                is ClassOverride.MapToExisting -> {
                    resolvedMap[lookupKey] = ov.classId
                    return@forEach
                }
                is ClassOverride.CreateCustom, null -> {
                    val alreadyAllocatedId = bucketToId[bucket.canonicalKey]
                    if (alreadyAllocatedId != null) {
                        resolvedMap[lookupKey] = alreadyAllocatedId
                        val idx = plannedList.indexOfFirst { it.id == alreadyAllocatedId }
                        if (idx >= 0 && trimmed !in plannedList[idx].rawVariants) {
                            plannedList[idx] = plannedList[idx].copy(
                                rawVariants = plannedList[idx].rawVariants + trimmed
                            )
                        }
                        return@forEach
                    }

                    val nextId = nextAvailableCustomClassId(workingCustom)
                    if (nextId == null) {
                        limitReached.add(lookupKey)
                        return@forEach
                    }

                    val nameBn = (ov as? ClassOverride.CreateCustom)?.nameBn?.trim()?.takeIf { it.isNotBlank() }
                        ?: bucket.suggestedNameBn
                    val shortBn = (ov as? ClassOverride.CreateCustom)?.shortBn?.trim()?.takeIf { it.isNotBlank() }
                        ?: bucket.suggestedShortBn
                    val tier = (ov as? ClassOverride.CreateCustom)?.syllabusTier?.let { if (it <= 2) 1 else 3 }
                        ?: bucket.suggestedTier

                    val planned = PlannedCustomClass(
                        id = nextId,
                        nameBn = nameBn.take(40),
                        shortBn = shortBn.take(12),
                        syllabusTier = tier,
                        canonicalKey = bucket.canonicalKey,
                        rawVariants = listOf(trimmed)
                    )
                    workingCustom.add(planned.toCustomClassDef())
                    plannedList.add(planned)
                    autoCreatedIds.add(nextId)
                    bucketToId[bucket.canonicalKey] = nextId
                    resolvedMap[lookupKey] = nextId
                }
            }
        }

        return ClassResolutionPlan(
            resolvedByNormalizedRaw = resolvedMap,
            plannedClasses = plannedList,
            autoCreatedClassIds = autoCreatedIds,
            rejectedRaws = rejected,
            autoExcludedStatusRaws = autoExcludedStatuses,
            limitReachedRaws = limitReached
        )
    }
}
