from core.docker import inspect_docker_image
from core.formatting import format_size
from jobs.base import BaseJob


class DockerBuildJob(BaseJob):
    def execute(self) -> None:
        self.require_path("Dockerfile")

        jars = self.find_application_jars()
        if not jars:
            self.add_error(
                "No JAR file found in target directory. "
                "The build-jar job should upload it as an artifact, "
                "and this job should download it into target/."
            )

        if self.errors:
            return

        print()
        print("JAR files available for Docker build:")
        for jar in jars:
            print(f"- {self.context.relative_to_workspace(jar)}")

        self.run_command(
            [
                "docker",
                "build",
                "--tag",
                self.context.ci_docker_image_tag,
                ".",
            ],
            failure_message="Docker image build failed.",
        )

    def write_summary(self) -> None:
        self.reporter.add_heading("Build Docker image summary")
        self.reporter.add_paragraph("Docker image")

        if self.errors:
            self.reporter.add_table(
                ("Property", "Value"),
                [
                    ("Image tag", self.context.ci_docker_image_tag),
                    ("Image ID", "N/A"),
                    ("Size", "N/A"),
                    ("Architecture", "N/A"),
                    ("OS", "N/A"),
                    ("Java 21 runtime", "Not verified"),
                    ("Docker Hub push", "No"),
                ],
            )
        else:
            image_summary = inspect_docker_image(
                runner=self.runner,
                image_tag=self.context.ci_docker_image_tag,
                cwd=self.context.project_dir,
            )

            self.reporter.add_table(
                ("Property", "Value"),
                [
                    ("Image tag", image_summary.image_tag),
                    ("Image ID", image_summary.image_id),
                    ("Size", format_size(image_summary.size_bytes)),
                    ("Architecture", image_summary.architecture),
                    ("OS", image_summary.os),
                    (
                        "Java 21 runtime",
                        "Verified" if image_summary.java_runtime_verified else "Not verified",
                    ),
                    ("Docker Hub push", "No"),
                ],
            )

        if self.errors:
            self.reporter.add_heading("Errors", level=4)
            self.reporter.add_bullet_list(self.errors)

        if self.warnings:
            self.reporter.add_heading("Warnings", level=4)
            self.reporter.add_bullet_list(self.warnings)

        self.reporter.add_paragraph("Job summary generated at run-time")
        self.reporter.write()