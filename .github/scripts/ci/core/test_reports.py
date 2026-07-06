from __future__ import annotations

import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class TestReportsSummary:
    report_files: int
    test_suites: int
    total_tests: int
    passed: int
    failures: int
    errors: int
    skipped: int
    duration_seconds: float


def read_surefire_reports(reports_dir: Path) -> TestReportsSummary:
    report_files = sorted(reports_dir.glob("TEST-*.xml"))

    test_suites = 0
    total_tests = 0
    failures = 0
    errors = 0
    skipped = 0
    duration_seconds = 0.0

    for report_file in report_files:
        root = ET.parse(report_file).getroot()

        suites = []

        if root.tag == "testsuite":
            suites.append(root)
        elif root.tag == "testsuites":
            suites.extend(root.findall("testsuite"))

        for suite in suites:
            test_suites += 1
            total_tests += int(suite.attrib.get("tests", "0"))
            failures += int(suite.attrib.get("failures", "0"))
            errors += int(suite.attrib.get("errors", "0"))
            skipped += int(suite.attrib.get("skipped", "0"))
            duration_seconds += float(suite.attrib.get("time", "0"))

    passed = total_tests - failures - errors - skipped

    return TestReportsSummary(
        report_files=len(report_files),
        test_suites=test_suites,
        total_tests=total_tests,
        passed=passed,
        failures=failures,
        errors=errors,
        skipped=skipped,
        duration_seconds=duration_seconds,
    )