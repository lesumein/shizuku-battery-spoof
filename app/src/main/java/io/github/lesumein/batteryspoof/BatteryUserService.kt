package io.github.lesumein.batteryspoof

import android.content.Context
import android.os.Process
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shizuku 가 shell(또는 root) 권한으로 띄우는 별도 프로세스에서 동작하는 서비스.
 * 일반 [Process] 실행만 사용하므로 숨겨진 API(Shizuku.newProcess)에 의존하지 않는다.
 */
class BatteryUserService : IBatteryService.Stub {

    @Keep
    constructor() : super()

    // Shizuku 13+ 는 Context 를 받는 생성자를 우선 사용한다.
    @Keep
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : super()

    override fun destroy() {
        exitProcess(0)
    }

    override fun getUid(): Int = Process.myUid()

    override fun runBatteryCommand(args: Array<String>): String {
        if (!BatteryCommands.isAllowed(args)) {
            return "-1\n허용되지 않은 인자: ${args.joinToString(" ")}"
        }
        return try {
            val process = ProcessBuilder(listOf("dumpsys", "battery") + args)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroy()
                return "-1\n시간 초과"
            }
            "${process.exitValue()}\n$output"
        } catch (e: Exception) {
            "-1\n${e.javaClass.simpleName}: ${e.message}"
        }
    }
}
