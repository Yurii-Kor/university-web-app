from __future__ import annotations

from ci.core.errors import CiError
from ci.core.version import read_project_version, validate_release_version
from ci.jobs.base import BaseJob


class UpdateVersionJob(BaseJob):
    def execute(self) -> None:
        self.require_path("pom.xml")
        self.require_path("mvnw")

        if self.errors:
            return

        if self.options.version is None:
            raise CiError("Missing required --version argument.")

        validate_release_version(self.options.version)

        if self.context.ref_name != "dev":
            raise CiError(
                "Project version can only be updated from the dev branch. "
                f"Current branch: {self.context.ref_name}"
            )

        self.previous_version = read_project_version(self.context.pom_path)

        self.run_maven(
            "versions:set",
            f"-DnewVersion={self.options.version}",
            "-DgenerateBackupPoms=false",
            failure_message="Maven Versions Plugin failed to update project version.",
        )

        self.updated_version = read_project_version(self.context.pom_path)

        if self.updated_version != self.options.version:
            self.add_error(
                "pom.xml version verification failed. "
                f"Expected {self.options.version}, found {self.updated_version}."
            )
            return

        self.configure_git_user()
        self.commit_and_push_version_change()

    def configure_git_user(self) -> None:
        self.run_command(
            ["git", "config", "user.name", "github-actions[bot]"],
            failure_message="Could not configure git user.name.",
        )

        self.run_command(
            [
                "git",
                "config",
                "user.email",
                "41898282+github-actions[bot]@users.noreply.github.com",
            ],
            failure_message="Could not configure git user.email.",
        )

    def commit_and_push_version_change(self) -> None:
        status_result = self.runner.run_capture(
            ["git", "status", "--porcelain", "pom.xml"],
            cwd=self.context.project_dir,
        )

        if not status_result.stdout.strip():
            self.add_warning("pom.xml was already using the requested version.")
            self.version_commit_created = False
            self.version_push_completed = False
            return

        commit_message = f"set project version to {self.options.version}"

        commit_succeeded = self.run_command(
            ["git", "commit", "-am", commit_message],
            failure_message="Could not commit pom.xml version update.",
        )

        if not commit_succeeded:
            self.version_commit_created = False
            self.version_push_completed = False
            return

        push_succeeded = self.run_command(
            ["git", "push", "origin", "HEAD:dev"],
            failure_message="Could not push pom.xml version update to dev.",
        )

        self.version_commit_created = True
        self.version_push_completed = push_succeeded

    def write_summary(self) -> None:
        self.reporter.add_heading("Update project version summary")
        self.reporter.add_paragraph("Maven project version update")

        self.reporter.add_table(
            ("Property", "Value"),
            [
                ("Branch", self.context.ref_name),
                ("Previous version", getattr(self, "previous_version", "N/A")),
                ("Requested version", self.options.version or "N/A"),
                ("Updated version", getattr(self, "updated_version", "N/A")),
                (
                    "Version update",
                    "Verified" if not self.errors else "Failed",
                ),
                (
                    "Commit created",
                    "Yes" if getattr(self, "version_commit_created", False) else "No",
                ),
                (
                    "Push to dev",
                    "Verified" if getattr(self, "version_push_completed", False) else "No",
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