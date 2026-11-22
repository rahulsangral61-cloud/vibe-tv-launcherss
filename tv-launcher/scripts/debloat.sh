#!/usr/bin/env bash
# Remove / restore bloatware for the current user via ADB. Nothing is deleted from the
# system partition, so a factory reset always brings everything back.
#
#   ./debloat.sh audit      list every package on the TV into packages-dump.txt
#   ./debloat.sh apply      remove every package listed in packages.txt
#   ./debloat.sh restore    bring back every package listed in packages.txt
#   ./debloat.sh one <pkg>  remove a single package
set -uo pipefail
cd "$(dirname "$0")"

command -v adb >/dev/null 2>&1 || { echo "adb not found"; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "No TV connected. Run ./connect.sh first."; exit 1; }

# Packages the TV needs to work. The script refuses to touch these.
is_protected() {
  case "$1" in
    android|com.android.systemui|com.android.settings|com.android.providers.*|com.android.shell|\
    com.android.packageinstaller|com.google.android.packageinstaller|com.android.permissioncontroller|\
    com.google.android.gms|com.google.android.gsf|com.android.vending|com.google.android.tvlauncher|\
    com.google.android.apps.tv.launcherx|com.android.inputmethod.*|com.google.android.inputmethod.*|\
    com.android.bluetooth|com.android.tv.settings|com.google.android.tv.remote.service|\
    com.google.android.webview|com.android.webview|com.android.tv|com.vibe.tvlauncher)
      return 0 ;;
    *) return 1 ;;
  esac
}

installed() {
  adb shell pm list packages 2>/dev/null | tr -d '\r' | grep -qx "package:$1"
}

remove_pkg() {
  local pkg="$1"
  if is_protected "$pkg"; then echo "  PROTECTED  $pkg (skipped)"; return; fi
  if ! installed "$pkg"; then echo "  not found  $pkg"; return; fi
  out="$(adb shell pm uninstall -k --user 0 "$pkg" 2>&1 | tr -d '\r')"
  echo "  $out  $pkg"
}

restore_pkg() {
  local pkg="$1"
  out="$(adb shell cmd package install-existing "$pkg" 2>&1 | tr -d '\r')"
  echo "  $out"
}

read_list() {
  grep -v '^[[:space:]]*#' packages.txt | sed 's/[[:space:]]//g' | grep -v '^$'
}

case "${1:-}" in
  audit)
    {
      echo "# Third-party apps (user installed)"
      adb shell pm list packages -3 | tr -d '\r' | sed 's/^package://' | sort
      echo
      echo "# System apps"
      adb shell pm list packages -s | tr -d '\r' | sed 's/^package://' | sort
    } > packages-dump.txt
    echo "Saved $(wc -l < packages-dump.txt) lines to scripts/packages-dump.txt"
    echo "Copy the unwanted ones into packages.txt, then run: ./debloat.sh apply"
    ;;
  apply)
    echo "Removing packages listed in packages.txt ..."
    while read -r pkg; do remove_pkg "$pkg"; done < <(read_list)
    ;;
  restore)
    echo "Restoring packages listed in packages.txt ..."
    while read -r pkg; do restore_pkg "$pkg"; done < <(read_list)
    ;;
  one)
    [[ -n "${2:-}" ]] || { echo "Usage: $0 one <package>"; exit 1; }
    remove_pkg "$2"
    ;;
  *)
    sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
    ;;
esac
