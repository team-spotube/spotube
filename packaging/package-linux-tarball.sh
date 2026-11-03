#!/usr/bin/env bash
# Packs the GraalVM native distributable into spotube-linux-<version>-<arch>.tar.xz, the
# portable tarball the AUR (spotube-bin) and Flathub (org.spotube.spotube) packages install from.
#
# Run after `:composeApp:createGraalvmNativeDistributable` WITHOUT -PappImage=true (the tarball uses the
# system's or the Flatpak runtime's VLC, GTK 3 and WebKitGTK, like the deb/rpm) and
# `:composeApp:renderPackagingLinux` (desktop entry + metainfo with the release notes).
#
# Usage: packaging/package-linux-tarball.sh <version> [stable|nightly] [out-dir]
#
# Layout:
#   spotube-linux-<version>-<arch>/
#     app/                                              binary + the JDK/Skiko libraries it loads
#     share/applications/org.spotube.spotube.desktop
#     share/metainfo/org.spotube.spotube.metainfo.xml
#     share/icons/hicolor/512x512/apps/org.spotube.spotube.png
#     LICENSE
set -euo pipefail

version="${1:?usage: $0 <version> [stable|nightly] [out-dir]}"
channel="${2:-stable}"
out_dir="${3:-.}"

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
app_dir="${SPOTUBE_APP_DIR:-$root/composeApp/build/compose/binaries/main/graalvm-app/org.spotube.spotube}"
linux_dir="$root/composeApp/build/packaging/linux"

case "$(uname -m)" in
  x86_64 | amd64) arch=x86_64 ;;
  aarch64 | arm64) arch=aarch64 ;;
  *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;;
esac

if [[ ! -x "$app_dir/spotube" ]]; then
  echo "No native distributable at $app_dir; run :composeApp:createGraalvmNativeDistributable first" >&2
  exit 1
fi
if [[ ! -f "$linux_dir/org.spotube.spotube.metainfo.xml" ]]; then
  echo "No rendered metainfo in $linux_dir; run :composeApp:renderPackagingLinux first" >&2
  exit 1
fi
# An AppImage build (-PappImage=true) leaves VLC plugins and a GTK/WebKit tree in the same dir
if [[ -d "$app_dir/plugins" || -d "$app_dir/usr" ]]; then
  echo "$app_dir bundles VLC/GTK (an -PappImage=true build); rebuild without it" >&2
  exit 1
fi

name="spotube-linux-$version-$arch"
staging="$(mktemp -d)"
trap 'rm -rf "$staging"' EXIT
dest="$staging/$name"

mkdir -p "$dest/share/applications" "$dest/share/metainfo" "$dest/share/icons/hicolor/512x512/apps"
cp -a "$app_dir" "$dest/app"
rm -f "$dest/app/.complete"
install -m644 "$linux_dir/org.spotube.spotube.desktop" "$dest/share/applications/"
install -m644 "$linux_dir/org.spotube.spotube.metainfo.xml" "$dest/share/metainfo/"
install -m644 "$root/composeApp/icons/$channel/icon.png" "$dest/share/icons/hicolor/512x512/apps/org.spotube.spotube.png"
install -m644 "$root/LICENSE" "$dest/LICENSE"

mkdir -p "$out_dir"
tarball="$(cd "$out_dir" && pwd)/$name.tar.xz"
tar -C "$staging" --sort=name --owner=0 --group=0 --numeric-owner \
  --mtime="@${SOURCE_DATE_EPOCH:-$(git -C "$root" log -1 --format=%ct)}" \
  -cf - "$name" | xz -T0 -9 > "$tarball"

echo "$tarball"
