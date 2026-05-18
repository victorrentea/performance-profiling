#!/usr/bin/env bash
# Builds a small Linux image (Temurin 21 + async-profiler) and runs
# BufferSizeDemo to produce side-by-side artifacts in ./out/:
#
#   buffer-demo.jfr        ← JFR of both runs; Java stacks are identical
#   flame-31mb-cpu.html    ← async-profiler CPU profile, 31 MB
#   flame-32mb-cpu.html    ← async-profiler CPU profile, 32 MB — extra kernel
#                            frames (handle_mm_fault, __pi_clear_page, munmap
#                            teardown) that JFR cannot see
#
# Also prints minor-page-fault counts side by side: 32 MB run produces ~50×
# more page faults than 31 MB, all hidden from JFR.
set -euo pipefail

cd "$(dirname "$0")/../.."
ROOT="$(pwd)"
SRC="src/main/java/victor/training/performance/profiling/BufferSizeDemo.java"
IMAGE="buffer-demo-profiler:21"
OUT="$ROOT/out"

mkdir -p "$OUT"
docker build -t "$IMAGE" docker/buffer-demo

# MALLOC_MMAP_THRESHOLD_=33554432 pins glibc's mmap threshold at exactly 32 MB.
# Without it, glibc's adaptive heuristic raises the threshold after the first
# free(), and the cliff disappears.
# THP=never (set inside the privileged container) forces 4 KB pages; otherwise
# the kernel uses 2 MB huge pages on the mmap'd region and the fault count
# difference shrinks ~500×.
DOCKER_RUN=(docker run --rm
  --privileged
  -v "$ROOT":/work
  -e SRC="$SRC"
  -e MALLOC_MMAP_THRESHOLD_=33554432
  -e MALLOC_TRIM_THRESHOLD_=-1
  -w /work
  "$IMAGE"
  bash -lc)

JAVA_OPTS='-XX:+UnlockDiagnosticVMOptions -XX:+PreserveFramePointer -XX:+DebugNonSafepoints'
DEMO_CLASS='victor.training.performance.profiling.BufferSizeDemo'

prelude='
  set -e
  echo never > /sys/kernel/mm/transparent_hugepage/enabled
  echo 1 > /proc/sys/kernel/perf_event_paranoid || true
  command -v /usr/bin/time >/dev/null || { apt-get update -q >/dev/null && apt-get install -y -q time >/dev/null 2>&1; }
'

echo "▶ Compile BufferSizeDemo (one-shot javac, no Maven)"
"${DOCKER_RUN[@]}" "
  set -e
  mkdir -p /work/out/classes
  javac -d /work/out/classes \"\$SRC\"
"

echo "▶ JFR — both 31 MB and 32 MB → out/buffer-demo.jfr"
"${DOCKER_RUN[@]}" "
  $prelude
  java $JAVA_OPTS -cp /work/out/classes \
    -XX:StartFlightRecording=filename=/work/out/buffer-demo.jfr,settings=profile,dumponexit=true \
    $DEMO_CLASS both
"

for mode in fast slow; do
  size=$([ "$mode" = "fast" ] && echo "31mb" || echo "32mb")
  echo "▶ async-profiler CPU + page-fault counters — $mode ($size)"
  "${DOCKER_RUN[@]}" "
    $prelude
    /usr/bin/time -v java $JAVA_OPTS -cp /work/out/classes \
      -agentpath:\$ASYNC_PROFILER_LIB=start,event=cpu,file=/work/out/flame-$size-cpu.html \
      $DEMO_CLASS $mode 2>&1 | grep -E 'buffer|User time|System|Elapsed|Minor.*page'
  "
done

echo
echo "==== Side-by-side kernel frames that appear in 32 MB but not 31 MB ===="
python3 - "$OUT/flame-31mb-cpu.html" "$OUT/flame-32mb-cpu.html" <<'PY'
import re, sys
def decode(p):
    html = open(p).read()
    m = re.search(r'const cpool = \[(.*?)\n\];', html, re.S)
    raw = re.findall(r"'((?:[^'\\]|\\.)*)'", m.group(1))
    pool = [raw[0]]
    for s in raw[1:]:
        if not s: pool.append(pool[-1])
        else:
            n = ord(s[0]) - 32
            pool.append(pool[-1][:n] + s[1:])
    keys = [int(k) for k in re.findall(r'f\((\d+),', html)]
    return [pool[k >> 3] for k in keys if (k >> 3) < len(pool)]

a, b = decode(sys.argv[1]), decode(sys.argv[2])
kernel = re.compile(r'handle_mm_fault|do_mem_abort|do_translation_fault|clear_page|alloc_zeroed|munmap_vmas|folio_(add|remove|batch|throttle)|uncharge_folio|free_unref_folios|do_anon|do_user_addr_fault')
sa = set(f for f in a if kernel.search(f))
sb = set(f for f in b if kernel.search(f))
only_b = sorted(sb - sa)
print(f"31 MB unique kernel frames: {len(sa - sb)}")
print(f"32 MB unique kernel frames: {len(only_b)} — the cliff")
for f in only_b:
    print(f"  {f}")
PY

echo
echo "==== JFR view (Java only — both runs identical) ===="
"${DOCKER_RUN[@]}" "jfr summary /work/out/buffer-demo.jfr | grep -E 'ExecutionSample|NativeMethodSample' | head -4"

echo
echo "Open in browser:"
echo "  open $OUT/flame-31mb-cpu.html   $OUT/flame-32mb-cpu.html"
