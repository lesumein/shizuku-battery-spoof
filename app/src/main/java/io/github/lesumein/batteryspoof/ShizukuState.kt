package io.github.lesumein.batteryspoof

enum class ShizukuStatus {
    /** Shizuku 바인더를 받지 못함 (Shizuku 미설치 또는 미실행) */
    NOT_RUNNING,
    /** Shizuku 버전이 너무 낮음 (v11 미만) */
    TOO_OLD,
    /** 실행 중이지만 권한이 없음 */
    PERMISSION_REQUIRED,
    /** 사용자가 권한을 거부하고 '다시 묻지 않음'을 선택함 */
    PERMISSION_DENIED,
    /** 권한 허용됨 */
    GRANTED,
}

enum class ServiceStatus { DISCONNECTED, CONNECTING, CONNECTED }

data class ShizukuState(
    val status: ShizukuStatus = ShizukuStatus.NOT_RUNNING,
    val shizukuUid: Int? = null,
    val serviceStatus: ServiceStatus = ServiceStatus.DISCONNECTED,
    val serviceUid: Int? = null,
)
