from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path


@dataclass
class Reporter:
    summary_path: Path | None
    summary_lines: list[str] = field(default_factory=list)

    def info(self, message: str) -> None:
        print(message)

    def warning(self, message: str) -> None:
        print(f"::warning::{self._escape_workflow_command(message)}")
        print(f"WARNING: {message}")

    def error(self, message: str) -> None:
        print(f"::error::{self._escape_workflow_command(message)}")
        print(f"ERROR: {message}")

    def add_heading(self, title: str, level: int = 3) -> None:
        self.summary_lines.append(f"{'#' * level} {title}")
        self.summary_lines.append("")

    def add_paragraph(self, text: str) -> None:
        self.summary_lines.append(text)
        self.summary_lines.append("")

    def add_table(self, headers: tuple[str, ...], rows: list[tuple[str, ...]]) -> None:
        self.summary_lines.append("| " + " | ".join(headers) + " |")
        self.summary_lines.append("| " + " | ".join("---" for _ in headers) + " |")

        for row in rows:
            self.summary_lines.append("| " + " | ".join(row) + " |")

        self.summary_lines.append("")

    def add_bullet_list(self, items: list[str]) -> None:
        for item in items:
            self.summary_lines.append(f"- {item}")

        self.summary_lines.append("")

    def write(self) -> None:
        if self.summary_path is None:
            return

        with self.summary_path.open("a", encoding="utf-8") as summary_file:
            for line in self.summary_lines:
                summary_file.write(line + "\n")

    @staticmethod
    def _escape_workflow_command(value: str) -> str:
        return (
            value.replace("%", "%25")
            .replace("\r", "%0D")
            .replace("\n", "%0A")
        )