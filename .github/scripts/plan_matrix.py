#!/usr/bin/env python3
"""Pick the builds a workflow run needs, so unchanged versions are never built.

  plan_matrix.py build   [--before SHA] [--sha SHA] [--files F...]   builds whose files changed in the push or pull request
  plan_matrix.py release [--tag vYYYY.M.N]                           builds whose own mod_version equals the tag

SMWTI keeps every build self-contained in versions/mc<minecraft>/<loader>/ (own gradle project, own
sources, own libs); nothing is shared between builds at compile time. So a change selects the builds
whose folder it touches, files that never reach a jar select nothing, and anything not listed below
selects every build (fail safe).

Writes `matrix=<json>` and `any=true|false` to $GITHUB_OUTPUT (and prints them).
"""
import argparse
import json
import os
import re
import subprocess
import sys

# (module, folder, java, minecraft version, loader, CurseForge game version ids)
ALL = [
    ("Forge 1.20.1", "versions/mc1.20.1/forge", "17", "1.20.1", "forge", "9990,7498,9638,9639"),
    ("NeoForge 1.20.1", "versions/mc1.20.1/neoforge", "17", "1.20.1", "neoforge", "9990,10150,9638,9639"),
    ("Forge 1.21.1", "versions/mc1.21.1/forge", "21", "1.21.1", "forge", "11779,7498,9638,9639"),
    ("NeoForge 1.21.1", "versions/mc1.21.1/neoforge", "21", "1.21.1", "neoforge", "11779,10150,9638,9639"),
    ("Forge 26.1.2", "versions/mc26.1.2/forge", "25", "26.1.2", "forge", "16082,7498,9638,9639"),
    ("NeoForge 26.1.2", "versions/mc26.1.2/neoforge", "25", "26.1.2", "neoforge", "16082,10150,9638,9639"),
    ("Forge 26.2", "versions/mc26.2/forge", "25", "26.2", "forge", "16498,7498,9638,9639"),
    ("NeoForge 26.2", "versions/mc26.2/neoforge", "25", "26.2", "neoforge", "16498,10150,9638,9639"),
    ("Forge 26.3", "versions/mc26.3/forge", "25", "26.3", "forge", "17045,7498,9638,9639"),
    ("NeoForge 26.3", "versions/mc26.3/neoforge", "25", "26.3", "neoforge", "17045,10150,9638,9639"),
]
ZERO = "0" * 40

# Files and folders that never reach a jar. Checked against the gradle files: no build reads anything
# outside its own versions/mc*/<loader>/ folder (no ../ source sets, no root resources).
NONE_PREFIXES = (
    "branding/", "curseforge/", "docs/", "wiki/", "dist/", "logs/", "reference/", "concept/",
    ".github/workflows/", ".github/scripts/", ".github/ISSUE_TEMPLATE/", ".claude/",
)
NONE_EXACT = {
    ".gitignore", ".gitattributes", "LICENSE", "PROJECT_STATUS.md", "PLAN.md", "RODIN_PROMPTS.md",
    ".github/pull_request_template.md", "server.out", "server.err",
}
NONE_SUFFIXES = (".md", ".log", ".err", ".out")
# Inside one build folder: run folders, logs and build output are not inputs of the jar.
MODULE_NONE = re.compile(r"^(run[^/]*|logs|build|\.gradle|devlibs)/")
MODULE_PATH = re.compile(r"^versions/mc([^/]+)/(forge|neoforge)/(.*)$")


def changed_files(before, sha):
    """Files changed between two commits; None when git cannot say (the caller then builds everything)."""
    if not before or before == ZERO:
        # first push of a branch or a manual run: compare with the parent commit
        before = sha + "^"
    try:
        out = subprocess.run(["git", "diff", "--name-only", before, sha], capture_output=True, text=True)
    except OSError:
        return None
    if out.returncode != 0:
        return None
    return [line.strip() for line in out.stdout.splitlines() if line.strip()]


def module_version(folder):
    try:
        with open(os.path.join(folder, "gradle.properties"), encoding="utf-8") as fh:
            text = fh.read()
    except OSError:
        return None
    m = re.search(r"^mod_version=(\S+)", text, re.M)
    return m.group(1) if m else None


def pick_for_files(files):
    """The ALL entries a list of changed paths needs. None for the list means unknown: everything."""
    if files is None:
        return list(ALL)
    chosen = set()
    for raw in files:
        f = raw.replace("\\", "/")
        if f.startswith("./"):
            f = f[2:]
        if f in NONE_EXACT or f.startswith(NONE_PREFIXES):
            continue
        m = MODULE_PATH.match(f)
        if m:
            folder = f"versions/mc{m.group(1)}/{m.group(2)}"
            if not any(entry[1] == folder for entry in ALL):
                return list(ALL)  # a build folder this script does not know: be safe
            if MODULE_NONE.match(m.group(3)):
                continue
            chosen.add(folder)
            continue
        if f.endswith(NONE_SUFFIXES):
            continue  # notes and logs never reach a jar (no .md under versions/ is tracked)
        return list(ALL)  # unknown path: build everything
    return [entry for entry in ALL if entry[1] in chosen]


def pick_for_release(version, root="."):
    """Entries whose own mod_version equals the release version."""
    return [entry for entry in ALL if module_version(os.path.join(root, entry[1])) == version]


def newest_version(root="."):
    found = {module_version(os.path.join(root, e[1])) for e in ALL} - {None}
    key = lambda v: [int(x) if x.isdigit() else 0 for x in re.split(r"[.]", v)]
    return sorted(found, key=key)[-1] if found else ""


def matrix_for(picked, release):
    include = []
    for name, folder, java, mc, loader, cf in picked:
        item = {"name": name, "dir": folder, "java": java, "mc": mc, "loader": loader}
        if release:
            item["cf_game_versions"] = cf
        include.append(item)
    return {"include": include}


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["build", "release"])
    ap.add_argument("--before")
    ap.add_argument("--sha", default="HEAD")
    ap.add_argument("--tag", help="release version, with or without the leading v; empty means the newest version")
    ap.add_argument("--files", nargs="*", help="changed files to classify instead of asking git (for tests)")
    args = ap.parse_args(argv)

    if args.mode == "release":
        version = (args.tag or "").lstrip("v")
        if not version:
            version = newest_version()  # a manual run without a release version: the newest one
        picked = pick_for_release(version)
    else:
        files = args.files if args.files is not None else changed_files(args.before, args.sha)
        picked = pick_for_files(files)

    matrix = matrix_for(picked, args.mode == "release")
    lines = [f"matrix={json.dumps(matrix)}", f"any={'true' if picked else 'false'}"]
    print("\n".join(lines))
    print("builds:", ", ".join(m[0] for m in picked) or "none")
    out = os.environ.get("GITHUB_OUTPUT")
    if out:
        with open(out, "a", encoding="utf-8") as fh:
            fh.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
