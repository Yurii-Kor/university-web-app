from ci.jobs.base import BaseJob


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
                f"{self.context.docker_image_name}:ci",
                ".",
            ],
            failure_message="Docker image build failed.",
        )