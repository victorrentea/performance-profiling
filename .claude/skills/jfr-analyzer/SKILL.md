---
name: jfr-analyzer
description: Use when the user wants to analyze a Java Flight Recorder (.jfr) file, investigate JFR recordings, find performance bottlenecks in JFR data, or mentions JFR, Java Flight Recorder, or flight recording analysis.
---

# JFR Analyzer

Analyze Java Flight Recorder (.jfr) files using the JDK's built-in `jfr` CLI tool and produce a structured performance report.

## Tool

Use `jfr` from the JDK (available since JDK 11):

```
jfr summary <file>       # Recording overview: duration, chunks, event counts
jfr print --json --events <types> <file>  # Dump specific events as JSON
jfr metadata <file>       # List all event types in the recording
```

Always use `--json` output and parse with Python for aggregation.

## Duration Format

JFR JSON encodes durations as ISO 8601 strings like `PT0.011257375S` (= 11.26ms).
Parse with regex: `re.match(r'PT([\d.]+)S', value)` then multiply by 1000 for ms.

## Analysis Workflow

### Step 1: Summary

Run `jfr summary <file>` to get:
- Recording duration
- Event types present and their counts
- JDK version (from jdk.JVMInformation if available)

Use this to decide which event categories to investigate (skip categories with 0 events).

### Step 2: Analyze Key Event Categories

Investigate each category **only if events exist** (count > 0 in summary). Run analyses in parallel where possible.

#### Lock Contention (`jdk.JavaMonitorEnter`)
- Group by `monitorClass.name`
- Compute: count, total blocked time, avg, max, min duration
- Show distribution buckets: 0-1ms, 1-5ms, 5-10ms, 10-50ms, 50-100ms, 100-500ms, 500ms+
- Show top 5 longest events with stack traces (first 8 frames, app code only)
- Show `previousOwner` thread to identify who held the lock
- Group by thread to see which threads suffer most

#### CPU Profiling (`jdk.ExecutionSample`)
- Group by top stack frame (`stackTrace.frames[0].method`) to find hot methods
- Also group by second frame for caller context
- Show top 20 methods by sample count

#### Thread Parking (`jdk.ThreadPark`)
- Group by `parkedClass.name` (what they're waiting on)
- Compute total wait time per class
- High counts on `ReentrantLock` or `CountDownLatch` = potential concurrency issue

#### CPU Load (`jdk.CPULoad`)
- Show `jvmUser`, `jvmSystem`, `machineTotal` over time
- Low JVM CPU + high contention = threads blocked, not computing

#### GC Activity (`jdk.GarbageCollection`, `jdk.GCPhasePause*`)
- Total GC pause time, count, avg/max pause
- GC algorithm (from `jdk.GCConfiguration`)

#### Memory Allocation (`jdk.ObjectAllocationInNewTLAB`, `jdk.ObjectAllocationOutsideTLAB`)
- Group by `objectClass.name`, count occurrences
- OutsideTLAB allocations are expensive - flag if numerous
- Show top 10 allocated types

#### I/O (`jdk.SocketRead`, `jdk.SocketWrite`, `jdk.FileRead`, `jdk.FileWrite`)
- Total count and total duration per type
- Top 5 longest I/O events with thread context
- Distinguish app I/O from RMI/JMX noise (thread name contains "RMI")

#### Exceptions (`jdk.JavaExceptionThrow`, `jdk.JavaErrorThrow`)
- Group by exception class
- High counts may indicate exception-driven control flow (anti-pattern)

### Step 3: Report

Produce a structured summary:

1. **Recording info**: duration, JDK version, GC algorithm, app framework detected
2. **Key findings** (ordered by severity/impact):
   - What is the bottleneck?
   - Quantify: how much time is wasted?
   - Where in the code? (class + method + line number)
3. **Root cause analysis**: explain WHY based on the evidence
4. **Recommendations**: specific fixes

## Python Parsing Template

```python
import json, sys, re
from collections import defaultdict

def parse_iso_dur_ms(d):
    """Parse JFR ISO 8601 duration to milliseconds."""
    m = re.match(r'PT([\d.]+)S', str(d))
    return float(m.group(1)) * 1000 if m else 0

def get_method_name(frame):
    """Extract readable method name from a JFR stack frame."""
    m = frame.get('method', {})
    type_name = m.get('type', {}).get('name', '?')
    method_name = m.get('name', '?')
    line = frame.get('lineNumber', '?')
    return f"{type_name}.{method_name}:{line}"

def is_app_frame(frame):
    """Filter out JDK/framework frames to find application code."""
    name = frame.get('method', {}).get('type', {}).get('name', '')
    skip = ('java/', 'javax/', 'jdk/', 'sun/', 'org/springframework/',
            'org/apache/', 'org/hibernate/', 'com/zaxxer/')
    return not any(name.startswith(s) for s in skip)

# Load events
data = json.load(sys.stdin)
events = data['recording']['events']
```

## Notes

- The `jfr` CLI is part of the JDK, no separate install needed
- For very large JFR files, filter by event type to avoid loading everything into memory
- Always run parallel analyses for independent event types
- Thread names like `http-nio-8080-exec-*` = Tomcat, `pool-*-thread-*` = ExecutorService
- The `previousOwner` field in `JavaMonitorEnter` tells you who was holding the lock when the waiting thread tried to acquire it
