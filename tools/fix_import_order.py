"""
Reorders `import` blocks to the layout detekt-formatting's ImportOrdering rule
expects: everything lexicographic first, then java.**, javax.**, kotlin.**,
and aliases last.

This exists because the package rename moved every first-party import from
`com.flowpay.app.*` to `vijayaapardhu.dev.fastpay.*`, which sorts *after*
`kotlinx.*` and `java.*` - so ~25 files were left with their own imports in the
middle of the block. Fixing that by hand across the tree is exactly the kind of
mechanical edit that goes wrong quietly, so it is scripted and reviewable.

Usage:  python tools/fix_import_order.py [--check]

--check exits non-zero and prints the offending files instead of rewriting, so
it can be used as a pre-commit guard.
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_DIRS = [ROOT / "app" / "src"]

JAVA_BUCKETS = ("java.", "javax.", "kotlin.")


def bucket(line: str) -> int:
    """0 = normal, 1 = java/javax/kotlin, 2 = alias."""
    if " as " in line:
        return 2
    path = line[len("import "):]
    if path.startswith(JAVA_BUCKETS):
        return 1
    return 0


def sort_key(line: str) -> tuple[int, str]:
    return (bucket(line), line)


def find_import_blocks(lines: list[str]) -> list[tuple[int, int]]:
    """Contiguous [start, end) index ranges that consist only of imports."""
    blocks: list[tuple[int, int]] = []
    start = None
    for i, line in enumerate(lines):
        if line.startswith("import "):
            if start is None:
                start = i
        else:
            if start is not None:
                blocks.append((start, i))
                start = None
    if start is not None:
        blocks.append((start, len(lines)))
    return blocks


def process(path: Path, check: bool) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.split("\n")
    changed = False

    for start, end in find_import_blocks(lines):
        block = lines[start:end]
        ordered = sorted(block, key=sort_key)
        if ordered != block:
            changed = True
            if not check:
                lines[start:end] = ordered

    if changed and not check:
        path.write_text("\n".join(lines), encoding="utf-8")
    return changed


def main() -> int:
    check = "--check" in sys.argv
    offenders: list[Path] = []

    for src in SRC_DIRS:
        for path in src.rglob("*.kt"):
            if "/build/" in path.as_posix():
                continue
            if process(path, check):
                offenders.append(path)

    if check:
        if offenders:
            print("imports need reordering in:")
            for p in offenders:
                print(f"  {p.relative_to(ROOT)}")
            return 1
        print("import ordering OK")
        return 0

    print(f"reordered imports in {len(offenders)} file(s)")
    for p in sorted(offenders):
        print(f"  {p.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())