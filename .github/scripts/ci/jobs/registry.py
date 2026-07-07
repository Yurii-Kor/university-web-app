from ci.jobs.base import BaseJob
from ci.jobs.build_jar import BuildJarJob
from ci.jobs.create_release_tag import CreateReleaseTagJob
from ci.jobs.docker_build import DockerBuildJob
from ci.jobs.publish_docker import PublishDockerJob
from ci.jobs.test import TestJob
from ci.jobs.update_version import UpdateVersionJob


JOBS: dict[str, type[BaseJob]] = {
    "test": TestJob,
    "build-jar": BuildJarJob,
    "build-docker-image": DockerBuildJob,
    "update-version": UpdateVersionJob,
    "create-release-tag": CreateReleaseTagJob,
    "publish-docker": PublishDockerJob,
}