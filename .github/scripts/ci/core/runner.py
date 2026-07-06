from __future__ import annotations

import subprocess
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class CommandResult:
    command: tuple[str, ...]
    cwd: Path
    return_code: int

    @property
    def succeeded(self) -> bool:
        return self.return_code == 0


class CommandRunner:
    def run(self, command: list[str], cwd: Path) -> CommandResult:
        print()
        print("$ " + " ".join(command))
        print(f"cwd: {cwd}")

        completed = subprocess.run(
            command,
            cwd=cwd,
            check=False,
        )

        return CommandResult(
            command=tuple(command),
            cwd=cwd,
            return_code=completed.returncode,
        )