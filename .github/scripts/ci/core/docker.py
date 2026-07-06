from __future__ import annotations

import json
from dataclasses import dataclass

from ci.core.runner import CommandRunner


@dataclass(frozen=True)
class DockerImageSummary:
    image_tag: str
    image_id: str
    size_bytes: int
    architecture: str
    os: str
    java_runtime_verified: bool


def inspect_docker_image(
    runner: CommandRunner,
    image_tag: str,
    cwd,
) -> DockerImageSummary:
    inspect_result = runner.run_capture(
        ["docker", "image", "inspect", image_tag],
        cwd=cwd,
    )

    if not inspect_result.succeeded:
        raise RuntimeError(f"Could not inspect Docker image: {image_tag}")

    inspect_data = json.loads(inspect_result.stdout)[0]

    java_result = runner.run_capture(
        ["docker", "run", "--rm", image_tag, "java", "-version"],
        cwd=cwd,
    )

    java_output = java_result.stdout + java_result.stderr
    java_runtime_verified = java_result.succeeded and "21" in java_output

    return DockerImageSummary(
        image_tag=image_tag,
        image_id=inspect_data.get("Id", "Unknown"),
        size_bytes=int(inspect_data.get("Size", 0)),
        architecture=inspect_data.get("Architecture", "Unknown"),
        os=inspect_data.get("Os", "Unknown"),
        java_runtime_verified=java_runtime_verified,
    )