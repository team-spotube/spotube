#!/usr/bin/env bash
# Builds the signed release APK and installs it on your OPPO phone over adb.
#
#   tools/install-release-android.sh [options]
#
#   --device SERIAL   install on this device instead of finding the OPPO (or set DEVICE=SERIAL)
#   --nightly         build the nightly flavor (app id org.spotube.spotube.nightly) instead of stable
#   --launch          open the app after installing (off by default, so it doesn't interrupt you)
#   --no-install      build and verify the APK only
#   --dry-run         run the checks (Rust, signing key, device) and stop before building
#   -h, --help        show this help
#
# Needs: the phone connected with USB debugging on and authorised, the Android SDK (adb,
# build-tools), and a release signing key in composeApp/local.properties (release.signing.*).
# Without that key Gradle makes an unsigned APK, which no phone will install, so the script stops.
#
# It never uninstalls the app: if the phone has a copy signed with a different key, uninstalling
# would erase its data, so the script tells you the command and leaves the decision to you.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

DEVICE="${DEVICE:-}"
MANUFACTURER="${MANUFACTURER:-oppo}" # matched case-insensitively against ro.product.manufacturer
FLAVOR="stable"
LAUNCH=0
INSTALL=1
DRY_RUN=0

log() { printf '\033[1m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[33mwarning:\033[0m %s\n' "$*" >&2; }
die() { printf '\033[31merror:\033[0m %s\n' "$*" >&2; exit 1; }

usage() { sed -n '2,/^$/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --device) [[ $# -ge 2 ]] || die "--device needs a serial"; DEVICE="$2"; shift 2 ;;
    --nightly) FLAVOR="nightly"; shift ;;
    --launch) LAUNCH=1; shift ;;
    --no-install) INSTALL=0; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown option: $1 (try --help)" ;;
  esac
done

FLAVOR_CAP="$(printf '%s' "${FLAVOR:0:1}" | tr '[:lower:]' '[:upper:]')${FLAVOR:1}"
PACKAGE="org.spotube.spotube"
[[ "$FLAVOR" == "nightly" ]] && PACKAGE="$PACKAGE.nightly"
APK="composeApp/build/outputs/apk/$FLAVOR/release/composeApp-$FLAVOR-release.apk"

# --- Rust ---------------------------------------------------------------------------------------
# The Android build runs cargo (Gobley). rustup pins the toolchain per project, and its cargo is
# often not on PATH in a fresh shell, which makes Gradle fail with "Could not start 'cargo'".
GRADLE_FLAGS=(--console=plain)
if command -v rustup >/dev/null 2>&1; then
  CARGO="$(rustup which cargo 2>/dev/null)" || die "rustup has no cargo for this project; install its toolchain with: rustup show"
  if [[ "$(command -v cargo || true)" != "$CARGO" ]]; then
    export PATH="$(dirname "$CARGO"):$PATH"
    # A running Gradle daemon keeps the PATH it started with and would still not find cargo.
    GRADLE_FLAGS+=(--no-daemon)
  fi
elif ! command -v cargo >/dev/null 2>&1; then
  die "cargo not found. Install Rust (https://rustup.rs) first."
fi
log "Rust: $(cargo --version)"
# Gobley 0.3.7 expects the build-script layout of Cargo before 1.100 (CI pins 1.99.1). With a newer
# cargo the Android build fails in the cargoBuild tasks with a FileNotFoundException.
CARGO_VERSION="$(cargo --version | awk '{print $2}')"
IFS=. read -r CARGO_MAJOR CARGO_MINOR _ <<<"$CARGO_VERSION"
if (( CARGO_MAJOR > 1 || (CARGO_MAJOR == 1 && CARGO_MINOR >= 100) )); then
  die "cargo $CARGO_VERSION is too new for Gobley 0.3.7. Pin this repo to 1.99.1: rustup toolchain install 1.99.1 && rustup override set 1.99.1"
fi

# --- Signing key --------------------------------------------------------------------------------
PROPS="composeApp/local.properties"
STORE="$(grep -E '^release\.signing\.storeFile=' "$PROPS" 2>/dev/null | cut -d= -f2- || true)"
[[ -n "$STORE" ]] || die "no release signing key: add release.signing.storeFile/storePassword/keyAlias/keyPassword to $PROPS"
[[ -f "$STORE" ]] || die "the release keystore in $PROPS is missing: $STORE"
log "Signing key: $STORE"

# --- Android SDK tools --------------------------------------------------------------------------
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
ADB="$(command -v adb || true)"
[[ -n "$ADB" ]] || ADB="$SDK/platform-tools/adb"
[[ -x "$ADB" ]] || die "adb not found (looked on PATH and in $SDK/platform-tools)"
APKSIGNER="$(ls -d "$SDK"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -1 || true)"
[[ -n "$APKSIGNER" ]] || die "apksigner not found under $SDK/build-tools"

# --- Disk (a full disk makes Gradle and the Rust build fail in confusing ways) ------------------
FREE_KB="$(df -k "$ROOT" | awk 'NR==2 {print $4}')"
(( FREE_KB > 3 * 1024 * 1024 )) || warn "only $(( FREE_KB / 1024 / 1024 )) GB free; the build may run out of space"

# --- Device -------------------------------------------------------------------------------------
pick_device() {
  if [[ -n "$DEVICE" ]]; then
    printf '%s\n' "$DEVICE"
    return 0
  fi
  local serial state manufacturer
  while read -r serial state _; do
    [[ "$state" == "device" ]] || continue
    manufacturer="$("$ADB" -s "$serial" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r' | tr '[:upper:]' '[:lower:]')"
    if [[ "$manufacturer" == "$MANUFACTURER" ]]; then
      printf '%s\n' "$serial"
      return 0
    fi
  done < <("$ADB" devices | tail -n +2)
  return 1
}

SERIAL=""
if (( INSTALL )); then
  SERIAL="$(pick_device)" || {
    "$ADB" devices >&2
    die "no '$MANUFACTURER' phone is connected and authorised. Plug it in, allow USB debugging, or pass --device SERIAL."
  }
  MODEL="$("$ADB" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
  log "Device: $MODEL ($SERIAL)"
fi

if (( DRY_RUN )); then
  log "Dry run: checks passed. Would build :composeApp:assemble${FLAVOR_CAP}Release${SERIAL:+ and install on $SERIAL}."
  exit 0
fi

# --- Build --------------------------------------------------------------------------------------
log "Building $FLAVOR release (the first build after a change takes a few minutes)"
./gradlew ":composeApp:assemble${FLAVOR_CAP}Release" "${GRADLE_FLAGS[@]}"
[[ -f "$APK" ]] || die "build finished but $APK is missing"

log "Verifying the signature"
CERTS="$("$APKSIGNER" verify --print-certs "$APK")" || die "the APK's signature does not verify (unsigned build?)"
printf '%s\n' "$CERTS" | grep -E "Signer #1 certificate (DN|SHA-256)" | sed 's/^/    /'
log "APK: $ROOT/$APK ($(du -h "$APK" | cut -f1))"

(( INSTALL )) || { log "Not installing (--no-install)"; exit 0; }

# --- Install ------------------------------------------------------------------------------------
log "Installing on $MODEL ($SERIAL)"
if ! OUTPUT="$("$ADB" -s "$SERIAL" install -r "$APK" 2>&1)"; then
  printf '%s\n' "$OUTPUT" >&2
  if grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE" <<<"$OUTPUT"; then
    die "the phone has $PACKAGE signed with a different key. Uninstalling erases its data, so this script won't. If you accept that: $ADB -s $SERIAL uninstall $PACKAGE"
  fi
  die "install failed"
fi
printf '%s\n' "$OUTPUT" | tail -1 | sed 's/^/    /'

if (( LAUNCH )); then
  log "Launching"
  "$ADB" -s "$SERIAL" shell monkey -p "$PACKAGE" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
fi
log "Done: $PACKAGE is installed on $MODEL"
