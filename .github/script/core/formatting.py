from __future__ import annotations


def format_duration(seconds: float) -> str:
    return f"{seconds:.3f} s"


def format_size(size_bytes: int) -> str:
    if size_bytes < 1024:
        return f"{size_bytes} B"

    size_kb = size_bytes / 1024
    if size_kb < 1024:
        return f"{size_kb:.1f} KB"

    size_mb = size_kb / 1024
    if size_mb < 1024:
        return f"{size_mb:.1f} MB"

    size_gb = size_mb / 1024
    return f"{size_gb:.1f} GB"