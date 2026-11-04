#!/bin/bash
# Smoke test for a loader's dedicated server: summons every creature, starts and stops every event, then stops.
# Usage: run.sh <run dir> <pack format> -- <command that starts the server in that run dir>
# For 1.16.5 and 1.18.2, set SMOKE_PACK=datapack-legacy.
# Prints the server log lines that matter and exits 1 if the log has errors from the mod or exceptions.
set -u
RUN_DIR=$1; FORMAT=$2; shift 3
DIR=$(cd "$(dirname "$0")" && pwd)
rm -rf "$RUN_DIR/world" "$RUN_DIR/logs"
mkdir -p "$RUN_DIR/world/datapacks"
cp -r "$DIR/${SMOKE_PACK:-datapack}" "$RUN_DIR/world/datapacks/smoke"
printf '{"pack":{"description":"World Omens smoke test","pack_format":%s,"min_format":%s,"max_format":%s}}\n' "$FORMAT" "$FORMAT" "$FORMAT" \
	> "$RUN_DIR/world/datapacks/smoke/pack.mcmeta"
echo "eula=true" > "$RUN_DIR/eula.txt"
printf 'online-mode=false\nspawn-protection=0\nfunction-permission-level=4\n' > "$RUN_DIR/server.properties"
LOG=$(mktemp)
"$@" > "$LOG" 2>&1
grep -E "\[smoke\]|worldevents|World Omens|Exception|ERROR|at dev\.santiago" "$LOG" | grep -vE "yggdrasil|minecraftservices|Failed to load properties|eula" | head -150
if grep -qE "\[smoke\] done" "$LOG" && grep -qE "\[smoke\] loot hook ok" "$LOG" && ! grep -E "Exception|/ERROR\]" "$LOG" | grep -vqE "yggdrasil|minecraftservices|Failed to load properties|UnknownHost|Cache miss|kqueue|Only supported on OSX|AppenderLoggingException|maxThreads"; then
	echo "SMOKE TEST PASSED"; rm "$LOG"; exit 0
fi
echo "SMOKE TEST FAILED (full log: $LOG)"; exit 1
