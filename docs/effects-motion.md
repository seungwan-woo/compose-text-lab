# Text Effect · TextMotion

**Effects** 탭은 위쪽에서 두 렌더러의 결과를 보여주고, 별도 설정 영역에서 효과와 파라미터를 바꿉니다. Motion을 선택하면 Compose의 `TextMotion.Static` / `Animated`를 바꿀 수 있습니다. View 쪽에서는 같은 실험 조건으로 subpixel과 hinting 플래그를 조절합니다.

| 항목 | 구현 및 검증 범위 |
|---|---|
| FX-01 Shadow | 같은 X/Y 오프셋과 블러 반경을 dp로 조절합니다. |
| FX-02 Gradient | 두 엔진에서 물리적 왼쪽→오른쪽 레이아웃 폭을 사용합니다. 문단 방향을 RTL로 바꿔 볼 수 있습니다. 줄별·글리프별 Gradient와 픽셀 일치 검증은 별도 항목입니다. |
| FX-03 Stroke | 공통 두께와 Padding을 바꿔 경계와 잘림을 관찰합니다. |
| FX-04 Fill + Stroke | 한 레이아웃을 활용해 테마의 강조색 외곽선과 본문색 채우기를 두 번에 나눠 그립니다. |
| FX-05 Marquee | 두 엔진에 반복을 설정합니다. 속도·간격·시작 지연은 Compose에서 조절합니다. TextView의 속도·간격·포커스는 플랫폼 정책을 따릅니다. CI는 두 엔진에서 실제 이동을 확인합니다. |
| FX-06 TextMotion | 레이어 이동과 Canvas 다시 그리기를 같은 이동 폭과 시간으로 비교합니다. CI는 Animated 정책·View 플래그, 레이어 이동 중 양쪽 텍스트 레이아웃 유지, Canvas draw 호출을 확인합니다. |
| FX-07 Variable Font | 양쪽에서 같은 Roboto Variable의 `wght` 300→800 및 `wdth` 75→100을 12단계로 재생합니다. CI는 실제 텍스트 레이아웃과 첫 줄 가로 폭이 갱신되는지 검사합니다. |
| FX-08 Frame Timeline | 앱 창 전체의 `FrameMetrics.TOTAL_DURATION`을 수집합니다. 60/120Hz 기기 비교와 UI Thread·RenderThread·GPU 분석에는 별도 단말과 Perfetto가 필요합니다. |
| FX-09 Dark / 시스템 폰트 | 공통 테마·폰트 설정을 효과에 적용할 수 있습니다. One UI 및 FlipFont 경로는 실기기 검증 항목입니다. |
| FX-10 FontScale | 효과에 공통 fontScale을 적용합니다. CI는 다크 테마에서 1.3 배율을 확인합니다. 효과별·배율별 전체 매트릭스는 수동 검증 범위입니다. |

Gradient 기준은 레이아웃의 가로 좌표입니다. Stroke/Shadow는 측정된 폭과 높이 바깥까지 그려질 수 있습니다. 전체 레이어 Blur는 이 실험에서 다루는 그림자와 적용 범위가 다르며 별도의 API 31+ 프로브가 필요합니다.

## 호출 횟수 읽기

1. **Motion → Layer translation** 또는 **Motion → Canvas redraw**를 선택하고 **Run FX sample**을 누릅니다. 두 경로 모두 같은 dx를 사용합니다. 레이어 이동은 Compose `graphicsLayer`와 `View.translationX`를 사용합니다. Canvas 경로는 기존 텍스트 레이아웃을 그리는 위치를 바꾸고 다시 draw를 요청합니다.
2. **Variable font → Run FX sample**은 앱에 포함된 Roboto Variable을 양쪽에 적용합니다. 다른 문자에 필요한 폰트는 플랫폼 fallback을 따릅니다.
3. **Copy FX report**로 호출 횟수와 처음·마지막 첫 줄의 가로 폭, 앱 환경을 JSON으로 복사합니다. Compose의 `measure`는 프로브 Layout Modifier, `textLayouts`는 `onTextLayout`, `composition`은 커밋된 프로브 구성을 셉니다. View의 `measure`는 `onMeasure`, `textLayouts`는 관찰된 `Layout` 인스턴스 교체, `draw`는 `onDraw`입니다. 각 API의 호출 단위가 달라 절대 횟수의 엔진 간 비교에는 주의가 필요합니다.
4. `windowFrames`의 P50/P95는 두 렌더러와 앱 UI를 함께 그린 창의 값입니다. 값은 단말·주사율·에뮬레이터 부하에 따라 달라집니다. 프레임 샘플이 없으면 JSON에 P50/P95가 생략됩니다.

### 기기에서 Frame Timeline 확인

```bash
adb shell dumpsys gfxinfo io.github.seungwanwoo.composetextlab reset
# 단말에서 같은 폭과 fontScale로 실험 실행
adb shell dumpsys gfxinfo io.github.seungwanwoo.composetextlab framestats
```

60Hz와 120Hz에서 동일한 실험을 재생하고 Perfetto의 Frame Timeline에서 앱·디스플레이 프레임, UI Thread 및 RenderThread를 확인할 수 있습니다. 에뮬레이터 CI는 경로·레이아웃 갱신·캡처/JSON 생성 검증을 담당합니다. 픽셀 유사도(MSSIM)와 기기 성능 기준치는 별도의 검증이 필요합니다.

- [Android FrameMetrics](https://developer.android.com/reference/android/view/FrameMetrics)
- [Compose TextMotion](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/style/TextMotion)
- [Google Fonts Roboto Variable 원본](https://github.com/google/fonts/tree/main/ofl/roboto) · [APK에 포함된 SIL OFL 라이선스](../app/src/main/assets/licenses/roboto-OFL.txt)
