from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

from ci.core.errors import CiError


VERSION_PATTERN = re.compile(r"^\d+\.\d+\.\d+([.-][A-Za-z0-9][A-Za-z0-9.-]*)?$")


class InvalidVersionError(CiError):
    def __init__(self, version: str):
        super().__init__(
            "Invalid version format. Use a Maven version like 0.1.0, "
            f"not '{version}'."
        )


class SnapshotVersionError(CiError):
    def __init__(self, version: str):
        super().__init__(
            "Release version must not end with -SNAPSHOT. "
            f"Received: {version}"
        )


def validate_release_version(version: str) -> None:
    if version.startswith("v"):
        raise InvalidVersionError(version)

    if not VERSION_PATTERN.match(version):
        raise InvalidVersionError(version)

    if version.endswith("-SNAPSHOT"):
        raise SnapshotVersionError(version)


def read_project_version(pom_path: Path) -> str:
    tree = ET.parse(pom_path)
    root = tree.getroot()

    namespace = _detect_namespace(root)

    version_element = root.find(f"{namespace}version")

    if version_element is None or version_element.text is None:
        raise CiError("Project version was not found in pom.xml.")

    return version_element.text.strip()


def _detect_namespace(root: ET.Element) -> str:
    if root.tag.startswith("{"):
        return root.tag.split("}", 1)[0] + "}"

    return ""