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