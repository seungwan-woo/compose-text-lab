# ICU · BiDi · 다국어 줄바꿈

**Lines** 탭은 한국어·영어 혼합, 일본어 괄호·문장부호, 아랍어·라틴 문자·숫자, 독일어 복합어, Emoji ZWJ를 제공합니다. **Sample locale**은 샘플에 맞는 언어를 적용합니다. **System locale** 또는 언어 태그를 선택해 같은 문장을 다른 언어 정책으로 비교할 수 있습니다.

**Maximum line panel width**로 양쪽의 최대 너비를 조절합니다. 창이 좁으면 실제 너비가 더 작아질 수 있으므로 화면과 JSON의 `layoutWidthPx`를 확인하세요. 태블릿에서는 설정을 열어 둔 채 좌우 비교를 볼 수 있습니다. 설정의 폰트, fontScale, 텍스트 크기, 패딩과 기준선 표시를 그대로 사용합니다. 20줄을 넘거나 폭을 초과하면 계측 화면에 잘린 범위를 표시합니다.

| 설정 | Compose | Android TextView |
| --- | --- | --- |
| 줄바꿈 전략 | `LineBreak.Strategy`의 Simple, HighQuality, Balanced | `breakStrategy` |
| 동아시아 금칙 규칙 | `LineBreak.Strictness`의 Loose, Normal, Strict | API 33+ `lineBreakStyle` |
| 어절 정책 | `LineBreak.WordBreak`의 Default, Phrase | API 33+ `lineBreakWordStyle`의 None, Phrase |
| 하이픈 허용 | `Hyphens.Auto` 또는 `None` | 현재 Compose Android 구현과 같은 `FULL_FAST` (33+) / `FULL` (26–32), 또는 `NONE` |
| 문단 방향 | `ContentOrLtr`, `ContentOrRtl`, `Ltr`, `Rtl` | 대응하는 `textDirection` 휴리스틱 |
| Locale | 단일 태그의 Compose `LocaleList` | `textLocale` |

API 26–32에서는 금칙 규칙과 Phrase 제어가 숨겨집니다. **Automatic hyphenation**은 하이픈 삽입을 허용하며 실제 결과는 전략과 언어 사전, 텍스트 폭에 따라 달라집니다. 예를 들어 독일어 샘플에 High quality를 선택한 다음 좁은 폭에서 스위치를 비교할 수 있습니다.

## 계측값

- `lines`는 렌더링된 각 줄의 논리적 UTF-16 범위 `[start, end)`, 문단 방향, 위젯 기준선과 수평 범위를 기록합니다. 서로 같은 줄 범위에서도 위치는 달라질 수 있습니다. 좌우 범위는 레이아웃 API가 반환하는 값이며 글리프 잉크 경계에는 별도 계측이 필요합니다.
- `carets`는 각 ICU grapheme 시작점에서 실제 레이아웃이 반환한 BiDi run 방향과 기본·보조 커서 x 좌표입니다. 혼합 방향 경계에서 두 위치가 갈릴 수 있습니다. 선택 핸들 제스처는 **Selection** 탭에서 검증할 수 있습니다.
- `icuBoundaries`는 단말의 ICU 버전과 grapheme·단어·줄바꿈 후보 오프셋을 기록합니다. 폭과 글꼴은 실제 렌더링의 줄 경계에 영향을 줍니다.
- **Copy line report**는 위 데이터와 선택한 정책, 실측 폭, 폰트·fontScale·단말 정보를 JSON으로 복사합니다. 양쪽 줄 범위 비교는 `matchingMeasuredLineRanges`, 전체 내용이 표시됐는지는 `completeTextVisible`을 확인하세요.

CI는 일본어 전략·Locale·금칙 규칙 적용, 독일어 하이픈 설정, Emoji 가족 시퀀스의 ICU 경계와 아랍어·숫자 BiDi를 확인합니다. 아랍어 샘플에서 원본 레이아웃 API와 JSON 줄 범위를 대조하고, 그 화면과 JSON을 Actions 아티팩트 및 GitHub Pages에 싣습니다. 개별 fallback 글꼴 식별, 픽셀 유사도(MSSIM), One UI 단말과 FlipFont, IME와 선택 핸들 실동작은 별도 기기 검증 항목입니다.

- [Compose LineBreak API](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/LineBreak)
- [Android TextView API](https://developer.android.com/reference/android/widget/TextView)
- [Android ICU BreakIterator API](https://developer.android.com/reference/android/icu/text/BreakIterator)
