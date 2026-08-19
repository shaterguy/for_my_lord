# 한칸

하루를 거창하게 관리하지 않고, 오늘 필요한 한 칸만 채우는 오프라인 Android 앱입니다.

## 기능

- 오늘 끝낼 한 가지 저장 및 완료
- 날짜 기반 연속 달성 기록
- 일시정지와 재개가 가능한 10분 집중 타이머
- 기기에만 남는 빠른 메모
- 가족과 함께할 짧은 활동 추천
- 로그인, 광고, 분석 SDK, 네트워크 권한 없음

## 설치

GitHub Releases에서 최신 `hankan-v1.0.0.apk`를 내려받아 Android 기기에 설치합니다. Android 8.0(API 26) 이상을 지원합니다.

## 빌드

JDK 17과 Gradle 8.10.2가 필요합니다.

```bash
gradle testReleaseUnitTest
gradle assembleRelease
```

릴리즈 APK는 `app/build/outputs/apk/release/app-release.apk`에 생성됩니다.

## 서명

현재 개인 배포판은 설치 가능한 APK 제공을 위해 Android 기본 debug signingConfig를 사용합니다. 앱 데이터는 외부로 전송되지 않습니다.

## 라이선스

MIT
