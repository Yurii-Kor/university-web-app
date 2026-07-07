#!/usr/bin/env python3

from __future__ import annotations

import argparse
import sys
from pathlib import Path


SCRIPTS_ROOT = Path(__file__).resolve().parents[1]

if str(SCRIPTS_ROOT) not in sys.path:
    sys.path.insert(0, str(SCRIPTS_ROOT))


from ci.core.context import CiContext
from ci.core.errors import UnknownCommandError
from ci.core.options import CiOptions
from ci.core.reporter import Reporter
from ci.jobs.base import BaseJob
from ci.jobs.registry import JOBS


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="CI helper for university-web-app GitHub Actions."
    )

    parser.add_argument(
        "command",
        choices=sorted(JOBS.keys()),
        help="CI command to run.",
    )

    parser.add_argument(
        "--version",
        help="Project version for release-related commands.",
    )

    return parser.parse_args()


def create_job(
    command: str,
    context: CiContext,
    reporter: Reporter,
    options: CiOptions,
) -> BaseJob:
    job_class = JOBS.get(command)

    if job_class is None:
        raise UnknownCommandError(command)

    return job_class(
        context=context,
        reporter=reporter,
        options=options,
    )


def main() -> int:
    args = parse_args()
    context = CiContext.from_env()
    reporter = Reporter(summary_path=context.summary_path)
    options = CiOptions(version=args.version)

    job = create_job(
        command=args.command,
        context=context,
        reporter=reporter,
        options=options,
    )

    return job.run()


if __name__ == "__main__":
    sys.exit(main())