#!/usr/bin/env python3
import argparse
import json
import urllib.request


PATHS = (
    "/api/info",
    "/api/studies?projection=SUMMARY",
    "/api/studies/sr_study_a/patients/PA1",
    "/api/studies/sr_study_a/patients/PA1/samples",
    "/api/studies/sr_study_a/molecular-profiles",
    "/api/molecular-profiles/sr_study_a_mutations/mutations?sampleListId=sr_study_a_all&entrezGeneId=7157&projection=SUMMARY",
    "/api/studies/sr_study_a/patients/PA1/clinical-events",
)

NONDETERMINISTIC_INFO_FIELDS = {
    "gitBranch",
    "gitCommitId",
    "gitCommitIdDescribe",
    "gitCommitIdDescribeShort",
    "gitCommitMessageFull",
    "gitCommitMessageShort",
    "gitCommitMessageUserEmail",
    "gitCommitMessageUserName",
    "gitDirty",
}


def fetch(base_url, path):
    with urllib.request.urlopen(base_url.rstrip("/") + path, timeout=30) as response:
        return json.load(response)


def canonicalize(value, path):
    if isinstance(value, dict):
        ignored = NONDETERMINISTIC_INFO_FIELDS if path == "/api/info" else set()
        return {
            key: canonicalize(item, path)
            for key, item in sorted(value.items())
            if key not in ignored
        }
    if isinstance(value, list):
        canonical = [canonicalize(item, path) for item in value]
        return sorted(canonical, key=lambda item: json.dumps(item, sort_keys=True))
    return value


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--reference-url", required=True)
    parser.add_argument("--starrocks-url", required=True)
    args = parser.parse_args()

    failures = []
    for path in PATHS:
        reference = canonicalize(fetch(args.reference_url, path), path)
        starrocks = canonicalize(fetch(args.starrocks_url, path), path)
        if reference != starrocks:
            failures.append(path)
    if failures:
        raise AssertionError("API parity mismatch: " + ", ".join(failures))
    print(json.dumps({"status": "PASS", "paths": len(PATHS)}, sort_keys=True))


if __name__ == "__main__":
    main()
