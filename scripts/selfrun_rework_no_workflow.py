from pathlib import Path

root = Path('.')

# Remove private note text from the external share payload.
p = root / 'app/src/main/java/com/shaterguy/hankan/MainActivity.java'
s = p.read_text(encoding='utf-8')
s = s.replace('        String note = store.getNote(date).trim();\n\n', '')
s = s.replace('        if (!note.isEmpty()) summary.append("\\n메모: ").append(note);\n', '')
p.write_text(s, encoding='utf-8')

# Separate DEV identity and fail closed for distributable release signing.
(root / 'app/build.gradle.kts').write_text(r'''plugins {
    id("com.android.application")
}

val releaseStoreFile = providers.environmentVariable("HANKAN_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("HANKAN_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("HANKAN_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("HANKAN_RELEASE_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.shaterguy.hankan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shaterguy.hankan"
        minSdk = 26
        targetSdk = 35
        versionCode = 10100
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("hankanRelease") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "한칸 DEV")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("hankanRelease")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("unsignedRelease") {
            initWith(getByName("release"))
            signingConfig = null
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.configureEach {
    val releaseDistributionTask = name == "assembleRelease" ||
        name == "bundleRelease" || name.startsWith("packageRelease")
    if (releaseDistributionTask) {
        doFirst {
            check(releaseSigningReady) {
                "Release signing credentials are required. Refusing to create a distributable release with an ephemeral debug key."
            }
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
''', encoding='utf-8')

manifest = root / 'app/src/main/AndroidManifest.xml'
s = manifest.read_text(encoding='utf-8')
if 'android:dataExtractionRules' not in s:
    s = s.replace('        android:allowBackup="false"\n', '        android:allowBackup="false"\n        android:dataExtractionRules="@xml/data_extraction_rules"\n        android:fullBackupContent="@xml/backup_rules"\n')
manifest.write_text(s, encoding='utf-8')

xml = root / 'app/src/main/res/xml'
xml.mkdir(parents=True, exist_ok=True)
(xml / 'backup_rules.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<full-backup-content>\n    <exclude domain="sharedpref" path="." />\n</full-backup-content>\n''', encoding='utf-8')
(xml / 'data_extraction_rules.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>\n<data-extraction-rules>\n    <cloud-backup disableIfNoEncryptionCapabilities="true">\n        <exclude domain="sharedpref" path="." />\n    </cloud-backup>\n    <device-transfer>\n        <exclude domain="sharedpref" path="." />\n    </device-transfer>\n</data-extraction-rules>\n''', encoding='utf-8')

(root / 'SIGNING_CERT_SHA256.txt').write_text('fdf618e37cd539e1fc23ff442bcb3dc85c510923b7715fadd62237e10111262c\n', encoding='utf-8')
(root / '.gitignore').write_text('*.jks\n*.keystore\n*.p12\nsigning.properties\nlocal.properties\n', encoding='utf-8')

(root / 'README.md').write_text('''# 한칸\n\n오늘 필요한 한 칸만 정하고, 짧게 집중하고, 기록을 남기는 오프라인 Android 앱입니다.\n\n## 주요 기능\n\n- 오늘의 한 칸: 날짜별 목표 저장·완료·완료 취소\n- 집중: 10·25·50분 프리셋, 일시정지·재개, 앱 재시작 뒤 상태 복원\n- 기록: 최근 7일 완료 현황, 날짜별 작업·집중·메모, 누적 통계\n- 생각 보관함: 기기 내부에만 저장되는 날짜별 메모\n- 오늘의 작은 재미: 가족과 함께할 짧은 활동 추천\n- 홈 화면 위젯: 오늘 목표·연속 기록 확인 및 완료\n- 오늘 요약 공유: 작업과 집중 기록만 Android Sharesheet로 공유\n- 시스템 Light/Dark 및 넓은 화면 대응\n- 로그인, 광고, 분석 SDK, 네트워크 권한 없음\n\n## 설치 계보\n\n정식 앱 ID는 `com.shaterguy.hankan`입니다. 개발 검증용 debug 빌드는 `com.shaterguy.hankan.dev`로 분리되어 정식 앱과 동시에 설치할 수 있습니다.\n\n초기 1.0.0은 GitHub Actions의 임시 debug keystore로 배포되어 Actions 실행마다 인증서가 달라졌습니다. 따라서 1.0.0에서 1.1.0으로의 인플레이스 업데이트는 보장할 수 없습니다. 1.1.0부터는 별도 영속 정식 서명 계보를 사용하며 이후 정식 버전은 이 계보를 유지합니다.\n\n## 빌드\n\nJDK 17과 Gradle 8.10.2가 필요합니다.\n\n```bash\ngradle testDebugUnitTest testUnsignedReleaseUnitTest\ngradle assembleDebug assembleUnsignedRelease\n```\n\n`assembleRelease`는 영속 정식 서명 자격증명이 없으면 의도적으로 실패합니다. 배포 가능한 정식 APK를 ephemeral debug key로 만드는 fallback은 없습니다.\n\nCI의 `unsignedRelease` APK는 배포물이 아니라 동일 소스의 정식 패키지 구성을 검증하고 외부의 영속 키로 최종 서명하기 위한 중간 산출물입니다.\n\n## 개인정보·백업\n\n사용 기록과 메모는 앱 전용 `SharedPreferences`에만 저장됩니다. 메모는 공유 payload에 포함되지 않습니다. `allowBackup=false`와 Android 12+ data extraction rules를 함께 사용해 cloud backup과 device-to-device transfer에서 앱의 SharedPreferences를 제외합니다.\n\n## 서명 자산\n\n공개 저장소에는 정식 개인키나 비밀번호를 저장하지 않습니다. 저장소에는 최종 APK 검증용 공개 인증서 SHA-256 지문만 포함합니다.\n\n## 라이선스\n\nMIT\n''', encoding='utf-8')

# Cleanup helper scripts only; workflow files are cleaned up via the GitHub connector.
Path('scripts/selfrun_rework.py').unlink(missing_ok=True)
Path('scripts/selfrun_rework_no_workflow.py').unlink(missing_ok=True)

assert '메모: ' not in p.read_text(encoding='utf-8')
assert 'signingConfig = signingConfigs.getByName("debug")' not in (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
