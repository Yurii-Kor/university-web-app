from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class CiContext:
    workspace: Path
    project_dir: Path
    docker_image_name: str
    event_name: str
    ref_name: str
    sha: str
    summary_path: Path | None

    @staticmethod
    def from_env() -> "CiContext":
        workspace = Path(os.environ.get("GITHUB_WORKSPACE", ".")).resolve()
        project_dir_name = os.environ.get("PROJECT_DIR", "university-webapp")

        summary_file = os.environ.get("GITHUB_STEP_SUMMARY")
        summary_path = Path(summary_file) if summary_file else None

        return CiContext(
            workspace=workspace,
            project_dir=(workspace / project_dir_name).resolve(),
            docker_image_name=os.environ.get("DOCKER_IMAGE_NAME", "university-web-app"),
            event_name=os.environ.get("GITHUB_EVENT_NAME", "local"),
            ref_name=os.environ.get("GITHUB_REF_NAME", "local"),
            sha=os.environ.get("GITHUB_SHA", "local"),
            summary_path=summary_path,
        )

    @property
    def pom_path(self) -> Path:
        return self.project_dir / "pom.xml"

    @property
    def target_dir(self) -> Path:
        return self.project_dir / "target"

    @property
    def dockerfile_path(self) -> Path:
        return self.project_dir / "Dockerfile"

    def relative_to_workspace(self, path: Path) -> str:
        try:
            return path.resolve().relative_to(self.workspace).as_posix()
        except ValueError:
            return path.as_posix()