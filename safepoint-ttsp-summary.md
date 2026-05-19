# Measuring Time-To-Safepoint (TTSP) in the JVM

## What the JVM flags do

### `-Xlog:safepoint*`

Modern JVM logging (JDK 9+, replaces the older `-XX:+PrintSafepointStatistics`). The `safepoint*` pattern matches all tags starting with "safepoint" (safepoint, safepoint+stats, safepoint+cleanup, etc.).

**What you see in the log:** for *every* safepoint (there are many — dozens per minute in a live application), a line like:

```
[12.345s][info][safepoint] Safepoint "G1CollectForAllocation", Time since last: 234567 ns, Reaching safepoint: 1234567 ns, At safepoint: 5678901 ns, Total: 6913468 ns
```

The numbers that matter:
- **`Reaching safepoint`** = **TTSP**. How long it took for all threads to stop. *This is the number that tells you if you have the problem.*
- **`At safepoint`** = how long the actual operation took (GC pause, etc.)
- **`Total`** = the sum — how long your application was actually blocked.

The safepoint type (`G1CollectForAllocation`, `RevokeBias`, `Deoptimize`, `ThreadDump`...) also tells you what caused the safepoint — sometimes you discover the GC isn't the cause, but other VM operations are.

**Cost:** practically zero. It just writes lines to a file. You can keep it on permanently in production.

Recommended full form (with file rotation so you don't fill the disk):
```
-Xlog:safepoint*=info:file=safepoint.log:time,uptime,tid:filecount=10,filesize=50M
```

### `-XX:+SafepointTimeout`

Enables a *watchdog*. The JVM watches how long TTSP takes and, if it crosses a threshold (see the next flag), **tells you exactly which threads failed to reach the safepoint**.

### `-XX:SafepointTimeoutDelay=500`

Sets the threshold (in milliseconds). With `500`, any TTSP > 500ms triggers a log like:

```
# SafepointSynchronize::begin: Timeout detected:
# SafepointSynchronize::begin: Timed out while spinning to reach a safepoint.
# SafepointSynchronize::begin: Threads which did not reach the safepoint:
# "worker-thread-42" #87 daemon prio=5 os_prio=0 tid=0x00007f... nid=0x... runnable
#    java.lang.Thread.State: RUNNABLE
#       at com.client.app.HeavyLoop.processBatch(HeavyLoop.java:142)
#       at com.client.app.Worker.run(Worker.java:88)
```

**This is the gold.** The stack trace points you directly to the line of code preventing the thread from reaching the safepoint. Usually you'll see the same class/method appearing repeatedly — that's the culprit.

## Why "a day in production"

- The problem only appears under **real load** and with *certain data combinations*. Staging almost certainly won't reproduce it.
- High TTSP events are **statistically rare** but **painful when they hit** — you want to catch several incidents, not just one.
- A day is enough to spot patterns (peak hours? nightly batch jobs? specific requests?).

## What to do with the data

After a day:
1. Filter `safepoint.log` for lines with large `Reaching safepoint:` values (`grep` + sort).
2. Look at the application log around those timestamps — find the timeout messages with stack traces.
3. 2–3 stack traces will repeat → that's the code to fix.

**Caveat:** a single high-TTSP event isn't enough. You're looking for a **repeating pattern**. And cross-check that a large GC log entry isn't caused by something else (huge allocation, disk swap) — the safepoint log helps you separate causes.

## Quick reference: causes of long TTSP

- **Counted loops without safepoint polls** — HotSpot historically omits polls in `for (int i = 0; i < N; i++)` loops. Heavy bodies or huge iteration counts → thread doesn't check the safepoint flag until loop exit. *Loop strip mining* (JDK 10+) mitigates this.
- **Bulk operations** like `Arrays.fill` / `System.arraycopy` on huge arrays.
- **JNI critical sections** (`GetPrimitiveArrayCritical`).
- **Long-running interpreted code** in unusual code paths.

## Solutions, in order of effort

1. **Upgrade JDK** (if still on 8/11). JDK 10+ has loop strip mining — may solve the problem with no code changes.
2. **`-XX:+UseCountedLoopSafepoints`** — forces polls in counted loops. Small throughput cost, but often a net win when GC pauses are large.
3. **Targeted refactor** of the cases identified by `SafepointTimeout`:
   - break large bulk ops into chunks
   - switch `int` indices to `long` (no longer a counted loop, gets polls)
   - move heavy computation out of counted loop bodies
4. **Switching the GC** (ZGC / Shenandoah) only helps if the real problem is **safepoint operation time**, not TTSP. Check the logs first:
   - If "Reaching safepoint" is large → it's TTSP, changing GC won't help.
   - If the actual GC pause is large → ZGC / Shenandoah (sub-ms goals, concurrent compaction) will help.
5. **Avoid `GetPrimitiveArrayCritical`** in your own JNI code.
