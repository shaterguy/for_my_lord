# 딱하나

오늘 꼭 끝낼 딱 하나를 정하고, 짧게 집중하고, 기록을 남기는 오프라인 Android 앱입니다.

## 주요 기능

- 오늘의 딱 하나: 날짜별 목표 저장·수정·완료·완료 취소
- 집중: 10·25·50분 프리셋, 일시정지·재개, 앱 재시작 뒤 상태 복원
- 기록: 최근 7일 완료 현황, 날짜별 작업·집중·메모, 누적 통계
- 생각 보관함: 기기 내부에만 저장되는 날짜별 메모
- 오늘의 작은 재미: 가족과 함께할 짧은 활동 추천
- 홈 화면 위젯: 오늘 목표·연속 기록 확인 및 완료
- 오늘 요약 공유: 작업과 집중 기록만 Android Sharesheet로 공유
- 시스템 Light/Dark, 회전 및 넓은 화면 대응
- 로그인·광고·분석 SDK·네트워크 권한 없음

## 설치 계보

정식 앱 ID는 `com.shaterguy.ddakhana`, DEV 앱 ID는 `com.shaterguy.ddakhana.dev`입니다. 기존 `한칸` 앱과는 별개의 신규 앱이며 서로 동시에 설치할 수 있습니다.

정식과 DEV는 각각 별도의 영속 RSA-4096 서명 계보를 사용합니다. 공개 저장소에는 개인키나 비밀번호를 넣지 않고 공개 인증서 SHA-256 지문만 둡니다.

## 빌드

JDK 17과 Gradle 8.10.2가 필요합니다.

```bash
gradle testUnsignedDevUnitTest testUnsignedReleaseUnitTest
gradle assembleUnsignedDev assembleUnsignedRelease
```

`unsignedDev`와 `unsignedRelease`는 외부 영속 키로 서명하기 위한 검증용 중간 산출물입니다. `assembleDev`와 `assembleRelease`는 각 영속 서명 자격증명이 없으면 의도적으로 실패합니다.

## 개인정보·백업

사용 기록과 메모는 앱 전용 `SharedPreferences`에 저장됩니다. 메모는 공유 payload에 포함되지 않습니다. 앱 데이터의 cloud backup과 device-to-device transfer도 제외합니다.

## 지원

Android 8.0(API 26) 이상을 지원합니다.

## 라이선스

개인 프로젝트 용도로 제작되었습니다.
