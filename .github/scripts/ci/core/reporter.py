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

    def add_summary_line(self, line: str = "") -> None:
        self.summary_lines.append(line)

    def write_job_summary(
        self,
        job_name: str,
        errors: list[str],
        warnings: list[str],
    ) -> None:
        self.add_summary_line(f"### {job_name}")
        self.add_summary_line()

        if errors:
            self.add_summary_line("**Status:** failed")
            self.add_summary_line()
            self.add_summary_line("#### Errors")
            for error in errors:
                self.add_summary_line(f"- {error}")
        else:
            self.add_summary_line("**Status:** passed")

        if warnings:
            self.add_summary_line()
            self.add_summary_line("#### Warnings")
            for warning in warnings:
                self.add_summary_line(f"- {warning}")

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