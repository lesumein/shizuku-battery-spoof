package io.github.lesumein.batteryspoof.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lesumein.batteryspoof.BatteryCommands
import io.github.lesumein.batteryspoof.BatteryReader
import io.github.lesumein.batteryspoof.MainViewModel
import io.github.lesumein.batteryspoof.PowerSource
import io.github.lesumein.batteryspoof.ServiceStatus
import io.github.lesumein.batteryspoof.ShizukuState
import io.github.lesumein.batteryspoof.ShizukuStatus
import io.github.lesumein.batteryspoof.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val shizuku by vm.shizukuState.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshAll() }
    LaunchedEffect(ui.lastMessage) {
        ui.lastMessage?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("배터리 표시 변경") },
                actions = { TextButton(onClick = vm::refreshAll) { Text("새로고침") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShizukuCard(shizuku, onRequestPermission = vm::requestPermission)
            BatteryCard(ui)
            ControlCard(
                ui = ui,
                enabled = shizuku.serviceStatus == ServiceStatus.CONNECTED && !ui.busy,
                onApplyLevel = vm::applyLevel,
                onApplyCharging = vm::applyCharging,
                onApplyStatus = vm::applyStatus,
                onReset = vm::reset,
            )
            ui.dumpsys?.let { DumpsysCard(it.raw) }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ShizukuCard(state: ShizukuState, onRequestPermission: () -> Unit) {
    SectionCard("Shizuku") {
        val statusText = when (state.status) {
            ShizukuStatus.NOT_RUNNING -> "실행 중 아님 (설치 후 Shizuku 를 시작하세요)"
            ShizukuStatus.TOO_OLD -> "Shizuku 버전이 너무 낮음 (v11 이상 필요)"
            ShizukuStatus.PERMISSION_REQUIRED -> "실행 중 · 권한 필요"
            ShizukuStatus.PERMISSION_DENIED -> "권한 거부됨 (Shizuku 앱에서 직접 허용하세요)"
            ShizukuStatus.GRANTED -> "실행 중 · 권한 허용됨"
        }
        InfoRow("상태", statusText)
        state.shizukuUid?.let { InfoRow("Shizuku 권한", uidLabel(it)) }
        val serviceText = when (state.serviceStatus) {
            ServiceStatus.DISCONNECTED -> "연결 안 됨"
            ServiceStatus.CONNECTING -> "연결 중…"
            ServiceStatus.CONNECTED -> "연결됨" + (state.serviceUid?.let { " (${uidLabel(it)})" } ?: "")
        }
        InfoRow("UserService 바인더", serviceText)
        if (state.status == ShizukuStatus.PERMISSION_REQUIRED) {
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text("Shizuku 권한 요청")
            }
        }
    }
}

private fun uidLabel(uid: Int) = when (uid) {
    0 -> "root"
    2000 -> "shell (adb)"
    else -> "uid $uid"
}

@Composable
private fun BatteryCard(ui: UiState) {
    val b = ui.battery
    SectionCard("현재 배터리 정보") {
        InfoRow("실제 잔량 (HAL)", b.actualLevel?.let { "$it%" } ?: "-")
        InfoRow("실제 상태 (HAL)", BatteryReader.statusLabel(b.actualStatus))
        InfoRow("표시 잔량 (시스템)", b.reportedLevel?.let { "$it%" } ?: "-")
        InfoRow("표시 상태 (시스템)", BatteryReader.statusLabel(b.reportedStatus))
        InfoRow("표시 전원", BatteryReader.pluggedLabel(b.reportedPlugged))
        b.temperatureC?.let { InfoRow("온도", "%.1f℃".format(it)) }
        b.voltageMv?.let { InfoRow("전압", "$it mV") }
        ui.dumpsys?.let {
            InfoRow("값 덮어쓰기", if (it.overridden) "적용 중" else "없음")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlCard(
    ui: UiState,
    enabled: Boolean,
    onApplyLevel: (Int) -> Unit,
    onApplyCharging: (Boolean, PowerSource) -> Unit,
    onApplyStatus: (Int) -> Unit,
    onReset: () -> Unit,
) {
    var level by rememberSaveable { mutableFloatStateOf((ui.battery.reportedLevel ?: 50).toFloat()) }
    var source by rememberSaveable { mutableStateOf(PowerSource.AC) }
    val charging = ui.dumpsys?.let { it.acPowered == true || it.usbPowered == true }
        ?: (ui.battery.reportedPlugged != 0)

    SectionCard("표시 값 변경") {
        if (!enabled && !ui.busy) {
            Text(
                "Shizuku 권한을 허용하고 서비스가 연결되면 사용할 수 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text("배터리 잔량: ${level.toInt()}%", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = level,
            onValueChange = { level = it },
            valueRange = 0f..100f,
            steps = 99,
            enabled = enabled,
        )
        Button(
            onClick = { onApplyLevel(level.toInt()) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("잔량 적용") }

        Spacer(Modifier.padding(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("충전 중으로 표시", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (charging) "현재: 충전 중 표시" else "현재: 충전 안 함 표시",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = charging,
                onCheckedChange = { onApplyCharging(it, source) },
                enabled = enabled,
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PowerSource.entries.forEachIndexed { i, s ->
                SegmentedButton(
                    selected = source == s,
                    onClick = {
                        source = s
                        if (charging) onApplyCharging(true, s)
                    },
                    shape = SegmentedButtonDefaults.itemShape(i, PowerSource.entries.size),
                    enabled = enabled,
                ) { Text(s.label) }
            }
        }

        Text("배터리 상태 직접 지정", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                BatteryCommands.STATUS_CHARGING,
                BatteryCommands.STATUS_DISCHARGING,
                BatteryCommands.STATUS_NOT_CHARGING,
                BatteryCommands.STATUS_FULL,
            ).forEach { status ->
                FilterChip(
                    selected = (ui.dumpsys?.status ?: ui.battery.reportedStatus) == status,
                    onClick = { onApplyStatus(status) },
                    label = { Text(BatteryReader.statusLabel(status)) },
                    enabled = enabled,
                )
            }
        }

        Spacer(Modifier.padding(4.dp))

        OutlinedButton(
            onClick = onReset,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) { Text("원래대로 복구 (reset)") }
    }
}

@Composable
private fun DumpsysCard(raw: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("dumpsys battery", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            SelectionContainer {
                Text(raw, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
