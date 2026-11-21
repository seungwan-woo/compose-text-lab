# Text effects / selection toolbar

## 구현 대응

| 기능 | Compose | Framework |
|---|---|---|
| Shadow | TextStyle.shadow | TextView.setShadowLayer |
| Gradient | TextStyle.brush | TextPaint.shader, 위젯 layout 후 갱신 |
| Stroke | TextStyle.drawStyle = Stroke | TextPaint.style = STROKE |
| Marquee | Modifier.basicMarquee, 즉시 실행 | singleLine, ellipsize=MARQUEE, selected=true |
| 선택 전용 | SelectionContainer + Text | setTextIsSelectable(true) |
| 편집 | BasicTextField + TextFieldValue | EditText |
| 확장 메뉴 | 기본 Compose 플랫폼 통합 경로 | 선택/삽입 ActionMode.Callback에 Lab item 추가 |

Marquee는 본질적으로 단일 줄 실험이며 maxLines/줄바꿈 설정을 대신합니다. 두 엔진의 속도, 초기 지연, 반복 간격과 포커스 정책을 동일하게 맞춘 실험은 아닙니다. Stroke는 outline-only입니다. 그림자/그라데이션/외곽선은 레이아웃 외부로 그려질 수 있습니다.

View ActionMode callback은 create/prepare/click/destroy를 기록하고 Lab item을 추가합니다. 다른 항목은 false를 반환하여 플랫폼 기본 처리를 유지합니다. Compose 편집 필드는 TextFieldValue의 실제 selection offset을 기록합니다. 선택 전용 Compose Text는 공개 SelectionContainer에서 선택 offset을 직접 노출하지 않으므로 추정값을 기록하지 않습니다. 최신 ContextMenu 경로를 포함한 내부 popup/provider/service 동작은 이 로그로 판별할 수 없습니다.

## One UI 단말 수동 검증

| 조건 | 관찰 및 기록 |
|---|---|
| Light/Dark/Dynamic/System | 공통 프로브 색과 별도 theme-default 프로브; 선택 핸들/메뉴/IME |
| 시스템 fontScale / preview override | 0.8, 1.0, 1.3, 1.6, 2.0; 실제 Context 설정 및 치수 |
| 시스템 폰트/FlipFont | System family 선택, 설정 왕복 후 glyph fallback/weight; 사용자 폰트와 구분 |
| TTF/OTF | 한글/라틴/Arabic/emoji; 400/700; 손상 파일/과대 파일/가져오기 취소 |
| 선택 | 길게 누르기, 핸들 이동, 전체 선택, Copy/Paste, 메뉴 확장 항목 |
| OEM 메뉴 | Translate/Smart selection 등 제공 여부; 앱에 표시된 실제 항목만 기록 |
| IME | 키보드 표시/숨김과 스크롤 중 앵커, 편집 값/selection 유지 |
| 폴더블/멀티윈도우/회전 | 실험 실제 너비 변화와 baseline, 툴바 앵커 재배치 |
| Framework 추적 | OS 빌드/브랜치에 맞는 SelectionToolbar 관련 로그; 원격 여부는 별도 증거 필요 |

## 공식 참고

- [Compose 텍스트 스타일](https://developer.android.com/develop/ui/compose/text/style-text)
- [Compose 텍스트 사용자 상호작용](https://developer.android.com/develop/ui/compose/text/user-interactions)
- [TextView API](https://developer.android.com/reference/android/widget/TextView)
- [ActionMode.Callback](https://developer.android.com/reference/android/view/ActionMode.Callback)
- [Android 14 비선형 폰트 크기 조절](https://developer.android.com/about/versions/14/features#non-linear-font-scaling)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)

참조 문서는 구현/제약 해석을 돕는 자료이며 현재 단말의 내부 경로를 증명하지 않습니다. 재현 버전의 소스와 실제 단말 결과를 함께 보세요.
