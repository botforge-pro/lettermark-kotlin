#!/usr/bin/env python3
"""Builds the grapheme table this port segments with, from the pinned UCD."""

import sys
import urllib.request
from pathlib import Path

UNICODE_VERSION = "16.0.0"
BASE = f"https://www.unicode.org/Public/{UNICODE_VERSION}/ucd"
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/graphemes.bin"

CLASSES = [
    "Other", "CR", "LF", "Control", "Extend", "ZWJ", "Regional_Indicator",
    "Prepend", "SpacingMark", "L", "V", "T", "LV", "LVT",
]
EXT_PICT = 0x10
INCB_LINKER = 0x20
INCB_CONSONANT = 0x40
INCB_EXTEND = 0x80


def fetch(path):
    with urllib.request.urlopen(f"{BASE}/{path}") as response:
        return response.read().decode("utf-8")


def entries(text, wanted):
    for line in text.splitlines():
        line = line.split("#", 1)[0].strip()
        if not line:
            continue
        fields = [f.strip() for f in line.split(";")]
        if len(fields) < 2:
            continue
        name = fields[1] if len(fields) == 2 else f"{fields[1]}/{fields[2]}"
        if name not in wanted:
            continue
        span = fields[0].split("..")
        start = int(span[0], 16)
        end = int(span[-1], 16)
        yield start, end, name


def main():
    props = {}

    def paint(start, end, bits, replace_class=False):
        for code in range(start, end + 1):
            current = props.get(code, 0)
            if replace_class:
                current = (current & 0xF0) | bits
            else:
                current |= bits
            props[code] = current

    breaks = fetch("auxiliary/GraphemeBreakProperty.txt")
    for start, end, name in entries(breaks, set(CLASSES)):
        paint(start, end, CLASSES.index(name), replace_class=True)

    emoji = fetch("emoji/emoji-data.txt")
    for start, end, _ in entries(emoji, {"Extended_Pictographic"}):
        paint(start, end, EXT_PICT)

    derived = fetch("DerivedCoreProperties.txt")
    for start, end, name in entries(derived, {"InCB/Linker", "InCB/Consonant", "InCB/Extend"}):
        bit = {
            "InCB/Linker": INCB_LINKER,
            "InCB/Consonant": INCB_CONSONANT,
            "InCB/Extend": INCB_EXTEND,
        }[name]
        paint(start, end, bit)

    ranges = []
    for code in sorted(props):
        value = props[code]
        if ranges and ranges[-1][1] == code - 1 and ranges[-1][2] == value:
            ranges[-1][1] = code
        else:
            ranges.append([code, code, value])

    out = bytearray()
    out += UNICODE_VERSION.encode("ascii") + b"\n"
    previous_end = -1
    for start, end, value in ranges:
        write_varint(out, start - previous_end - 1)
        write_varint(out, end - start)
        out.append(value)
        previous_end = end

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_bytes(bytes(out))
    print(f"{len(ranges)} ranges, {len(out)} bytes -> {OUT}", file=sys.stderr)


def write_varint(out, value):
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return


if __name__ == "__main__":
    main()
