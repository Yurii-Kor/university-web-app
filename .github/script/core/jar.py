from __future__ import annotations

import hashlib
import zipfile
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class JarSummary:
    path: Path
    size_bytes: int
    sha256: str
    main_class: str
    start_class: str
    executable_archive: bool


def read_jar_summary(jar_path: Path) -> JarSummary:
    return JarSummary(
        path=jar_path,
        size_bytes=jar_path.stat().st_size,
        sha256=calculate_sha256(jar_path),
        main_class=read_manifest_value(jar_path, "Main-Class"),
        start_class=read_manifest_value(jar_path, "Start-Class"),
        executable_archive=is_executable_spring_boot_jar(jar_path),
    )


def calculate_sha256(path: Path) -> str:
    digest = hashlib.sha256()

    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)

    return digest.hexdigest()


def read_manifest_value(jar_path: Path, key: str) -> str:
    try:
        manifest = read_manifest(jar_path)
    except KeyError:
        return "Not found"

    return manifest.get(key, "Not found")


def read_manifest(jar_path: Path) -> dict[str, str]:
    with zipfile.ZipFile(jar_path) as archive:
        raw_manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")

    manifest: dict[str, str] = {}
    current_key: str | None = None

    for line in raw_manifest.splitlines():
        if not line:
            continue

        if line.startswith(" ") and current_key is not None:
            manifest[current_key] += line[1:]
            continue

        if ":" not in line:
            continue

        key, value = line.split(":", 1)
        current_key = key.strip()
        manifest[current_key] = value.strip()

    return manifest


def is_executable_spring_boot_jar(jar_path: Path) -> bool:
    with zipfile.ZipFile(jar_path) as archive:
        names = set(archive.namelist())

    return (
        "BOOT-INF/classes/" in names
        or any(name.startswith("BOOT-INF/lib/") for name in names)
    )