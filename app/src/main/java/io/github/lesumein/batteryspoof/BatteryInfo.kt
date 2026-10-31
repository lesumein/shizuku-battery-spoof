package io.github.lesumein.batteryspoof

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** 시스템이 브로드캐스트하는 값(스푸핑 영향을 받음)과 HAL 에서 직접 읽은 실제 값. */
data class BatteryInfo(
    val reportedLevel: Int? = null,
    val reportedStatus: Int? = null,
    val reportedPlugged: Int = 0,
    val temperatureC: Float? = null,
    val voltageMv: Int? = null,
    val actualLevel: Int? = null,
    val actualStatus: Int? = null,
)

object BatteryReader {
    fun read(context: Context, sticky: Intent? = null): BatteryInfo {
        val intent = sticky ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val actualLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val actualStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        return BatteryInfo(
            reportedLevel = if (level >= 0 && scale > 0) level * 100 / scale else null,
            reportedStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)?.takeIf { it > 0 },
            reportedPlugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0,
            temperatureC = if (temp != Int.MIN_VALUE) temp / 10f else null,
            voltageMv = voltage.takeIf { it > 0 },
            // 일부 기기는 지원하지 않으면 Int.MIN_VALUE 또는 0 을 반환한다.
            actualLevel = actualLevel?.takeIf { it in 0..100 },
            actualStatus = actualStatus?.takeIf { it > 0 },
        )
    }

    fun statusLabel(status: Int?): String = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "충전 중"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "방전 중"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "충전 안 함"
        BatteryManager.BATTERY_STATUS_FULL -> "완충"
        BatteryManager.BATTERY_STATUS_UNKNOWN -> "알 수 없음"
        null -> "-"
        else -> "알 수 없음($status)"
    }

    fun pluggedLabel(plugged: Int): String {
        if (plugged == 0) return "연결 안 됨"
        val parts = buildList {
            if (plugged and BatteryManager.BATTERY_PLUGGED_AC != 0) add("AC")
            if (plugged and BatteryManager.BATTERY_PLUGGED_USB != 0) add("USB")
            if (plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0) add("무선")
        }
        return parts.ifEmpty { listOf("기타($plugged)") }.joinToString(" + ")
    }
}

/** `dumpsys battery` 출력에서 필요한 값만 추린 결과. */
data class DumpsysBattery(
    val overridden: Boolean,
    val level: Int?,
    val status: Int?,
    val acPowered: Boolean?,
    val usbPowered: Boolean?,
    val raw: String,
) {
    companion object {
        fun parse(output: String): DumpsysBattery {
            fun value(key: String): String? = output.lineSequence()
                .map { it.trim() }
                .firstOrNull { it.startsWith("$key:") }
                ?.substringAfter(':')?.trim()
            return DumpsysBattery(
                overridden = output.contains("UPDATES STOPPED"),
                level = value("level")?.toIntOrNull(),
                status = value("status")?.toIntOrNull(),
                acPowered = value("AC powered")?.toBooleanStrictOrNull(),
                usbPowered = value("USB powered")?.toBooleanStrictOrNull(),
                raw = output.trim(),
            )
        }
    }
}
