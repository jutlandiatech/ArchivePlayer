#!/usr/bin/env python3
"""Writes docs/tdd/<phase>-<slice>.json from JUnit XML results plus SHA-256 hashes of the test sources.

Usage: python3 tools/record-tdd-evidence.py <red|green> <slice> "<command that produced the results>" [results-dir ...]
Default results dirs: core unit tests and connected Android tests of both flavours.
"""
import datetime, glob, hashlib, json, os, subprocess, sys, xml.etree.ElementTree as ET

root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
phase, slice_name, command = sys.argv[1], sys.argv[2], sys.argv[3]
dirs = sys.argv[4:] or ["core/build/test-results/test", "app/build/outputs/androidTest-results/connected"]

tests, summary = [], {"total": 0, "passed": 0, "failed": 0, "skipped": 0}
for d in dirs:
    for path in sorted(glob.glob(os.path.join(root, d, "**", "*.xml"), recursive=True)):
        try:
            suite = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        device = next((p.get("value") for p in suite.iter("property") if p.get("name") == "device"), None)
        flavour = next((f for f in ("full", "foss") if f"/{f}" in path.replace("\\", "/").lower()), None)
        for case in suite.iter("testcase"):
            failure = case.find("failure") if case.find("failure") is not None else case.find("error")
            if failure is not None:
                status = "failed"
            elif case.find("skipped") is not None:
                status = "skipped"
            else:
                status = "passed"
            message = (failure.get("message") or failure.text or "").strip().splitlines()[0][:200] if failure is not None else None
            entry = {"class": case.get("classname"), "name": case.get("name"), "status": status}
            if failure is not None:
                entry["failure"] = {"type": failure.get("type") or (failure.text or "").split(":")[0].strip(), "message": message}
            if device:
                entry["device"] = device
            if flavour:
                entry["flavour"] = flavour
            tests.append(entry)
            summary["total"] += 1
            summary[status] += 1

sources = {}
for pattern in ("core/src/test/**/*.kt", "app/src/androidTest*/**/*.kt", "app/src/test*/**/*.kt"):
    for path in sorted(glob.glob(os.path.join(root, pattern), recursive=True)):
        rel = os.path.relpath(path, root).replace("\\", "/")
        sources[rel] = hashlib.sha256(open(path, "rb").read()).hexdigest()

commit = subprocess.run(["git", "rev-parse", "HEAD"], cwd=root, capture_output=True, text=True).stdout.strip()
out = {
    "phase": phase,
    "slice": slice_name,
    "recordedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
    "baseCommit": commit,
    "command": command,
    "summary": summary,
    "testSourceSha256": sources,
    "tests": tests,
}
target = os.path.join(root, "docs", "tdd", f"{phase}-{slice_name}.json")
with open(target, "w", encoding="utf-8") as fh:
    json.dump(out, fh, indent=2, ensure_ascii=False)
    fh.write("\n")
print(f"{target}: {summary}")
