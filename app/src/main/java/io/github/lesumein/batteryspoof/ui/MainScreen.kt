package io.github.lesumein.batteryspoof.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.lesumein.batteryspoof.BatteryReader
import io.github.lesumein.batteryspoof.MainViewModel
import io.github.lesumein.batteryspoof.R
import io.github.lesumein.batteryspoof.ServiceStatus
import io.github.lesumein.batteryspoof.ShizukuState
import io.github.lesumein.batteryspoof.ShizukuStatus

/** 로비에서 열 수 있는 패널 */
enum class Panel { LEVEL, CHARGING, STATUS, ADVANCED, INFO, SHIZUKU }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val shizuku by vm.shizukuState.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var panel by rememberSaveable { mutableStateOf<Panel?>(null) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshAll() }
    LaunchedEffect(ui.lastMessage) {
        ui.lastMessage?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val ready = shizuku.serviceStatus == ServiceStatus.CONNECTED
    val enabled = ready && !ui.busy
    val dump = ui.dumpsys
    val charging = dump?.let { it.acPowered == true || it.usbPowered == true || it.wirelessPowered == true }
        ?: (ui.battery.reportedPlugged != 0)

    Box(Modifier.fillMaxSize().background(LobbyColors.Navy)) {
        Image(
            painter = painterResource(R.drawable.lobby_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 위·아래 UI 가독성을 위한 그라데이션
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.65f),
                        0.22f to Color.Transparent,
                        0.6f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.8f),
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LobbyHud(shizuku, onShizukuClick = { panel = Panel.SHIZUKU }, onRefresh = vm::refreshAll)

            Spacer(Modifier.weight(1f))

            BatteryCore(
                displayLevel = dump?.level ?: ui.battery.reportedLevel,
                actualLevel = ui.battery.actualLevel,
                charging = charging,
                overridden = dump?.overridden == true,
                modifier = Modifier.fillMaxWidth(0.72f).widthIn(max = 320.dp),
            )
            Text(
                "${BatteryReader.statusLabel(dump?.status ?: ui.battery.reportedStatus)} · " +
                    BatteryReader.pluggedLabel(ui.battery.reportedPlugged),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.weight(1f))

            if (!ready) {
                ConnectBanner(shizuku, onRequest = vm::requestPermission, onOpen = { panel = Panel.SHIZUKU })
                Spacer(Modifier.height(12.dp))
            }

            MenuGrid(
                enabled = enabled,
                level = dump?.level ?: ui.battery.reportedLevel,
                charging = charging,
                status = BatteryReader.statusLabel(dump?.status ?: ui.battery.reportedStatus),
                temperature = (dump?.temperature?.let { it / 10f } ?: ui.battery.temperatureC),
                overridden = dump?.overridden == true,
                onOpen = { panel = it },
                onReset = { confirmReset = true },
            )
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp),
        )
    }

    panel?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { panel = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                when (current) {
                    Panel.LEVEL -> LevelPanel(ui, enabled, vm::applyLevel)
                    Panel.CHARGING -> ChargingPanel(charging, enabled, vm::applyCharging)
                    Panel.STATUS -> StatusPanel(dump?.status ?: ui.battery.reportedStatus, enabled, vm::applyStatus)
                    Panel.ADVANCED -> AdvancedPanel(
                        ui, enabled, vm::applyTemperature, vm::applyPresent, vm::applyInvalidCharger,
                    )
                    Panel.INFO -> InfoPanel(ui)
                    Panel.SHIZUKU -> ShizukuPanel(shizuku, vm::requestPermission)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("원래대로 복구") },
            text = { Text("잔량, 충전, 상태, 온도 등 바꾼 모든 표시 값을 실제 값으로 되돌립니다.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    vm.reset()
                }) { Text("복구", color = LobbyColors.Danger) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun LobbyHud(state: ShizukuState, onShizukuClick: () -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "BATTERY LAB",
                color = LobbyColors.Cyan,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
            )
            Text("배터리 표시 변경", color = LobbyColors.TextDim, style = MaterialTheme.typography.labelMedium)
        }
        GlassPanel(onClick = onShizukuClick) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val (color, label) = shizukuBadge(state)
                GlowDot(color, pulse = state.serviceStatus == ServiceStatus.CONNECTING)
                Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
        Spacer(Modifier.padding(4.dp))
        GlassPanel(onClick = onRefresh) {
            Text("↻", color = LobbyColors.Cyan, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

fun shizukuBadge(state: ShizukuState): Pair<Color, String> = when {
    state.serviceStatus == ServiceStatus.CONNECTED -> LobbyColors.Lime to "ONLINE"
    state.serviceStatus == ServiceStatus.CONNECTING -> LobbyColors.Cyan to "연결 중"
    state.status == ShizukuStatus.GRANTED -> LobbyColors.Amber to "대기"
    state.status == ShizukuStatus.PERMISSION_REQUIRED || state.status == ShizukuStatus.PERMISSION_DENIED ->
        LobbyColors.Amber to "권한 필요"
    else -> LobbyColors.Danger to "OFFLINE"
}

@Composable
private fun ConnectBanner(state: ShizukuState, onRequest: () -> Unit, onOpen: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth(), borderColor = LobbyColors.Amber.copy(alpha = 0.6f)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Shizuku 연결 필요", color = LobbyColors.Amber, fontWeight = FontWeight.Bold)
                Text(
                    when (state.status) {
                        ShizukuStatus.NOT_RUNNING -> "Shizuku 앱을 설치하고 실행하세요."
                        ShizukuStatus.TOO_OLD -> "Shizuku v11 이상이 필요합니다."
                        ShizukuStatus.PERMISSION_REQUIRED -> "권한을 허용하면 바로 시작할 수 있어요."
                        ShizukuStatus.PERMISSION_DENIED -> "Shizuku 앱에서 이 앱의 권한을 허용하세요."
                        ShizukuStatus.GRANTED -> "서비스에 연결하는 중…"
                    },
                    color = LobbyColors.TextDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.status == ShizukuStatus.PERMISSION_REQUIRED) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = LobbyColors.Amber, contentColor = LobbyColors.Navy),
                ) { Text("권한 요청", fontWeight = FontWeight.Bold) }
            } else {
                TextButton(onClick = onOpen) { Text("자세히") }
            }
        }
    }
}

@Composable
private fun MenuGrid(
    enabled: Boolean,
    level: Int?,
    charging: Boolean,
    status: String,
    temperature: Float?,
    overridden: Boolean,
    onOpen: (Panel) -> Unit,
    onReset: () -> Unit,
) {
    val gap = 10.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            MenuTile("▮", "잔량", level?.let { "$it%" } ?: "-", Modifier.weight(1f),
                accent = levelColor(level), enabled = enabled) { onOpen(Panel.LEVEL) }
            MenuTile("⚡", "충전", if (charging) "충전 중" else "충전 안 함", Modifier.weight(1f),
                accent = LobbyColors.Lime, enabled = enabled) { onOpen(Panel.CHARGING) }
            MenuTile("◉", "상태", status, Modifier.weight(1f), enabled = enabled) { onOpen(Panel.STATUS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            MenuTile("✦", "온도·기타", temperature?.let { "%.1f℃".format(it) } ?: "-", Modifier.weight(1f),
                accent = LobbyColors.Amber, enabled = enabled) { onOpen(Panel.ADVANCED) }
            MenuTile("ⓘ", "정보", "실제 값·로그", Modifier.weight(1f)) { onOpen(Panel.INFO) }
            MenuTile("⟲", "복구", if (overridden) "적용 중" else "원래 값", Modifier.weight(1f),
                accent = LobbyColors.Danger, enabled = enabled, onClick = onReset)
        }
    }
}
