#!/usr/bin/env python3
import json
import sys
import time
import urllib.error
import urllib.request


def get(base_url: str, path: str):
    request = urllib.request.Request(base_url.rstrip("/") + path)
    with urllib.request.urlopen(request, timeout=30) as response:
        if response.status != 200:
            raise AssertionError(f"{path}: HTTP {response.status}")
        content_type = response.headers.get("Content-Type", "")
        body = response.read()
        return json.loads(body) if "json" in content_type else body.decode("utf-8")


def wait_for_readiness(base_url: str):
    deadline = time.monotonic() + 300
    while time.monotonic() < deadline:
        try:
            readiness = get(base_url, "/api/health/starrocks")
            if readiness.get("ready"):
                return readiness
        except (OSError, urllib.error.HTTPError, json.JSONDecodeError):
            pass
        time.sleep(3)
    raise TimeoutError("StarRocks readiness did not become healthy within five minutes")


def ids(rows, key):
    return {row.get(key) for row in rows}


def main():
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    readiness = wait_for_readiness(base_url)
    assert readiness["backend"] == "starrocks"
    assert readiness["actualMigration"] == "1"
    assert readiness["missingTables"] == []

    index = get(base_url, "/")
    assert "<!DOCTYPE html" in index or "<!doctype html" in index

    studies = get(base_url, "/api/studies?projection=SUMMARY")
    assert ids(studies, "studyId") == {"sr_study_a", "sr_study_b"}

    patient = get(base_url, "/api/studies/sr_study_a/patients/PA1")
    assert patient["patientId"] == "PA1"

    samples = get(base_url, "/api/studies/sr_study_a/patients/PA1/samples")
    assert ids(samples, "sampleId") == {"SA1", "SA2"}

    profiles = get(base_url, "/api/studies/sr_study_a/molecular-profiles")
    assert "sr_study_a_mutations" in ids(profiles, "molecularProfileId")

    mutations = get(
        base_url,
        "/api/molecular-profiles/sr_study_a_mutations/mutations"
        "?sampleListId=sr_study_a_all&entrezGeneId=7157&projection=SUMMARY",
    )
    assert len(mutations) == 2
    assert ids(mutations, "sampleId") == {"SA1", "SA2"}

    events = get(base_url, "/api/studies/sr_study_a/patients/PA1/clinical-events")
    assert len(events) >= 3

    print(
        json.dumps(
            {
                "status": "PASS",
                "backend": readiness["backend"],
                "databaseVersion": readiness["databaseVersion"],
                "studies": len(studies),
                "patientSamples": len(samples),
                "mutations": len(mutations),
                "clinicalEvents": len(events),
            },
            sort_keys=True,
        )
    )


if __name__ == "__main__":
    main()
