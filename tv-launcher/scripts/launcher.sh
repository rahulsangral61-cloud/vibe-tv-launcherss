#!/usr/bin/env bash
# Install the launcher and make it the Home screen.
#
#   ./launcher.sh install [apk]   install / update the APK (default: the debug build output)
#   ./launcher.sh activate        disable the stock launcher so Home opens Vibe Launcher
#   ./launcher.sh restore-stock   re-enable the stock launcher (undo activate)
#   ./launcher.sh status          show which app currently handles the Home button
set -uo pipefail
cd "$(dirname "$0")"

PKG="com.vibe.tvlauncher"
# Stock launchers: classic Android TV and Google TV
STOCK_LAUNCHERS=(com.google.android.tvlauncher com.google.android.apps.tv.launcherx)
DEFAULT_APK="../app/build/outputs/apk/debug/app-debug.apk"

command -v adb >/dev/null 2>&1 || { echo "adb not found"; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "No TV connected. Run ./connect.sh first."; exit 1; }

installed() { adb shell pm list packages 2>/dev/null | tr -d '\r' | grep -qx "package:$1"; }

case "${1:-}" in
  install)
    apk="${2:-$DEFAULT_APK}"
    [[ -f "$apk" ]] || { echo "APK not found: $apk  (build it first, see README)"; exit 1; }
    adb install -r "$apk"
    ;;
  activate)
    installed "$PKG" || { echo "Install the launcher first:  ./launcher.sh install"; exit 1; }
    # Some builds honour this directly
    adb shell cmd package set-home-activity "$PKG/.MainActivity" >/dev/null 2>&1 || true
    for s in "${STOCK_LAUNCHERS[@]}"; do
      if installed "$s"; then
        echo "Disabling stock launcher: $s"
        adb shell pm disable-user --user 0 "$s"
      fi
    done
    echo
    echo "Done. Press Home on the remote. If anything looks wrong:  ./launcher.sh restore-stock"
    ;;
  restore-stock)
    for s in "${STOCK_LAUNCHERS[@]}"; do
      if installed "$s"; then
        echo "Enabling stock launcher: $s"
        adb shell pm enable "$s"
      fi
    done
    ;;
  status)
    echo "Home handler:"
    adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -n1
    for s in "${STOCK_LAUNCHERS[@]}"; do
      installed "$s" && echo "Stock launcher present: $s"
    done
    installed "$PKG" && echo "Vibe Launcher: installed" || echo "Vibe Launcher: not installed"
    ;;
  *)
    sed -n '2,7p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
