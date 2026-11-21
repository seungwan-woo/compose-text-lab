# AnnotatedString과 Spannable

**Rich text** 탭은 영어, 한국어, 아랍어와 이모지가 섞인 하나의 샘플을 양쪽에서 렌더링합니다. **Side by side**로 비교하고, 기존 설정으로 테마·글꼴·미리보기 fontScale을 바꿀 수 있습니다. 각 패널은 첫 기준선, 줄 수, 텍스트 영역 높이를 px로 표시합니다.

| 기능 | TextView | Compose |
| --- | --- | --- |
| 부분 색상·굵기 | `ForegroundColorSpan`, `StyleSpan` | `SpanStyle(color, fontWeight)` |
| 부분 배경·밑줄 | `BackgroundColorSpan`, `UnderlineSpan` | `SpanStyle(background, textDecoration)` |
| URL과 앱 내부 동작 | `URLSpan`, `ClickableSpan`, `LinkMovementMethod` | `LinkAnnotation.Url`, `LinkAnnotation.Clickable` |
| 줄 안 배지 | `ReplacementSpan.getSize()`와 `draw()` | `InlineTextContent`와 `Placeholder` |
| 문단 스타일 | `ParagraphStyle` 계열 Span | `ParagraphStyle` 범위 |
| 범위 편집 | `SpannableStringBuilder`와 Span 경계 플래그 | 상태를 갱신해 새 `AnnotatedString` 생성 |

**Android docs**와 **Details**를 누르면 각 패널이 URL 또는 클릭 이벤트를 표시합니다. URL 클릭은 이동할 주소를 기록하므로 외부 브라우저 없이 같은 화면에서 비교할 수 있습니다.

앱은 중립적인 텍스트 조각을 두 변환기에 전달합니다. 일반 스타일은 범위별로 매핑할 수 있지만, 커스텀 `ReplacementSpan`과 `MetricAffectingSpan`의 측정·그리기 동작은 `AnnotatedString`에 복사할 수 없습니다. 배지·아이콘은 인라인 Composable로 재구현할 수 있고, 글꼴 메트릭을 바꾸거나 글리프를 직접 그리는 Span은 필요한 동작을 별도로 분류해야 합니다. 샘플의 원형 배지는 두 경로의 구현을 비교하며, 임의의 Span 변환을 제공하지 않습니다.

CI는 동일 문자열, 스타일과 링크 범위, 인라인 배지 레이아웃, fontScale 1.6에서 두 URL·클릭 동작을 확인하고 화면을 캡처합니다. TalkBack 탐색, OEM 시스템 폰트와 FlipFont, Span 범위의 실시간 삽입·삭제, 픽셀 허용 오차 비교는 별도 검증 항목입니다.

앱은 기존 BOM `2025.12.00`의 Compose UI/Foundation `1.10.0`과 AGP `8.13.2`를 사용합니다. `LinkAnnotation`은 이 조합에서 사용할 수 있습니다. Compose `1.13.0`은 별도의 도구 호환성 검증 후 업그레이드할 수 있습니다.

- [Compose 텍스트 상호작용](https://developer.android.com/develop/ui/compose/text/user-interactions)
- [AnnotatedString.Builder](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString.Builder)
- [ReplacementSpan](https://developer.android.com/reference/android/text/style/ReplacementSpan)
- [Compose UI 릴리스 노트](https://developer.android.com/jetpack/androidx/releases/compose-ui)
