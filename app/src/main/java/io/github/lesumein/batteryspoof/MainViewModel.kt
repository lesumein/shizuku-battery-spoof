package io.github.lesumein.batteryspoof

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val battery: BatteryInfo = BatteryInfo(),
    val dumpsys: DumpsysBattery? = null,
    val busy: Boolean = false,
    val lastMessage: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val shizuku = ShizukuManager(application.packageName)
    val shizukuState: StateFlow<ShizukuState> get() = shizuku.state

    private val _ui = MutableStateFlow(UiState(battery = BatteryReader.read(application)))
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            _ui.update { it.copy(battery = BatteryReader.read(context, intent)) }
        }
    }

    init {
        ContextCompat.registerReceiver(
            application,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        shizuku.start()
        viewModelScope.launch {
            shizuku.state.distinctUntilChangedBy { it.serviceStatus }.collect {
                if (it.serviceStatus == ServiceStatus.CONNECTED) refreshDumpsys()
            }
        }
    }

    fun requestPermission() = shizuku.requestPermission()

    fun refreshAll() {
        shizuku.refresh()
        _ui.update { it.copy(battery = BatteryReader.read(getApplication())) }
        refreshDumpsys()
    }

    fun applyLevel(level: Int) = execute("레벨을 $level% 로 설정", listOf(BatteryCommands.setLevel(level)))

    fun applyCharging(charging: Boolean, source: PowerSource) {
        val commands = if (charging) {
            PowerSource.entries.map { BatteryCommands.setPlugged(it, it == source) } +
                listOf(BatteryCommands.setStatus(BatteryCommands.STATUS_CHARGING))
        } else {
            listOf(
                BatteryCommands.unplug(),
                BatteryCommands.setStatus(BatteryCommands.STATUS_DISCHARGING),
            )
        }
        execute(if (charging) "${source.label} 충전 중으로 표시" else "충전 안 함으로 표시", commands)
    }

    fun applyStatus(status: Int) =
        execute("상태를 ${BatteryReader.statusLabel(status)}(으)로 설정", listOf(BatteryCommands.setStatus(status)))

    fun reset() = execute("원래대로 복구", listOf(BatteryCommands.reset()))

    private fun refreshDumpsys() {
        val service = shizuku.service ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { service.runBatteryCommand(BatteryCommands.dump()) } }
            result.getOrNull()?.let { out ->
                _ui.update { it.copy(dumpsys = DumpsysBattery.parse(out.substringAfter('\n'))) }
            }
        }
    }

    private fun execute(label: String, commands: List<Array<String>>) {
        val service = shizuku.service
        if (service == null) {
            _ui.update { it.copy(lastMessage = "Shizuku 서비스가 연결되지 않았습니다.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(busy = true) }
            val error = withContext(Dispatchers.IO) {
                commands.firstNotNullOfOrNull { args ->
                    val out = runCatching { service.runBatteryCommand(args) }
                        .getOrElse { e -> "-1\n${e.message}" }
                    val code = out.substringBefore('\n').toIntOrNull() ?: -1
                    val body = out.substringAfter('\n', "").trim()
                    if (code != 0) "dumpsys battery ${args.joinToString(" ")} 실패($code): $body" else null
                }
            }
            _ui.update { it.copy(busy = false, lastMessage = error ?: "$label 완료") }
            refreshDumpsys()
        }
    }

    fun consumeMessage() = _ui.update { it.copy(lastMessage = null) }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(batteryReceiver)
        shizuku.stop()
    }
}
