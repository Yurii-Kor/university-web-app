from __future__ import annotations

import traceback
from dataclasses import dataclass, field
from pathlib import Path

from ci.core.context import CiContext
from ci.core.errors import CiError
from ci.core.options import CiOptions
from ci.core.reporter import Reporter
from ci.core.runner import CommandRunner


@dataclass
class BaseJob:
    context: CiContext
    reporter: Reporter
    options: CiOptions = field(default_factory=CiOptions)
    runner: CommandRunner = field(default_factory=CommandRunner)
    errors: list[str] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)

    @property
    def name(self) -> str:
        return self.__class__.__name__

    def run(self) -> int:
        try:
            self.validate_context()
            self.execute()
        except CiError as error:
            self.add_error(str(error))
        except Exception:
            traceback.print_exc()
            self.add_error("Unexpected CI script failure. See traceback above.")
        finally:
            self.print_console_summary()
            self.write_summary()

        return 1 if self.errors else 0

    def execute(self) -> None:
        raise NotImplementedError

    def write_summary(self) -> None:
        self.reporter.add_heading(f"{self.name} summary")

        rows = [("Status", "failed" if self.errors else "passed")]

        self.reporter.add_table(("Property", "Value"), rows)

        if self.errors:
            self.reporter.add_heading("Errors", level=4)
            self.reporter.add_bullet_list(self.errors)

        if self.warnings:
            self.reporter.add_heading("Warnings", level=4)
            self.reporter.add_bullet_list(self.warnings)

        self.reporter.write()

    def validate_context(self) -> None:
        if not self.context.workspace.exists():
            self.add_error(f"Workspace does not exist: {self.context.workspace}")

        if not self.context.project_dir.exists():
            self.add_error(
                "Project directory does not exist: "
                f"{self.context.relative_to_workspace(self.context.project_dir)}"
            )

    def add_error(self, message: str) -> None:
        self.errors.append(message)
        self.reporter.error(message)

    def add_warning(self, message: str) -> None:
        self.warnings.append(message)
        self.reporter.warning(message)

    def require_path(self, relative_path: str) -> bool:
        path = self.context.project_dir / relative_path

        if path.exists():
            return True

        self.add_error(f"Missing required path: {relative_path}")
        return False

    def run_command(self, command: list[str], failure_message: str) -> bool:
        result = self.runner.run(command, cwd=self.context.project_dir)

        if result.succeeded:
            return True

        self.add_error(f"{failure_message} Exit code: {result.return_code}")
        return False

    def run_maven(self, *args: str, failure_message: str) -> bool:
        return self.run_command(
            ["bash", "./mvnw", *args],
            failure_message=failure_message,
        )

    def find_application_jars(self) -> list[Path]:
        if not self.context.target_dir.exists():
            return []

        jars = sorted(self.context.target_dir.glob("*.jar"))

        application_jars = [
            jar for jar in jars
            if not jar.name.startswith("original-")
            and not jar.name.endswith("-sources.jar")
            and not jar.name.endswith("-javadoc.jar")
        ]

        return application_jars or jars

    def print_console_summary(self) -> None:
        print()
        print("=" * 80)
        print(f"CI job: {self.name}")
        print(f"Event: {self.context.event_name}")
        print(f"Ref: {self.context.ref_name}")
        print(f"SHA: {self.context.sha}")
        print("=" * 80)

        if self.warnings:
            print()
            print("Warnings:")
            for warning in self.warnings:
                print(f"- {warning}")

        if self.errors:
            print()
            print("Errors:")
            for error in self.errors:
                print(f"- {error}")
        else:
            print()
            print("Validation passed.")