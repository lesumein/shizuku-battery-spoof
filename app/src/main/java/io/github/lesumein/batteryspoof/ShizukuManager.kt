package io.github.lesumein.batteryspoof

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import rikka.shizuku.Shizuku

/** Shizuku 바인더 상태, 권한, UserService 연결을 관리한다. */
class ShizukuManager(private val packageName: String) {

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    @Volatile
    var service: IBatteryService? = null
        private set

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(packageName, BatteryUserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("battery")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) {
                onServiceDisconnected(name)
                return
            }
            val s = IBatteryService.Stub.asInterface(binder)
            service = s
            val uid = runCatching { s.uid }.getOrNull()
            _state.update { it.copy(serviceStatus = ServiceStatus.CONNECTED, serviceUid = uid) }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            _state.update { it.copy(serviceStatus = ServiceStatus.DISCONNECTED, serviceUid = null) }
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        service = null
        _state.value = ShizukuState()
    }
    private val permissionResult =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_CODE) refresh()
        }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        unbindService()
    }

    fun refresh() {
        val status = currentStatus()
        val uid = if (status == ShizukuStatus.NOT_RUNNING) null else runCatching { Shizuku.getUid() }.getOrNull()
        _state.update { it.copy(status = status, shizukuUid = uid) }
        if (status == ShizukuStatus.GRANTED) bindService()
    }

    fun requestPermission() {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun currentStatus(): ShizukuStatus = try {
        when {
            !Shizuku.pingBinder() -> ShizukuStatus.NOT_RUNNING
            Shizuku.isPreV11() -> ShizukuStatus.TOO_OLD
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuStatus.GRANTED
            Shizuku.shouldShowRequestPermissionRationale() -> ShizukuStatus.PERMISSION_DENIED
            else -> ShizukuStatus.PERMISSION_REQUIRED
        }
    } catch (e: Exception) {
        Log.w(TAG, "Shizuku 상태 확인 실패", e)
        ShizukuStatus.NOT_RUNNING
    }

    private fun bindService() {
        if (_state.value.serviceStatus != ServiceStatus.DISCONNECTED) return
        _state.update { it.copy(serviceStatus = ServiceStatus.CONNECTING) }
        try {
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (e: Exception) {
            Log.w(TAG, "UserService 바인딩 실패", e)
            _state.update { it.copy(serviceStatus = ServiceStatus.DISCONNECTED) }
        }
    }

    private fun unbindService() {
        if (service == null && _state.value.serviceStatus == ServiceStatus.DISCONNECTED) return
        runCatching { Shizuku.unbindUserService(userServiceArgs, connection, true) }
        service = null
        _state.update { it.copy(serviceStatus = ServiceStatus.DISCONNECTED, serviceUid = null) }
    }

    companion object {
        private const val TAG = "ShizukuManager"
        private const val REQUEST_CODE = 1001
    }
}
