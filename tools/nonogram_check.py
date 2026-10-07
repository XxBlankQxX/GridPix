#!/usr/bin/env python3
"""Validate GridPix pack JSON files: shape, characters and line-logic solvability.

Usage:  python tools/nonogram_check.py app/src/main/assets/packs/animals.json [more.json ...]
        python tools/nonogram_check.py --grid ".##..|#..#.|..." (quick check of one grid, rows joined by |)

A puzzle passes only when the same line-logic solver the app uses (game/LineSolver.kt and
game/PuzzleSolver.kt, ported here) determines every cell without guessing. For a failing
puzzle the ambiguous cells are printed as '?' so the artist can tweak them.

Exit code 0 when every puzzle passes, 1 otherwise.
"""
import json
import re
import sys

UNKNOWN, FILLED, EMPTY = 0, 1, 2
PUZZLES_PER_PACK = 30
# (small, large): puzzles 1-20 use the small size, 21-30 the large size. Must match PackContentTest.kt.
DEFAULT_SIZES = (10, 15)
PACK_SIZES = {"anime": (15, 20)}
HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")


def line_clue(cells):
    clue, run = [], 0
    for c in cells:
        if c:
            run += 1
        elif run:
            clue.append(run)
            run = 0
    if run:
        clue.append(run)
    return clue


def solve_line(clue, line):
    """Return deduced line or None on contradiction. Enumerates placements consistent with known cells."""
    n = len(line)
    if not clue:
        return None if FILLED in line else [EMPTY] * n
    can_fill = [False] * n
    can_empty = [False] * n
    starts = [0] * len(clue)
    found = [False]
    min_span = [0] * (len(clue) + 1)
    for b in range(len(clue) - 1, -1, -1):
        min_span[b] = min_span[b + 1] + clue[b] + (1 if b + 1 < len(clue) else 0)

    def fits(start, length):
        for i in range(start, start + length):
            if line[i] == EMPTY:
                return False
        after = start + length
        return after >= n or line[after] != FILLED

    def record():
        found[0] = True
        b = i = 0
        while i < n:
            if b < len(clue) and i == starts[b]:
                for _ in range(clue[b]):
                    can_fill[i] = True
                    i += 1
                b += 1
            else:
                can_empty[i] = True
                i += 1

    def place(b, frm):
        if b == len(clue):
            for i in range(frm, n):
                if line[i] == FILLED:
                    return
            record()
            return
        length = clue[b]
        last = n - min_span[b]
        start = frm
        while start <= last:
            if start > frm and line[start - 1] == FILLED:
                return
            if fits(start, length):
                starts[b] = start
                place(b + 1, start + length + 1)
            start += 1

    place(0, 0)
    if not found[0]:
        return None
    out = []
    for i in range(n):
        if can_fill[i] and not can_empty[i]:
            out.append(FILLED)
        elif can_empty[i] and not can_fill[i]:
            out.append(EMPTY)
        else:
            out.append(line[i])
    return out


def solve(rows_clues, cols_clues):
    n = len(rows_clues)
    grid = [[UNKNOWN] * n for _ in range(n)]
    row_dirty = [True] * n
    col_dirty = [True] * n
    progress = True
    while progress:
        progress = False
        for r in range(n):
            if not row_dirty[r]:
                continue
            row_dirty[r] = False
            res = solve_line(rows_clues[r], grid[r])
            if res is None:
                return None
            for c in range(n):
                if res[c] != grid[r][c]:
                    grid[r][c] = res[c]
                    col_dirty[c] = True
                    progress = True
        for c in range(n):
            if not col_dirty[c]:
                continue
            col_dirty[c] = False
            res = solve_line(cols_clues[c], [grid[r][c] for r in range(n)])
            if res is None:
                return None
            for r in range(n):
                if res[r] != grid[r][c]:
                    grid[r][c] = res[r]
                    row_dirty[r] = True
                    progress = True
    return grid


def check_grid(rows, allowed=None):
    """Return (ok, message). rows: list of strings of '#' and '.'."""
    n = len(rows)
    if allowed and n not in allowed:
        return False, f"size {n} not in {sorted(allowed)}"
    for r, row in enumerate(rows):
        if len(row) != n:
            return False, f"row {r} has {len(row)} chars, expected {n}"
        bad = set(row) - {"#", "."}
        if bad:
            return False, f"row {r} has invalid chars {sorted(bad)}"
    cells = [[ch == "#" for ch in row] for row in rows]
    filled = sum(sum(row) for row in cells)
    if filled == 0:
        return False, "picture is empty"
    rows_clues = [line_clue(row) for row in cells]
    cols_clues = [line_clue([cells[r][c] for r in range(n)]) for c in range(n)]
    grid = solve(rows_clues, cols_clues)
    if grid is None:
        return False, "contradiction (bug)"
    unknown = sum(1 for r in range(n) for c in range(n) if grid[r][c] == UNKNOWN)
    if unknown == 0:
        return True, f"ok ({filled}/{n*n} filled, {filled*100//(n*n)}%)"
    lines = []
    for r in range(n):
        lines.append("".join("?" if grid[r][c] == UNKNOWN else ("#" if cells[r][c] else ".") for c in range(n)))
    return False, f"{unknown} cells not deducible by line logic (shown as ?):\n    " + "\n    ".join(lines)


def check_colors(p):
    """Colour map for the solved reveal. Returns a problem string or None.

    "palette": {"a": "#RRGGBB", ...}  single lowercase letters (or digits) -> colour
    "colors":  same shape as "grid"; '.' exactly where grid has '.', a palette key where grid has '#'.
    """
    grid = p.get("grid", [])
    palette = p.get("palette")
    colors = p.get("colors")
    if palette is None or colors is None:
        return "missing 'palette' or 'colors'"
    if not isinstance(palette, dict) or not palette:
        return "palette must be a non-empty object"
    for k, v in palette.items():
        if len(k) != 1 or k in ".#" or not (k.islower() or k.isdigit()):
            return f"palette key {k!r} must be one lowercase letter or digit"
        if not HEX.match(str(v)):
            return f"palette value {v!r} must look like #RRGGBB"
    if len(colors) != len(grid):
        return f"colors has {len(colors)} rows, grid has {len(grid)}"
    for r, (crow, grow) in enumerate(zip(colors, grid)):
        if len(crow) != len(grow):
            return f"colors row {r} length {len(crow)} != {len(grow)}"
        for c, (ch, g) in enumerate(zip(crow, grow)):
            if g == "." and ch != ".":
                return f"colors row {r} col {c}: '{ch}' on an empty cell (must be '.')"
            if g == "#" and ch not in palette:
                return f"colors row {r} col {c}: '{ch}' is not a palette key (cell is filled)"
    return None


def check_pack(path):
    with open(path, encoding="utf-8") as f:
        pack = json.load(f)
    problems = 0
    for key in ("id", "name", "puzzles"):
        if key not in pack:
            print(f"{path}: missing '{key}'")
            problems += 1
    puzzles = pack.get("puzzles", [])
    if len(puzzles) != PUZZLES_PER_PACK:
        print(f"{path}: {len(puzzles)} puzzles, expected {PUZZLES_PER_PACK}")
        problems += 1
    names = set()
    sizes = {}
    small, large = PACK_SIZES.get(pack.get("id"), DEFAULT_SIZES)
    for i, p in enumerate(puzzles, start=1):
        name = p.get("name", "")
        if not name or name in names:
            print(f"{path} #{i:02d}: missing or duplicate name '{name}'")
            problems += 1
        names.add(name)
        if "picross" in name.lower():
            print(f"{path} #{i:02d}: forbidden word in name")
            problems += 1
        expected = small if i <= 20 else large
        ok, msg = check_grid(p.get("grid", []), {expected})
        if ok:
            color_problem = check_colors(p)
            if color_problem:
                ok, msg = False, "colour map: " + color_problem
        sizes[len(p.get("grid", []))] = sizes.get(len(p.get("grid", [])), 0) + 1
        status = "OK  " if ok else "FAIL"
        print(f"{status} {path} #{i:02d} {name!r}: {msg}")
        if not ok:
            problems += 1
    print(f"{path}: sizes {sizes}, {problems} problem(s)")
    return problems == 0


def main(argv):
    if len(argv) >= 2 and argv[0] == "--grid":
        ok, msg = check_grid(argv[1].split("|"))
        print(("OK   " if ok else "FAIL ") + msg)
        return 0 if ok else 1
    if not argv:
        print(__doc__)
        return 2
    all_ok = True
    for path in argv:
        if not check_pack(path):
            all_ok = False
    return 0 if all_ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
