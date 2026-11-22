#!/usr/bin/env bash
# Connect to the TV over Wi-Fi ADB.
#
#   ./connect.sh                    -> auto-discover the TV (mDNS), else prompt for IP
#   ./connect.sh 192.168.1.50       -> connect on the classic port 5555
#   ./connect.sh 192.168.1.50:41234 -> connect on the port shown in "Wireless debugging"
#   ./connect.sh pair 192.168.1.50:37001 123456
#                                   -> pair using the pairing IP:port + code shown on the TV
set -uo pipefail

command -v adb >/dev/null 2>&1 || {
  echo "adb not found. Install Android platform-tools first: https://developer.android.com/tools/releases/platform-tools"
  exit 1
}

if [[ "${1:-}" == "pair" ]]; then
  [[ $# -eq 3 ]] || { echo "Usage: $0 pair <ip:pairing-port> <pairing-code>"; exit 1; }
  adb pair "$2" "$3"
  exit $?
fi

target="${1:-}"

if [[ -z "$target" ]]; then
  echo "Looking for TVs advertising wireless debugging..."
  adb start-server >/dev/null 2>&1
  found="$(adb mdns services 2>/dev/null | awk '/_adb-tls-connect/ {print $NF}' | head -n1)"
  if [[ -n "$found" ]]; then
    echo "Found: $found"
    target="$found"
  else
    read -rp "TV IP address (Settings > Network on the TV): " target
  fi
fi

# No port given -> classic 5555
[[ "$target" == *:* ]] || target="$target:5555"

adb connect "$target"
sleep 1

if adb -s "$target" get-state >/dev/null 2>&1; then
  echo
  echo "Connected to $target"
  echo "Model: $(adb -s "$target" shell getprop ro.product.model | tr -d '\r')"
  echo "Brand: $(adb -s "$target" shell getprop ro.product.brand | tr -d '\r')"
  echo "Android: $(adb -s "$target" shell getprop ro.build.version.release | tr -d '\r')  (SDK $(adb -s "$target" shell getprop ro.build.version.sdk | tr -d '\r'))"
  echo
  echo "If the TV shows 'Allow USB debugging?', choose 'Always allow' and run this script again."
else
  echo
  echo "Could not connect. Check:"
  echo "  - TV and computer are on the same Wi-Fi"
  echo "  - Developer options > USB debugging / Wireless debugging is ON"
  echo "  - Android 11+ with Wireless debugging: pair first ->  $0 pair <ip:pairing-port> <code>"
  exit 1
fi
