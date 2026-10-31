package io.github.lesumein.batteryspoof;

interface IBatteryService {
    // Shizuku 가 UserService 를 종료할 때 호출하는 예약된 트랜잭션 코드.
    void destroy() = 16777114;

    // Shizuku 사용자 서비스 프로세스(shell/root uid)에서 'dumpsys battery <args>' 를 실행한다.
    // 반환값: "<exitCode>\n<stdout+stderr>"
    String runBatteryCommand(in String[] args) = 1;

    int getUid() = 2;
}
