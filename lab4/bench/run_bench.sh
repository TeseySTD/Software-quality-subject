#!/usr/bin/env bash
# Cold-start benchmark of v0..v4. Run from lab4/:  bash bench/run_bench.sh
# Needs a JDK (javac/java) and python3. Wall-clock time of the whole JVM process, median of N fresh JVMs.
set -euo pipefail
export JAVA_TOOL_OPTIONS=
FILE=src/txt/harry.txt
BIG=$(mktemp)
for i in $(seq 50); do cat "$FILE" >> "$BIG"; done
OUT=$(mktemp -d)

for v in v0-original v1-refactor v2-streams v3-optimized v4-fast; do
  mkdir -p "$OUT/$v"
  javac -nowarn -d "$OUT/$v" bench/$v/*.java
done

median_ms() {  # usage: median_ms N cmd...
  local n=$1; shift
  python3 - "$n" "$@" <<'PY'
import statistics, subprocess, sys, time
n = int(sys.argv[1]); cmd = sys.argv[2:]
ts = []
for _ in range(n):
    t = time.perf_counter()
    subprocess.run(cmd, stdout=subprocess.DEVNULL, check=True)
    ts.append((time.perf_counter() - t) * 1000)
print(f"{statistics.median(ts):.1f}")
PY
}

main_of() { if [ "$1" = v0-original ]; then echo Main; else echo skoreiko.chnu.edu.Main; fi; }

echo "== Book (0.4 MB), median of 15 fresh JVMs (v0: 5), wall-clock ms =="
for v in v0-original v1-refactor v2-streams v3-optimized v4-fast; do
  n=15; [ "$v" = v0-original ] && n=5
  echo "$v | $(median_ms $n java -cp "$OUT/$v" "$(main_of $v)" "$FILE")"
done

echo "== Book x50 (21 MB), median of 3 fresh JVMs, wall-clock ms (v0 skipped: quadratic) =="
for v in v1-refactor v2-streams v3-optimized v4-fast; do
  echo "$v | $(median_ms 3 java -cp "$OUT/$v" "$(main_of $v)" "$BIG")"
done
rm -f "$BIG"

echo "== Time inside Main.main (as printed by the program), median of 15 fresh JVMs, ms =="
for v in v3-optimized v4-fast; do
  t=$(for i in $(seq 15); do java -cp "$OUT/$v" skoreiko.chnu.edu.Main "$FILE" 2>/dev/null | grep 'Time, ms' | sed 's/.*: *//'; done       | python3 -c 'import sys,statistics as s;print(s.median(map(float,sys.stdin.read().split())))')
  echo "$v | $t"
done
