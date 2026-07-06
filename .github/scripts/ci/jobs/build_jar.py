from ci.core.formatting import format_size
from ci.core.jar import read_jar_summary
from ci.jobs.base import BaseJob


class BuildJarJob(BaseJob):
    def execute(self) -> None:
        self.require_common_maven_project_files()

        if self.errors:
            return

        build_succeeded = self.run_maven(
            "--batch-mode",
            "--no-transfer-progress",
            "package",
            "-DskipTests",
            failure_message="Maven JAR build failed.",
        )

        if not build_succeeded:
            return

        jars = self.find_application_jars()

        if not jars:
            self.add_error("No JAR file was produced in target directory.")
            return

        print()
        print("Produced JAR files:")
        for jar in jars:
            print(f"- {self.context.relative_to_workspace(jar)}")

    def write_summary(self) -> None:
        self.reporter.add_heading("Build Spring Boot JAR summary")
        self.reporter.add_paragraph("Spring Boot JAR")

        jars = self.find_application_jars()

        if not jars:
            self.reporter.add_table(
                ("Property", "Value"),
                [
                    ("File", "Not found"),
                    ("Size", "N/A"),
                    ("SHA-256", "N/A"),
                    ("Main class", "N/A"),
                    ("Start class", "N/A"),
                    ("Executable archive", "Not verified"),
                ],
            )
        else:
            jar_summary = read_jar_summary(jars[0])

            self.reporter.add_table(
                ("Property", "Value"),
                [
                    ("File", jar_summary.path.name),
                    ("Size", format_size(jar_summary.size_bytes)),
                    ("SHA-256", jar_summary.sha256),
                    ("Main class", jar_summary.main_class),
                    ("Start class", jar_summary.start_class),
                    (
                        "Executable archive",
                        "Verified" if jar_summary.executable_archive else "Not verified",
                    ),
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

    def require_common_maven_project_files(self) -> None:
        required_paths = (
            "pom.xml",
            "mvnw",
            "mvnw.cmd",
            ".mvn/wrapper/maven-wrapper.properties",
            "src/main/java",
        )

        for relative_path in required_paths:
            self.require_path(relative_path)