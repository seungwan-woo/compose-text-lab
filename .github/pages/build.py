"""Build the static report from screenshots produced by the Android test."""

import os
import struct
import sys
from html import escape
from pathlib import Path


SCENES = (
    ("render", "기본 렌더링", "왼쪽 Compose, 오른쪽 TextView에 같은 문단 방향 규칙을 적용합니다."),
    ("dark-font-scale", "다크 테마 · fontScale 1.6", "두 엔진에 같은 미리보기 배율을 적용합니다."),
    ("gradient", "그라데이션", "같은 텍스트에 적용된 색상 변화를 비교합니다."),
)

TOOLBARS = (
    ("compose-selection", "Compose 선택 메뉴", "왼쪽 SelectionContainer에서 같은 단어를 선택합니다."),
    ("view-selection", "TextView 선택 메뉴", "오른쪽 TextView에서 같은 단어를 선택합니다."),
)

MOUSE_MENUS = (
    ("compose-right-click", "Compose 우클릭", "SelectionContainer에 마우스 보조 버튼 이벤트를 보냅니다."),
    ("view-right-click", "TextView 우클릭", "선택 가능한 TextView에 같은 종류의 입력을 보냅니다."),
)

OVERFLOW = (
    ("overflow-ellipsis", "한 줄 · 말줄임", "두 엔진 모두 끝에 말줄임표를 표시합니다."),
    ("overflow-clip", "한 줄 · 잘라내기", "줄 수 제한을 넘는 내용은 잘라냅니다."),
)

MARQUEE = (
    ("marquee-start", "Marquee · 첫 프레임", "두 엔진에 같은 한 줄 문자열을 표시합니다."),
    ("marquee-moving", "Marquee · 나중 프레임", "두 패널의 텍스트 위치가 바뀌었는지 검사했습니다."),
)

EFFECTS_MOTION = (
    ("effects-motion", "가변 폰트 · Effects 계측",
     "같은 Roboto Variable의 굵기와 폭 축을 변경하며 레이아웃과 그리기 호출을 관찰합니다."),
)

MULTILINGUAL = (
    ("multilingual-lines", "BiDi · 아랍어와 숫자",
     "같은 텍스트 너비에서 실제 줄 경계, 문단 방향, 기준선, 가로 범위를 비교합니다."),
)

RICH_TEXT = (
    ("rich-text-comparison", "Rich Text · fontScale 1.6",
     "왼쪽은 Compose AnnotatedString, 오른쪽은 TextView Spannable입니다."),
)

TABLET = (
    ("tablet-selection", "태블릿 · 1200dp 에뮬레이터", "왼쪽에서 설정을 스크롤하는 동안 오른쪽 상단의 선택 비교가 자리를 유지합니다."),
)


def screenshot_card(site: Path, scene: str, title: str, caption: str) -> str:
    image = f"screenshots/{scene}.png"
    with (site / image).open("rb") as screenshot:
        header = screenshot.read(24)
    if len(header) != 24 or header[:16] != b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR":
        raise ValueError(f"Invalid PNG header: {image}")
    width, height = struct.unpack(">II", header[16:24])
    if not width or not height:
        raise ValueError(f"Invalid PNG dimensions: {image}")
    return f'''<figure>
      <figcaption><strong>{title}</strong><span>{caption}</span></figcaption>
      <a href="{image}"><img src="{image}" width="{width}" height="{height}" alt="{title} 화면" loading="lazy"></a>
    </figure>'''


def main() -> None:
    site = Path(sys.argv[1])
    repo = f"{os.environ['GITHUB_SERVER_URL']}/{os.environ['GITHUB_REPOSITORY']}"
    run = f"{repo}/actions/runs/{os.environ['GITHUB_RUN_ID']}"
    sha = os.environ["GITHUB_SHA"]
    template = Path(__file__).with_name("index.html").read_text(encoding="utf-8")
    runtime = (site / "runtime.txt").read_text(encoding="utf-8").strip()
    if not runtime:
        raise ValueError("Missing emulator version in runtime.txt")
    report = (template.replace("{{RUN_URL}}", escape(run))
              .replace("{{COMMIT_URL}}", escape(f"{repo}/commit/{sha}"))
              .replace("{{RICH_TEXT_DOC_URL}}", escape(f"{repo}/blob/{sha}/docs/rich-text.md"))
              .replace("{{SELECTION_DOC_URL}}", escape(f"{repo}/blob/{sha}/docs/selection-toolbar.md"))
              .replace("{{LINE_LAYOUT_DOC_URL}}", escape(f"{repo}/blob/{sha}/docs/multilingual-lines.md"))
              .replace("{{EFFECTS_MOTION_DOC_URL}}", escape(f"{repo}/blob/{sha}/docs/effects-motion.md"))
              .replace("{{ANDROID_RUNTIME}}", escape(runtime))
              .replace("{{SHORT_SHA}}", escape(sha[:7])))
    for label, scenes in (("CARDS", SCENES), ("TOOLBARS", TOOLBARS), ("MOUSE_MENUS", MOUSE_MENUS),
                          ("OVERFLOW", OVERFLOW), ("MARQUEE", MARQUEE), ("RICH_TEXT", RICH_TEXT),
                          ("TABLET", TABLET), ("MULTILINGUAL", MULTILINGUAL), ("EFFECTS_MOTION", EFFECTS_MOTION)):
        report = report.replace(f"{{{{{label}}}}}", "\n".join(screenshot_card(site, *scene) for scene in scenes))
    (site / "index.html").write_text(report, encoding="utf-8")


if __name__ == "__main__":
    main()
