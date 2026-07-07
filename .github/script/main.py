#!/usr/bin/env python3

from __future__ import annotations

import argparse
import sys

from core.context import CiContext
from core.errors import UnknownCommandError
from core.options import CiOptions
from core.reporter import Reporter
from jobs.base import BaseJob
from jobs.registry import JOBS


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="GitHub workflow automation helper for university-web-app."
    )

    parser.add_argument(
        "command",
        choices=sorted(JOBS.keys()),
        help="Automation command to run.",
    )

    parser.add_argument(
        "--version",
        help="Project version for versioning and release commands.",
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