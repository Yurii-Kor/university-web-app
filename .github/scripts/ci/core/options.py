from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class CiOptions:
    version: str | None = None