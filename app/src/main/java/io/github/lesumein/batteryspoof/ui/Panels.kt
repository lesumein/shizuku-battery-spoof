package io.github.lesumein.batteryspoof.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.lesumein.batteryspoof.BatteryCommands
import io.github.lesumein.batteryspoof.BatteryReader
import io.github.lesumein.batteryspoof.PowerSource
import io.github.lesumein.batteryspoof.ServiceStatus
import io.github.lesumein.batteryspoof.ShizukuState
import io.github.lesumein.batteryspoof.ShizukuStatus
import io.github.lesumein.batteryspoof.UiState

@Composable
private fun PanelTitle(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    subtitle?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = LobbyColors.TextDim)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = LobbyColors.TextDim)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LobbyColors.TextDim)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelPanel(ui: UiState, enabled: Boolean, onApply: (Int) -> Unit) {
    var level by rememberSaveable {
        mutableFloatStateOf((ui.dumpsys?.level ?: ui.battery.reportedLevel ?: 50).toFloat())
    }
    PanelTitle("배터리 잔량", "dumpsys battery set level")
    Text(
        "${level.toInt()}%",
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        color = levelColor(level.toInt()),
        modifier = Modifier.padding(top = 12.dp),
    )
    Slider(value = level, onValueChange = { level = it }, valueRange = 0f..100f, steps = 99, enabled = enabled)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1, 5, 15, 50, 100).forEach { preset ->
            FilterChip(
                selected = level.toInt() == preset,
                onClick = { level = preset.toFloat() },
                label = { Text("$preset%") },
                enabled = enabled,
            )
        }
    }
    Button(
        onClick = { onApply(level.toInt()) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) { Text("잔량 적용", fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingPanel(charging: Boolean, enabled: Boolean, onApply: (Boolean, PowerSource) -> Unit) {
    var source by rememberSaveable { mutableStateOf(PowerSource.AC) }
    PanelTitle("충전 표시", "set ac/usb/wireless · unplug · set status")
    SwitchRow(
        title = "충전 중으로 표시",
        subtitle = if (charging) "현재: 충전 중 표시" else "현재: 충전 안 함 표시",
        checked = charging,
        enabled = enabled,
        onChange = { onApply(it, source) },
    )
    Text("전원 종류", style = MaterialTheme.typography.labelLarge, color = LobbyColors.TextDim)
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        PowerSource.entries.forEachIndexed { i, s ->
            SegmentedButton(
                selected = source == s,
                onClick = {
                    source = s
                    if (charging) onApply(true, s)
                },
                shape = SegmentedButtonDefaults.itemShape(i, PowerSource.entries.size),
                enabled = enabled,
            ) { Text(s.label) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPanel(current: Int?, enabled: Boolean, onApply: (Int) -> Unit) {
    PanelTitle("배터리 상태 직접 지정", "dumpsys battery set status")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 12.dp),
    ) {
        listOf(
            BatteryCommands.STATUS_CHARGING,
            BatteryCommands.STATUS_DISCHARGING,
            BatteryCommands.STATUS_NOT_CHARGING,
            BatteryCommands.STATUS_FULL,
            BatteryCommands.STATUS_UNKNOWN,
        ).forEach { status ->
            FilterChip(
                selected = current == status,
                onClick = { onApply(status) },
                label = { Text(BatteryReader.statusLabel(status)) },
                enabled = enabled,
            )
        }
    }
}

@Composable
fun AdvancedPanel(
    ui: UiState,
    enabled: Boolean,
    onApplyTemperature: (Int) -> Unit,
    onApplyPresent: (Boolean) -> Unit,
    onApplyInvalidCharger: (Boolean) -> Unit,
) {
    // 슬라이더는 0.1℃ 단위 값을 그대로 다룬다.
    var temp by rememberSaveable {
        mutableFloatStateOf(
            (ui.dumpsys?.temperature ?: ui.battery.temperatureC?.let { (it * 10).toInt() } ?: 250)
                .coerceIn(BatteryCommands.TEMP_MIN, BatteryCommands.TEMP_MAX)
                .toFloat()
        )
    }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val present = ui.dumpsys?.present ?: true

    PanelTitle("온도·기타", "기기·Android 버전에 따라 지원되지 않는 항목이 있을 수 있습니다.")
    Text(
        "%.1f℃".format(temp.toInt() / 10f),
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        color = LobbyColors.Amber,
        modifier = Modifier.padding(top = 12.dp),
    )
    Slider(
        value = temp,
        onValueChange = { temp = (it / 5).toInt() * 5f },
        valueRange = BatteryCommands.TEMP_MIN.toFloat()..BatteryCommands.TEMP_MAX.toFloat(),
        enabled = enabled,
    )
    Button(
        onClick = { onApplyTemperature(temp.toInt()) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("온도 적용", fontWeight = FontWeight.Bold) }

    HorizontalDivider(Modifier.padding(vertical = 12.dp))
    SwitchRow("배터리 장착됨으로 표시", "끄면 배터리가 없는 것처럼 표시 (set present)", present, enabled, onApplyPresent)
    SwitchRow("충전기 비정상으로 표시", "호환되지 않는 충전기 경고 (set invalid)", invalid, enabled) {
        invalid = it
        onApplyInvalidCharger(it)
    }
}

@Composable
fun InfoPanel(ui: UiState) {
    val b = ui.battery
    PanelTitle("배터리 정보")
    Column(Modifier.padding(top = 8.dp)) {
        InfoRow("실제 잔량 (HAL)", b.actualLevel?.let { "$it%" } ?: "-")
        InfoRow("실제 상태 (HAL)", BatteryReader.statusLabel(b.actualStatus))
        InfoRow("표시 잔량 (시스템)", b.reportedLevel?.let { "$it%" } ?: "-")
        InfoRow("표시 상태 (시스템)", BatteryReader.statusLabel(b.reportedStatus))
        InfoRow("표시 전원", BatteryReader.pluggedLabel(b.reportedPlugged))
        b.temperatureC?.let { InfoRow("온도", "%.1f℃".format(it)) }
        b.voltageMv?.let { InfoRow("전압", "$it mV") }
        ui.dumpsys?.let { InfoRow("값 덮어쓰기", if (it.overridden) "적용 중" else "없음") }
    }
    ui.dumpsys?.let {
        Text(
            "dumpsys battery",
            style = MaterialTheme.typography.labelLarge,
            color = LobbyColors.TextDim,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        )
        SelectionContainer(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .background(LobbyColors.Navy, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(it.raw, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ShizukuPanel(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit,
    onDownload: () -> Unit,
    onOpenReleases: () -> Unit,
) {
    PanelTitle("Shizuku 연결")
    Column(Modifier.padding(top = 8.dp)) {
        InfoRow(
            "상태",
            when (state.status) {
                ShizukuStatus.NOT_RUNNING -> "실행 중 아님"
                ShizukuStatus.TOO_OLD -> "버전이 너무 낮음 (v11+)"
                ShizukuStatus.PERMISSION_REQUIRED -> "권한 필요"
                ShizukuStatus.PERMISSION_DENIED -> "권한 거부됨"
                ShizukuStatus.GRANTED -> "권한 허용됨"
            },
        )
        InfoRow("Shizuku 앱", if (state.managerInstalled) "설치됨" else "설치 안 됨")
        state.shizukuUid?.let { InfoRow("Shizuku 권한", uidLabel(it)) }
        InfoRow(
            "UserService 바인더",
            when (state.serviceStatus) {
                ServiceStatus.DISCONNECTED -> "연결 안 됨"
                ServiceStatus.CONNECTING -> "연결 중…"
                ServiceStatus.CONNECTED -> "연결됨" + (state.serviceUid?.let { " (${uidLabel(it)})" } ?: "")
            },
        )
    }
    if (state.status == ShizukuStatus.PERMISSION_REQUIRED) {
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Shizuku 권한 요청", fontWeight = FontWeight.Bold)
        }
    }
    if (state.managerInstalled) {
        OutlinedButton(onClick = onOpenShizuku, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Shizuku 앱 열기")
        }
    } else {
        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Shizuku 설치 (Play 스토어)", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = onOpenReleases, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("GitHub 에서 APK 받기")
        }
    }
    Text(
        "1. Shizuku 앱 설치 → 무선 디버깅(또는 adb/루트)으로 시작\n" +
            "2. 이 앱에서 권한 요청 → 허용\n" +
            "3. 바인더가 '연결됨' 이 되면 로비의 메뉴를 사용할 수 있습니다.",
        style = MaterialTheme.typography.bodySmall,
        color = LobbyColors.TextDim,
        modifier = Modifier.padding(top = 12.dp),
    )
}

private fun uidLabel(uid: Int) = when (uid) {
    0 -> "root"
    2000 -> "shell (adb)"
    else -> "uid $uid"
}
