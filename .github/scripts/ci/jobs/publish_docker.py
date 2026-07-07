from __future__ import annotations

import os

from ci.core.docker import inspect_docker_image
from ci.core.formatting import format_size
from ci.core.version import read_project_version, validate_release_version
from ci.jobs.base import BaseJob


class PublishDockerJob(BaseJob):
    def execute(self) -> None:
        self.require_path("pom.xml")
        self.require_path("mvnw")
        self.require_path("Dockerfile")

        if self.errors:
            return

        if self.context.ref_name != "main":
            self.add_error(
                "Docker image can only be published from the main branch. "
                f"Current branch: {self.context.ref_name}"
            )
            return

        if self.options.version is None:
            self.add_error("Missing required --version argument.")
            return

        self.project_version = read_project_version(self.context.pom_path)
        validate_release_version(self.project_version)

        if self.project_version != self.options.version:
            self.add_error(
                "Version mismatch between git tag job and pom.xml. "
                f"Expected {self.options.version}, found {self.project_version}."
            )
            return

        self.dockerhub_username = os.environ.get("DOCKERHUB_USERNAME", "").strip()

        if not self.dockerhub_username:
            self.add_error("DOCKERHUB_USERNAME environment variable is missing.")
            return

        self.docker_repository = (
            f"{self.dockerhub_username}/{self.context.docker_image_name}"
        )
        self.version_image_tag = f"{self.docker_repository}:{self.project_version}"
        self.latest_image_tag = f"{self.docker_repository}:latest"

        jar_built = self.run_maven(
            "--batch-mode",
            "--no-transfer-progress",
            "package",
            "-DskipTests",
            failure_message="Maven release JAR build failed.",
        )

        if not jar_built:
            return

        jars = self.find_application_jars()

        if not jars:
            self.add_error("No JAR file was produced in target directory.")
            return

        self.release_jar = jars[0]

        docker_built = self.run_command(
            [
                "docker",
                "build",
                "--tag",
                self.version_image_tag,
                "--tag",
                self.latest_image_tag,
                ".",
            ],
            failure_message="Docker image build failed.",
        )

        if not docker_built:
            return

        self.image_summary = inspect_docker_image(
            runner=self.runner,
            image_tag=self.version_image_tag,
            cwd=self.context.project_dir,
        )

        version_pushed = self.run_command(
            ["docker", "push", self.version_image_tag],
            failure_message=f"Could not push Docker image {self.version_image_tag}.",
        )

        latest_pushed = self.run_command(
            ["docker", "push", self.latest_image_tag],
            failure_message=f"Could not push Docker image {self.latest_image_tag}.",
        )

        self.version_pushed = version_pushed
        self.latest_pushed = latest_pushed

    def write_summary(self) -> None:
        self.reporter.add_heading("Publish Docker image summary")
        self.reporter.add_paragraph("Docker Hub release image")

        image_summary = getattr(self, "image_summary", None)

        self.reporter.add_table(
            ("Property", "Value"),
            [
                ("Branch", self.context.ref_name),
                ("Project version", getattr(self, "project_version", "N/A")),
                ("Docker repository", getattr(self, "docker_repository", "N/A")),
                ("Version tag", getattr(self, "version_image_tag", "N/A")),
                ("Latest tag", getattr(self, "latest_image_tag", "N/A")),
                (
                    "Release JAR",
                    getattr(getattr(self, "release_jar", None), "name", "N/A"),
                ),
                (
                    "Image ID",
                    image_summary.image_id if image_summary else "N/A",
                ),
                (
                    "Size",
                    format_size(image_summary.size_bytes) if image_summary else "N/A",
                ),
                (
                    "Architecture",
                    image_summary.architecture if image_summary else "N/A",
                ),
                (
                    "OS",
                    image_summary.os if image_summary else "N/A",
                ),
                (
                    "Java 21 runtime",
                    "Verified"
                    if image_summary and image_summary.java_runtime_verified
                    else "Not verified",
                ),
                (
                    "Version push",
                    "Verified" if getattr(self, "version_pushed", False) else "No",
                ),
                (
                    "Latest push",
                    "Verified" if getattr(self, "latest_pushed", False) else "No",
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