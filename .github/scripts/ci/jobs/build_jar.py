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