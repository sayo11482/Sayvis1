#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# SAYVIS launcher-asset generator
#
# Rebuilds every launcher icon in the project from ONE source image:
#   usage:  tools/make-icons.sh [source-image]
#           tools/make-icons.sh                        # uses art/sayvis-icon-master.png
#           tools/make-icons.sh ~/Downloads/my-art.jpg # e.g. the owner's original artwork
#
# The source may be any aspect ratio; it is centre-cropped to a square first.
# Produces:
#   mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp        (legacy, pre-API 26)
#   mipmap-{...}/ic_launcher_round.webp                             (circular, transparent corners)
#   drawable-nodpi/ic_launcher_foreground.png                       (adaptive foreground, 108dp @4x)
#   art/sayvis-icon-512.png                                         (store listing)
#
# The adaptive background colour and the themed-icon (monochrome) glyph live in
# res/drawable XML and are not regenerated here.
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")/.."

SRC="${1:-art/sayvis-icon-master.png}"
RES="app/src/main/res"

command -v convert >/dev/null 2>&1 || { echo "ImageMagick (convert) is required." >&2; exit 1; }
[ -f "$SRC" ] || { echo "source image not found: $SRC" >&2; exit 1; }

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

# 1) centre-crop to a square so a landscape artwork still frames the subject
W=$(identify -format "%w" "$SRC")
H=$(identify -format "%h" "$SRC")
if [ "$W" -gt "$H" ]; then GEO="${H}x${H}"; else GEO="${W}x${W}"; fi
convert "$SRC" -gravity center -crop "${GEO}+0+0" +repage "$TMP/square.png"
echo "source ${W}x${H} -> square ${GEO}"

# 2) legacy launcher icons (devices below API 26)
for spec in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
  d="${spec%%:*}"; s="${spec##*:}"
  mkdir -p "$RES/mipmap-$d"
  convert "$TMP/square.png" -resize "${s}x${s}" -strip -quality 90 \
    "$RES/mipmap-$d/ic_launcher.webp"
  # round variant: same art inside a circular mask with transparent corners
  c=$((s / 2)); r=$((s / 2 - 1))
  convert "$TMP/square.png" -resize "${s}x${s}" \
    \( -size "${s}x${s}" xc:none -fill white -draw "circle ${c},${c} ${c},$((c + r))" \) \
    -alpha off -compose CopyOpacity -composite -strip -quality 90 \
    "$RES/mipmap-$d/ic_launcher_round.webp"
  echo "  mipmap-$d  ${s}x${s}"
done

# 3) adaptive-icon foreground layer: 108dp canvas rendered at 4x = 432px
mkdir -p "$RES/drawable-nodpi"
convert "$TMP/square.png" -resize 432x432 -strip "$RES/drawable-nodpi/ic_launcher_foreground.png"
echo "  drawable-nodpi/ic_launcher_foreground.png 432x432"

# 4) store-listing asset
mkdir -p art
convert "$TMP/square.png" -resize 512x512 -strip art/sayvis-icon-512.png
echo "  art/sayvis-icon-512.png 512x512"

echo "done. launcher assets regenerated from: $SRC"
