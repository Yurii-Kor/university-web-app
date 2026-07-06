from ci.jobs.base import BaseJob
from ci.jobs.build_jar import BuildJarJob
from ci.jobs.docker_build import DockerBuildJob
from ci.jobs.test import TestJob


JOBS: dict[str, type[BaseJob]] = {
    "test": TestJob,
    "build-jar": BuildJarJob,
    "build-docker-image": DockerBuildJob,
}