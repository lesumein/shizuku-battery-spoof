package io.github.lesumein.batteryspoof

/** UserService 에 전달 가능한 `dumpsys battery` 인자 화이트리스트. */
object BatteryCommands {
    const val STATUS_UNKNOWN = 1
    const val STATUS_CHARGING = 2
    const val STATUS_DISCHARGING = 3
    const val STATUS_NOT_CHARGING = 4
    const val STATUS_FULL = 5

    const val TEMP_MIN = -200
    const val TEMP_MAX = 900

    fun setLevel(level: Int) = arrayOf("set", "level", level.coerceIn(0, 100).toString())
    fun setPlugged(source: PowerSource, plugged: Boolean) =
        arrayOf("set", source.key, if (plugged) "1" else "0")
    fun setStatus(status: Int) = arrayOf("set", "status", status.toString())
    /** 온도는 0.1℃ 단위 (예: 250 = 25.0℃) */
    fun setTemperature(tenthsCelsius: Int) =
        arrayOf("set", "temp", tenthsCelsius.coerceIn(TEMP_MIN, TEMP_MAX).toString())
    fun setPresent(present: Boolean) = arrayOf("set", "present", if (present) "1" else "0")
    fun setInvalidCharger(invalid: Boolean) = arrayOf("set", "invalid", if (invalid) "1" else "0")
    fun unplug() = arrayOf("unplug")
    fun reset() = arrayOf("reset")
    fun dump() = emptyArray<String>()

    fun isAllowed(args: Array<String>): Boolean = when {
        args.isEmpty() -> true
        args.size == 1 -> args[0] == "unplug" || args[0] == "reset"
        args.size == 3 && args[0] == "set" -> {
            val value = args[2].toIntOrNull() ?: return false
            when (args[1]) {
                "level" -> value in 0..100
                "ac", "usb", "wireless" -> value == 0 || value == 1
                "status" -> value in STATUS_UNKNOWN..STATUS_FULL
                "temp" -> value in TEMP_MIN..TEMP_MAX
                "present", "invalid" -> value == 0 || value == 1
                else -> false
            }
        }
        else -> false
    }
}

enum class PowerSource(val key: String, val label: String) {
    AC("ac", "AC"),
    USB("usb", "USB"),
    WIRELESS("wireless", "무선"),
}
