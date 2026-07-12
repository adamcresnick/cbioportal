#!/usr/bin/env python3
import json
import statistics
import sys
import time
import urllib.request
from pathlib import Path


def percentile(values, quantile):
    ordered = sorted(values)
    return ordered[min(len(ordered) - 1, int(len(ordered) * quantile))]


def main():
    base_url = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
    budget_path = Path(__file__).with_name("performance-budgets.json")
    budgets = json.loads(budget_path.read_text())
    report = {}
    failures = []
    for path, budget_ms in budgets.items():
        timings = []
        for _ in range(5):
            start = time.perf_counter()
            with urllib.request.urlopen(base_url.rstrip("/") + path, timeout=30) as response:
                response.read()
                if response.status != 200:
                    raise AssertionError(f"{path}: HTTP {response.status}")
            timings.append((time.perf_counter() - start) * 1000)
        p95 = percentile(timings, 0.95)
        report[path] = {
            "budgetMs": budget_ms,
            "medianMs": round(statistics.median(timings), 2),
            "p95Ms": round(p95, 2),
        }
        if p95 > budget_ms:
            failures.append(f"{path} p95={p95:.2f}ms budget={budget_ms}ms")
    print(json.dumps(report, indent=2, sort_keys=True))
    if failures:
        raise AssertionError("Performance budget exceeded: " + "; ".join(failures))


if __name__ == "__main__":
    main()
