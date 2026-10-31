# Shizuku Battery Spoof

[Shizuku](https://shizuku.rikka.app/) 의 shell 권한을 이용해 **시스템에 표시되는 배터리 수치**(잔량, 충전 상태)를 바꾸는 Android 앱입니다.
내부적으로 `dumpsys battery` 명령을 사용하므로 실제 배터리에는 아무 영향이 없고, **복구(reset)** 하거나 재부팅하면 원래 값으로 돌아옵니다.

## 기능

- Shizuku 실행 여부 / 권한 / UserService 바인더 연결 상태 표시, 권한 요청
- 현재 배터리 정보 표시
  - **실제 값**: `BatteryManager.getIntProperty()` (HAL 에서 직접 읽으므로 스푸핑 영향 없음)
  - **표시 값**: `ACTION_BATTERY_CHANGED` 브로드캐스트 (다른 앱·상태바가 보는 값)
  - `dumpsys battery` 원본 출력과 덮어쓰기 적용 여부
- 잔량 슬라이더(0–100) + 적용 → `dumpsys battery set level <n>`
- 충전 중 표시 토글 (AC / USB / 무선 선택)
  - 켜기 → `dumpsys battery set ac|usb|wireless 1` (나머지는 0) + `set status 2`
  - 끄기 → `dumpsys battery unplug` + `set status 3`
- 배터리 상태 직접 지정 (충전 중 / 방전 중 / 충전 안 함 / 완충) → `dumpsys battery set status <n>`
- 추가 항목 (기기·버전에 따라 미지원일 수 있음)
  - 온도 → `dumpsys battery set temp <0.1℃ 단위>` (-20.0℃ ~ 90.0℃)
  - 배터리 장착 여부 → `dumpsys battery set present <0|1>`
  - 충전기 비정상 표시 → `dumpsys battery set invalid <0|1>`
- **원래대로 복구** → `dumpsys battery reset`

## 화면 (로비형 UI)

게임 로비처럼 한 화면에서 상태를 보고, 하단 메뉴 타일을 눌러 각 기능 패널(바텀시트)을 엽니다.

- 배경: Gemini 이미지 모델(`gemini-3-pro-image`)로 생성한 에너지 코어 챔버 이미지 (`res/drawable-nodpi/lobby_bg.webp`)
- 상단 HUD: Shizuku 연결 상태 배지(ONLINE / 권한 필요 / OFFLINE), 새로고침
- 중앙 코어: 표시 잔량(바깥 호·큰 숫자)과 실제 잔량(안쪽 가는 호), 충전 시 빠르게 도는 링, 덮어쓰기 중이면 `OVERRIDE` 배지
- 하단 메뉴: 잔량 · 충전 · 상태 · 온도·기타 · 정보 · 복구(확인 후 reset)

## 동작 방식

명령은 [Shizuku UserService](https://github.com/RikkaApps/Shizuku-API#userservice)(AIDL)로 실행합니다.
Shizuku 가 앱의 `BatteryUserService` 를 shell(uid 2000) 또는 root 권한의 별도 프로세스로 띄우고,
앱은 AIDL 인터페이스(`IBatteryService`)로 그 프로세스에 `dumpsys battery ...` 실행을 요청합니다.
숨겨진 API 인 `Shizuku.newProcess` 는 사용하지 않습니다.
UserService 쪽에서도 인자를 화이트리스트로 검증해 `dumpsys battery` 의 위 하위 명령만 실행합니다.

## 빌드

요구 사항: JDK 17 이상(21 권장), Android SDK (compileSdk 37)

```bash
# ANDROID_HOME 을 설정하거나 local.properties 에 sdk.dir 을 지정
./gradlew assembleDebug
# 결과물: app/build/outputs/apk/debug/app-debug.apk
```

Android Studio 에서 프로젝트를 열어 바로 실행해도 됩니다.

GitHub Actions(`.github/workflows/build.yml`)가 push 마다 debug APK 를 빌드합니다.
Actions 탭의 해당 실행에서 `app-debug` 아티팩트를 내려받을 수 있습니다.

| 항목 | 버전 |
| --- | --- |
| AGP | 9.5.0 (내장 Kotlin 지원) |
| Kotlin / Compose 컴파일러 | 2.4.21 |
| Gradle | 9.8.2 |
| Compose BOM | 2026.09.00 |
| Shizuku API | 13.1.5 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 |

## 사용법

1. **Shizuku 설치** — [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) 또는 [GitHub 릴리스](https://github.com/RikkaApps/Shizuku/releases)에서 설치합니다.
2. **Shizuku 실행** — Shizuku 앱에서 아래 중 하나로 시작합니다.
   - Android 11 이상: 개발자 옵션 → *무선 디버깅* 을 켠 뒤 Shizuku 앱의 "무선 디버깅으로 시작" (페어링 필요)
   - PC 연결: `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh` (Shizuku 앱에 표시되는 명령 사용)
   - 루팅 기기: "시작(루트)"
   - 재부팅하면 Shizuku 가 꺼지므로 다시 시작해야 합니다(루트 제외).
3. **이 앱 설치 후 실행** — Shizuku 카드에 "실행 중 · 권한 필요" 가 보이면 **Shizuku 권한 요청** 을 눌러 허용합니다.
   (실수로 거부했다면 Shizuku 앱 → 앱 관리에서 직접 허용)
4. 권한이 허용되면 UserService 가 자동으로 연결되고 "연결됨 (shell (adb))" 처럼 표시됩니다.
5. 슬라이더로 잔량을 고른 뒤 **잔량 적용**, 충전 표시 토글 등을 사용합니다.
6. 끝나면 **원래대로 복구 (reset)** 를 눌러 실제 값으로 되돌립니다.

## 주의

- 덮어쓰기가 적용된 동안에는 시스템이 실제 배터리 변화를 반영하지 않습니다(`UPDATES STOPPED`).
  배터리 부족 경고, 절전 모드 자동 전환 등이 실제 상태와 다르게 동작할 수 있으니 사용 후 반드시 복구하세요.
- 재부팅하면 덮어쓰기는 자동으로 해제됩니다.
- 일부 제조사 ROM 은 상태바·설정 화면이 별도 경로로 배터리 값을 읽어 변경이 반영되지 않을 수 있습니다.
