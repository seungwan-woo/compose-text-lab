# Compose Text Lab

Android 실험 앱: 같은 문자열과 설정으로 **Jetpack Compose Text ↔ Android TextView**를 관찰합니다. 입력과 선택 메뉴는 BasicTextField ↔ EditText, SelectionContainer ↔ selectable TextView로 비교합니다.

이 저장소는 이전 ChatGPT 대화에 기록된 v0.2 기능 범위를 재구현한 것입니다. 당시 sandbox ZIP의 원본 파일은 이 작업 환경에서 복구하지 못했습니다. 특정 두 엔진이 픽셀 단위로 같거나 OEM에서 같은 경로를 사용한다고 가정하지 않습니다.

## 실행 / APK

1. [Actions](https://github.com/seungwan-woo/compose-text-lab/actions/workflows/android.yml)에서 최신 성공한 **Android debug APK** 실행을 엽니다.
2. **Artifacts → compose-text-lab-debug-<commit>** ZIP을 다운로드하여 APK를 설치합니다. GitHub 로그인과 유효한 보존 기간(14일)이 필요합니다.
3. 또는 Android Studio에서 프로젝트를 열거나 JDK 17과 Android SDK 36을 준비한 뒤 실행합니다.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 연결된 단말/에뮬레이터에서 UI 통합 검증
./gradlew :app:connectedDebugAndroidTest
```

Windows에서는 `gradlew.bat`를 사용합니다. SDK는 `ANDROID_HOME` 또는 커밋하지 않는 `local.properties`의 `sdk.dir`로 지정합니다.

고정 재현 기준: AGP **8.13.2**, Kotlin/Compose compiler **2.2.21**, Compose BOM **2025.12.00**, Gradle **8.13**, JDK **17**, compile/target SDK **36**, min SDK **26**. 최신 버전이라는 의미가 아닙니다. SESL을 포함하지 않는 AndroidX/Framework 기준 앱입니다.

## 실험

| 화면 | 지원 항목 |
|---|---|
| Render | 좌우/상하 배치, 같은 문자열, fontSize/weight/letterSpacing/lineHeight, includeFontPadding, maxLines/ellipsis, 패딩, 기준선, 고정 실험 너비 |
| Theme | System/Light/Dark/Dynamic, 명시적 공통 스타일과 별도의 테마 기본 텍스트 비교 |
| Font | System/Sans/Serif/Monospace, 시스템 문서 선택기를 통한 TTF/OTF 가져오기 및 복원 |
| Font scale | 실제 시스템 값 추종 또는 미리보기 한정 0.8–2.0 Configuration override; 시스템 화면 설정 바로가기 |
| Effects | None/Shadow/Gradient/Stroke/Marquee, 동일 글꼴/설정으로 비교 |
| Selection | SelectionContainer, BasicTextField, TextView, EditText, View 커스텀 ActionMode 메뉴, 선택 offset/ActionMode 이벤트 로그 |
| Rich text | AnnotatedString ↔ Spannable, 부분 스타일, URL·커스텀 클릭, InlineTextContent ↔ ReplacementSpan, 기준선·높이 |
| Report | 실제 레이아웃/위젯 px 치수, 줄 수, 첫/마지막 baseline, ellipsis/overflow, 단말/설정/폰트 식별자; JSON 복사/파일 저장 |

Render, Effects, Selection 상단의 **Side by side**를 켜면 Compose는 왼쪽, TextView는 오른쪽에 같은 너비로 표시됩니다. **Requested panel width**로 패널의 최대 너비를 정할 수 있습니다. 좁은 화면에서 한 줄에 더 많은 글자를 보려면 기기를 가로로 돌리거나 상하 모드를 사용하세요. 입력 프로브의 편집 값은 독립적이며 sample text 변경 시 초기화됩니다.

두 엔진은 같은 굵기로 해석한 Android Typeface를 사용합니다. API 26–27에서는 NORMAL/BOLD로 정규화되며 100–900 가중치를 각각 재현하지 못합니다. Marquee에서는 두 엔진에 같은 한 줄 문자열(개행을 공백으로 변환)을 전달하며, 스크롤되는 텍스트 레이아웃 폭과 실제 표시 영역 폭을 따로 보고합니다. 입력 내용/선택은 탭·스타일·테마 변경 및 화면 재생성 시 유지됩니다.

TTF/OTF는 앱 내부에 복사하여 재실행 시 복원합니다. 20 MiB 제한과 컨테이너/플랫폼 파서 검증을 적용합니다. 시스템 전체 폰트를 변경하지 않으며 저장소 권한이나 네트워크 권한이 필요하지 않습니다. 가져오기 취소/실패 시 기존 글꼴을 유지합니다. Report에는 입력한 sample 문자열이 포함되므로 공유 전에 확인하세요.

## 측정 해석

- **Paragraph direction**은 두 엔진에 공통으로 적용됩니다. 기본값 `FirstStrong`은 문단에 처음 나타나는 강한 방향의 문자를 따릅니다. `LTR`과 `RTL`은 문단의 기본 방향을 고정하되, 문단 안의 아랍어와 라틴 문자 배열에는 여전히 Unicode 양방향 규칙이 적용됩니다. 테마 기본 프로브에는 이 설정을 적용하지 않습니다.
- 공통 스타일 프로브에는 대응 속성을 명시합니다. 테마 기본 프로브는 Material 3 Typography와 Framework textAppearance의 실제 기본값을 별도로 보여주며, 이 둘의 일치를 전제로 하지 않습니다.
- fontScale override는 미리보기 전용입니다. Android Configuration에서 파생한 Context와 Compose Density를 함께 사용하며, Android 14+의 비선형 SP 변환을 단순 `sp × scaledDensity` 계산으로 대체하지 않습니다. 시스템 변경 검증에는 **Follow system fontScale**을 사용합니다.
- baseline은 **각 프로브 위젯 상단에서의 px 거리**입니다. Compose `TextLayoutResult`에는 바깥 padding이 없으므로 보고 시 padding을 더합니다. TextView는 `baseline`, `totalPaddingTop`과 Layout의 baseline을 사용합니다. font padding은 엔진 자체의 정책입니다.
- Compose lineHeight와 TextView `setLineSpacing`은 정책이 다릅니다. View에는 요청 lineHeight와 `Paint.fontMetrics` 높이 차이를 extra spacing으로 적용하며 마지막 줄 아래 여백은 동일하지 않을 수 있습니다. 실제 결과를 확인하세요.
- Layout 줄 수와 위젯 높이, ellipsis와 overflow는 구분합니다. View의 숨겨진 원문 전체 줄 수를 알아낸다고 주장하지 않습니다. 효과의 그림자/선 외곽 bounds는 레이아웃 bounds와 다릅니다.
- FontScale/테마/문자열/폰트/실험 변경 시 이전 측정은 버립니다. 표시하지 않은 화면의 측정값은 생성하지 않습니다. 선택/입력 프로브는 별도이며 렌더링 수치 보고 대상은 아닙니다.
- 이 앱은 자동 픽셀 diff, 성능 벤치마크, glyph ink bounds 측정, Text Motion 계측, Compose 커스텀 ContextMenu provider를 구현하지 않습니다.

## Floating toolbar / One UI

일반 공개 SDK 앱으로 텍스트 선택 메뉴를 관찰합니다. `HorizontalFloatingToolbar` 위젯 실험이 아닙니다. RemoteFloatingToolbarPopup/SelectionToolbarService 등 내부 시스템 경로를 강제로 켜거나 검출하지 않습니다. 실제 원격 렌더링 여부는 OEM Framework 로그/추적과 함께 별도 검증해야 합니다.

자세한 API 대응, 제약 및 단말 검증표는 [effects-selection-toolbar.md](docs/effects-selection-toolbar.md)에 있습니다.

Rich text 탭과 자동 검증의 범위는 [AnnotatedString과 Spannable](docs/rich-text.md)에 정리했습니다. CI의 `rich-text-comparison` 아티팩트에서 비교 화면을 확인할 수 있습니다.

## 검증 범위

CI는 JVM 단위 테스트, Android Lint, debug APK 및 Android 테스트 APK 빌드와 **API 37.2 에뮬레이터 테스트**(Google APIs x86_64, 16 KB 이미지)를 실행합니다. `compileSdk`와 `targetSdk`는 36이며, 보고서에는 부팅한 에뮬레이터에서 읽은 API 버전을 표시합니다. PR에서는 `android-results-page`와 `rich-text-comparison` 아티팩트를 내려받아 결과를 볼 수 있습니다. `main`에서 성공한 결과는 [GitHub Pages](https://seungwan-woo.github.io/compose-text-lab/)에 게시합니다. 저장소의 Settings → Pages → Source는 **GitHub Actions**로 설정해야 합니다. 에뮬레이터 성공은 Samsung One UI 단말의 toolbar, FlipFont, IME 또는 시스템 서비스 동작을 보증하지 않습니다.

선택 메뉴는 터치 길게 누르기와 마우스 우클릭을 별도로 검증합니다. Overflow는 한 줄에 들어가는 텍스트, 끝 말줄임, 말줄임 없는 잘라내기를 검증합니다. Marquee는 두 프레임을 비교해 양쪽 텍스트가 실제로 움직이는지 검사합니다. 두 엔진 모두 지원하지만 시작 조건과 속도 설정은 다릅니다. [Compose 전용 텍스트 API와 View 대안](docs/effects-selection-toolbar.md#compose-텍스트-api와-view-대안)을 참고하세요.

로컬 검증은 **단위 테스트 8개 + API 36 통합 테스트 5개 통과**, **Lint 오류 0개(경고 17개)**입니다. 세부 범위와 제한은 [검증 기록](docs/validation.md)에 있습니다.
