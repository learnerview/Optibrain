#!/bin/sh
# On Windows, `sh` often resolves to a WSL relay that cannot start Windows Node/Java/Gradle
# services. Delegate to the PowerShell launcher, which starts each service independently.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$DIR/run-all.ps1" "$@"
