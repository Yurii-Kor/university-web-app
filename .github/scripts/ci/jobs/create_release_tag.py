from __future__ import annotations

import os
from pathlib import Path

from ci.core.version import read_project_version, validate_release_version
from ci.jobs.base import BaseJob


class CreateReleaseTagJob(BaseJob):
    def execute(self) -> None:
        self.require_path("pom.xml")

        if self.errors:
            return

        if self.context.ref_name != "main":
            self.add_error(
                "Release tag can only be created from the main branch. "
                f"Current branch: {self.context.ref_name}"
            )
            return

        self.project_version = read_project_version(self.context.pom_path)
        validate_release_version(self.project_version)

        self.git_tag = f"v{self.project_version}"
        self.current_commit = self.read_current_commit()

        if self.tag_exists(self.git_tag):
            existing_tag_commit = self.read_tag_commit(self.git_tag)

            if existing_tag_commit == self.current_commit:
                self.add_warning(
                    f"Git tag {self.git_tag} already exists on the current commit."
                )
                self.tag_created = False
                self.tag_pushed = False
                self.write_outputs()
                return

            self.add_error(
                f"Git tag {self.git_tag} already exists on a different commit. "
                f"Tag commit: {existing_tag_commit}, current commit: {self.current_commit}."
            )
            return

        tag_created = self.run_command(
            ["git", "tag", self.git_tag],
            failure_message=f"Could not create git tag {self.git_tag}.",
        )

        if not tag_created:
            self.tag_created = False
            self.tag_pushed = False
            return

        tag_pushed = self.run_command(
            ["git", "push", "origin", self.git_tag],
            failure_message=f"Could not push git tag {self.git_tag}.",
        )

        self.tag_created = tag_created
        self.tag_pushed = tag_pushed

        if tag_pushed:
            self.write_outputs()

    def read_current_commit(self) -> str:
        result = self.runner.run_capture(
            ["git", "rev-parse", "HEAD"],
            cwd=self.context.project_dir,
        )

        if not result.succeeded:
            self.add_error("Could not read current commit SHA.")
            return "N/A"

        return result.stdout.strip()

    def tag_exists(self, tag: str) -> bool:
        result = self.runner.run_capture(
            ["git", "rev-parse", "--verify", f"refs/tags/{tag}"],
            cwd=self.context.project_dir,
        )

        return result.succeeded

    def read_tag_commit(self, tag: str) -> str:
        result = self.runner.run_capture(
            ["git", "rev-list", "-n", "1", tag],
            cwd=self.context.project_dir,
        )

        if not result.succeeded:
            self.add_error(f"Could not read commit for tag {tag}.")
            return "N/A"

        return result.stdout.strip()

    def write_outputs(self) -> None:
        output_path = os.environ.get("GITHUB_OUTPUT")

        if output_path is None:
            self.add_warning("GITHUB_OUTPUT is not available. Workflow outputs were not written.")
            return

        outputs = {
            "version": self.project_version,
            "git_tag": self.git_tag,
        }

        with Path(output_path).open("a", encoding="utf-8") as output_file:
            for name, value in outputs.items():
                output_file.write(f"{name}={value}\n")

    def write_summary(self) -> None:
        self.reporter.add_heading("Create release tag summary")
        self.reporter.add_paragraph("Git release tag")

        self.reporter.add_table(
            ("Property", "Value"),
            [
                ("Branch", self.context.ref_name),
                ("Project version", getattr(self, "project_version", "N/A")),
                ("Git tag", getattr(self, "git_tag", "N/A")),
                ("Current commit", getattr(self, "current_commit", "N/A")),
                (
                    "Tag created",
                    "Yes" if getattr(self, "tag_created", False) else "No",
                ),
                (
                    "Tag pushed",
                    "Verified" if getattr(self, "tag_pushed", False) else "No",
                ),
            ],
        )

        if self.errors:
            self.reporter.add_heading("Errors", level=4)
            self.reporter.add_bullet_list(self.errors)

        if self.warnings:
            self.reporter.add_heading("Warnings", level=4)
            self.reporter.add_bullet_list(self.warnings)

        self.reporter.write()