#!/bin/bash
set -u
RUN_DIR=$1; FORMAT=$2; shift 3
DIR=$(cd "$(dirname "$0")" && pwd)
if ! grep -Eq '^[[:space:]]*eula[[:space:]]*=[[:space:]]*true[[:space:]]*$' "$RUN_DIR/eula.txt" 2>/dev/null; then
	echo "SMOKE TEST BLOCKED: an existing accepted eula.txt is required in $RUN_DIR"
	exit 2
fi
if [ -e "$RUN_DIR/world" ] || [ -e "$RUN_DIR/server.properties" ]; then
	echo "SMOKE TEST BLOCKED: use a fresh test directory to preserve existing worlds and settings"
	exit 2
fi
mkdir -p "$RUN_DIR/world/datapacks"
cp -r "$DIR/${SMOKE_PACK:-datapack}" "$RUN_DIR/world/datapacks/smoke"
printf '{"pack":{"description":"World Omens smoke test","pack_format":%s,"min_format":%s,"max_format":%s}}\n' "$FORMAT" "$FORMAT" "$FORMAT" \
	> "$RUN_DIR/world/datapacks/smoke/pack.mcmeta"
printf 'online-mode=false\nspawn-protection=0\nfunction-permission-level=4\npause-when-empty-seconds=-1\n' > "$RUN_DIR/server.properties"
LOG=$(mktemp)
"$@" > "$LOG" 2>&1
grep -E "\[smoke\]|worldevents|World Omens|Exception|ERROR|at dev\.santiago" "$LOG" | grep -vE "yggdrasil|minecraftservices|Failed to load properties|eula" | head -150
if grep -qE "\[smoke\] done" "$LOG" && grep -qE "\[smoke\] loot hook ok" "$LOG" && ! grep -E "Exception|/ERROR\]" "$LOG" | grep -vqE "yggdrasil|minecraftservices|Failed to load properties|UnknownHost|Cache miss|kqueue|Only supported on OSX|AppenderLoggingException|maxThreads"; then
	echo "SMOKE TEST PASSED"; rm "$LOG"; exit 0
fi
echo "SMOKE TEST FAILED (full log: $LOG)"; exit 1
