# 한칸

오늘 필요한 한 칸만 정하고, 짧게 집중하고, 기록을 남기는 오프라인 Android 앱입니다.

## 주요 기능

- 오늘의 한 칸: 날짜별 목표 저장·완료·완료 취소
- 집중: 10·25·50분 프리셋, 일시정지·재개, 앱 재시작 뒤 상태 복원
- 기록: 최근 7일 완료 현황, 날짜별 작업·집중·메모, 누적 통계
- 생각 보관함: 기기 내부에만 저장되는 날짜별 메모
- 오늘의 작은 재미: 가족과 함께할 짧은 활동 추천
- 홈 화면 위젯: 오늘 목표·연속 기록 확인 및 완료
- 오늘 요약 공유: 작업과 집중 기록만 Android Sharesheet로 공유
- 시스템 Light/Dark 및 넓은 화면 대응
- 로그인, 광고, 분석 SDK, 네트워크 권한 없음

## 설치 계보

정식 앱 ID는 `com.shaterguy.hankan`입니다. 개발 검증용 debug 빌드는 `com.shaterguy.hankan.dev`로 분리되어 정식 앱과 동시에 설치할 수 있습니다.

초기 1.0.0은 GitHub Actions의 임시 debug keystore로 배포되어 Actions 실행마다 인증서가 달라졌습니다. 따라서 1.0.0에서 1.1.0으로의 인플레이스 업데이트는 보장할 수 없습니다. 1.1.0부터는 별도 영속 정식 서명 계보를 사용하며 이후 정식 버전은 이 계보를 유지합니다.

## 빌드

JDK 17과 Gradle 8.10.2가 필요합니다.

```bash
gradle testDebugUnitTest testUnsignedReleaseUnitTest
gradle assembleDebug assembleUnsignedRelease
```

`assembleRelease`는 영속 정식 서명 자격증명이 없으면 의도적으로 실패합니다. 배포 가능한 정식 APK를 ephemeral debug key로 만드는 fallback은 없습니다.

CI의 `unsignedRelease` APK는 배포물이 아니라 동일 소스의 정식 패키지 구성을 검증하고 외부의 영속 키로 최종 서명하기 위한 중간 산출물입니다.

## 개인정보·백업

사용 기록과 메모는 앱 전용 `SharedPreferences`에만 저장됩니다. 메모는 공유 payload에 포함되지 않습니다. `allowBackup=false`와 Android 12+ data extraction rules를 함께 사용해 cloud backup과 device-to-device transfer에서 앱의 SharedPreferences를 제외합니다.

## 서명 자산

공개 저장소에는 정식 개인키나 비밀번호를 저장하지 않습니다. 저장소에는 최종 APK 검증용 공개 인증서 SHA-256 지문만 포함합니다.

## 라이선스

MIT
