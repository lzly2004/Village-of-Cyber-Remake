#!/usr/bin/env bash
# Linux desktop safety wrapper. VOCR_GRADLE may select a cached Gradle installation.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
gradle_bin=${VOCR_GRADLE:-./gradlew}
exec 9>"${XDG_RUNTIME_DIR:-/tmp}/vocr-gradle-${UID}.lock"
flock -n 9 || { echo 'Another guarded JVM task is running.' >&2; exit 75; }
"$gradle_bin" --stop
free -h
available_kib() { awk '/^MemAvailable:/ {print $2}' /proc/meminfo; }
if (( $(available_kib) < 2097152 )); then
    echo 'REFUSED: MemAvailable is below 2 GiB.' >&2
    exit 75
fi
if pgrep -u "$UID" -f '[o]rg.gradle.launcher.daemon|[G]radleWorkerMain|[K]otlinCompileDaemon' >/dev/null; then
    echo 'REFUSED: another Gradle/Kotlin JVM remains; inspect it first.' >&2
    exit 75
fi
if (( $# == 0 )); then echo 'Supply a targeted task and test filters.' >&2; exit 64; fi
# Full regression/package runs require an explicit human-approved override.
for arg in "$@"; do
    case "$arg" in
        build|check|fatJar|assemble|lint|connectedAndroidTest)
            [[ ${VOCR_APPROVED_FULL_CHECK:-0} == 1 ]] || { echo 'Full/package task is not approved.' >&2; exit 64; } ;;
        test)
            if [[ ${VOCR_APPROVED_FULL_CHECK:-0} != 1 && " $* " != *' --tests '* ]]; then
                echo 'Unfiltered test is not approved.' >&2; exit 64
            fi ;;
    esac
done
# Never enumerate or signal the machine's process table here. The supervisor
# fails closed unless Linux child adoption is available, and signals only its
# own unreaped children (never process groups or ps/awk-selected PIDs).
exec python3 -B tools/guarded-process.py -- \
    choom -n 500 -- "$gradle_bin" --no-daemon --max-workers=1 --console=plain "$@"
