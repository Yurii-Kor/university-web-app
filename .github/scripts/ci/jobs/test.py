from ci.core.formatting import format_duration
from ci.core.test_reports import read_surefire_reports
from ci.jobs.base import BaseJob


class TestJob(BaseJob):
    def execute(self) -> None:
        self.require_common_maven_project_files()

        if self.errors:
            return

        self.run_maven(
            "--batch-mode",
            "--no-transfer-progress",
            "test",
            failure_message="Maven tests failed.",
        )

    def write_summary(self) -> None:
        self.reporter.add_heading("Run tests summary")
        self.reporter.add_paragraph("University Web App test results")

        if self.context.surefire_reports_dir.exists():
            summary = read_surefire_reports(self.context.surefire_reports_dir)

            self.reporter.add_table(
                ("Result", "Count"),
                [
                    ("Report files", str(summary.report_files)),
                    ("Test suites", str(summary.test_suites)),
                    ("Total tests", str(summary.total_tests)),
                    ("Passed", str(summary.passed)),
                    ("Failures", str(summary.failures)),
                    ("Errors", str(summary.errors)),
                    ("Skipped", str(summary.skipped)),
                    ("Reported duration", format_duration(summary.duration_seconds)),
                ],
            )
        else:
            self.add_warning("Surefire reports directory was not found.")
            self.reporter.add_table(
                ("Result", "Count"),
                [
                    ("Report files", "0"),
                    ("Test suites", "0"),
                    ("Total tests", "0"),
                    ("Passed", "0"),
                    ("Failures", "0"),
                    ("Errors", "0"),
                    ("Skipped", "0"),
                    ("Reported duration", "0.000 s"),
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
            "src/test/java",
        )

        for relative_path in required_paths:
            self.require_path(relative_path)