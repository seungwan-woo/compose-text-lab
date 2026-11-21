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
    cards = "\n".join(screenshot_card(site, *scene) for scene in SCENES)
    toolbars = "\n".join(screenshot_card(site, *scene) for scene in TOOLBARS)
    template = Path(__file__).with_name("index.html").read_text(encoding="utf-8")
    report = (template.replace("{{RUN_URL}}", escape(run))
              .replace("{{COMMIT_URL}}", escape(f"{repo}/commit/{sha}"))
              .replace("{{SHORT_SHA}}", escape(sha[:7]))
              .replace("{{CARDS}}", cards)
              .replace("{{TOOLBARS}}", toolbars))
    (site / "index.html").write_text(report, encoding="utf-8")


if __name__ == "__main__":
    main()
