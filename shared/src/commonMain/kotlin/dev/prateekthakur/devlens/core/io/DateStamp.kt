package dev.prateekthakur.devlens.core.io

import kotlin.time.Clock

/** Today's date as `yyyy-MM-dd` (local time on desktop, UTC on web). */
expect fun todayStamp(): String

/** UTC date from the system clock, via the civil-from-days algorithm (no datetime library needed). */
internal fun utcTodayStamp(): String {
    val days = Clock.System.now().epochSeconds.floorDiv(86_400L)
    val z = days + 719_468
    val era = z.floorDiv(146_097L)
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    val y = yoe + era * 400 + if (m <= 2) 1 else 0
    return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}
