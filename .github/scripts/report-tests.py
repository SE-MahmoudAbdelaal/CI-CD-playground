"""Shows Gradle's test results on the GitHub Actions run page.

Each failed test becomes an error annotation that points at the line of the test
that failed, and the job summary gets the test counts, a table of failed tests
and the coverage numbers. Run it from the Gradle project folder (demo/).
"""
import glob
import os
import re
import xml.etree.ElementTree as ET


def escape_data(text):
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def escape_property(text):
    return escape_data(text).replace(":", "%3A").replace(",", "%2C")


def one_line(text):
    return " ".join(text.split()).replace("|", "\\|")


total = 0
failed = []
for path in sorted(glob.glob("build/test-results/test/TEST-*.xml")):
    for case in ET.parse(path).getroot().iter("testcase"):
        total += 1
        problem = case.find("failure")
        if problem is None:
            problem = case.find("error")
        if problem is None:
            continue
        test_class = case.get("classname").split("$")[0]
        simple_name = test_class.rsplit(".", 1)[-1]
        test = f"{simple_name} > {case.get('name')}"
        kind = problem.get("type") or ""
        message = (problem.get("message") or "").strip()
        if kind and message.startswith(kind + ":"):
            message = message[len(kind) + 1:].strip()
        message = message or kind or "Test failed"
        file = "demo/src/test/java/" + test_class.replace(".", "/") + ".java"
        location = re.search(r"\(" + re.escape(simple_name) + r"\.java:(\d+)\)", problem.text or "")
        line = location.group(1) if location else "1"
        print(f"::error file={escape_property(file)},line={line},title={escape_property(test)}::{escape_data(message)}")
        failed.append((test, message))

lines = ["### Tests", "", f"{total - len(failed)} of {total} tests passed."]
if failed:
    lines += ["", "| Failed test | Message |", "|---|---|"]
    lines += [f"| {one_line(test)} | {one_line(message)} |" for test, message in failed]

report = "build/reports/jacoco/test/jacocoTestReport.xml"
if os.path.exists(report):
    lines += ["", "### Test coverage", "", "| Counter | Coverage |", "|---|---|"]
    for counter in ET.parse(report).getroot().findall("counter"):
        missed, covered = int(counter.get("missed")), int(counter.get("covered"))
        lines.append(f"| {counter.get('type').title()} | {100 * covered / (missed + covered):.1f}% |")

with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
    summary.write("\n".join(lines) + "\n")
