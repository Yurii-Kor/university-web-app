from jobs.base import BaseJob
from jobs.build_jar import BuildJarJob
from jobs.create_release_tag import CreateReleaseTagJob
from jobs.docker_build import DockerBuildJob
from jobs.publish_docker import PublishDockerJob
from jobs.test import TestJob
from jobs.update_version import UpdateVersionJob


JOBS: dict[str, type[BaseJob]] = {
    "test": TestJob,
    "build-jar": BuildJarJob,
    "build-docker-image": DockerBuildJob,
    "update-version": UpdateVersionJob,
    "create-release-tag": CreateReleaseTagJob,
    "publish-docker": PublishDockerJob,
}