"""Tests for plan_matrix.py. Run from the repo root: python -I .github/scripts/test_plan_matrix.py"""
import os
import subprocess
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, HERE)
import plan_matrix as pm  # noqa: E402

FOLDERS = [e[1] for e in pm.ALL]


def folders(picked):
    return sorted(e[1] for e in picked)


def commit_exists(sha):
    r = subprocess.run(["git", "cat-file", "-e", sha + "^{commit}"], cwd=ROOT, capture_output=True)
    return r.returncode == 0


def real_diff(sha):
    r = subprocess.run(["git", "diff", "--name-only", sha + "^", sha], cwd=ROOT, capture_output=True, text=True)
    return [l for l in r.stdout.splitlines() if l.strip()]


class SyntheticFiles(unittest.TestCase):
    def test_docs_and_status_select_nothing(self):
        files = ["README.md", "CHANGELOG.md", "PROJECT_STATUS.md", "TRANSLATING.md", "curseforge/DESCRIPTION.md",
                 "branding/banners/banner.png", "docs/x.md", "LICENSE", ".gitignore", "client.err", "shots.out"]
        self.assertEqual(pm.pick_for_files(files), [])

    def test_workflow_only_selects_nothing(self):
        self.assertEqual(pm.pick_for_files([".github/workflows/build.yml", ".github/workflows/release.yml",
                                            ".github/scripts/plan_matrix.py"]), [])

    def test_single_module(self):
        picked = pm.pick_for_files(["versions/mc1.20.1/neoforge/src/main/java/A.java", "CHANGELOG.md"])
        self.assertEqual(folders(picked), ["versions/mc1.20.1/neoforge"])

    def test_module_gradle_files_select_that_module(self):
        picked = pm.pick_for_files(["versions/mc26.2/forge/build.gradle", "versions/mc26.2/forge/gradle/wrapper/gradle-wrapper.jar"])
        self.assertEqual(folders(picked), ["versions/mc26.2/forge"])

    def test_one_minecraft_version_both_loaders(self):
        picked = pm.pick_for_files(["versions/mc26.3/forge/gradle.properties", "versions/mc26.3/neoforge/gradle.properties"])
        self.assertEqual(folders(picked), ["versions/mc26.3/forge", "versions/mc26.3/neoforge"])

    def test_run_logs_and_build_output_inside_a_module_select_nothing(self):
        files = ["versions/mc1.20.1/forge/logs/debug-1.log.gz", "versions/mc1.20.1/forge/run/options.txt",
                 "versions/mc1.20.1/forge/run-data/config/fml.toml", "versions/mc1.20.1/forge/build/libs/x.jar"]
        self.assertEqual(pm.pick_for_files(files), [])

    def test_unknown_path_selects_everything(self):
        self.assertEqual(len(pm.pick_for_files(["tools/new_script.sh"])), len(pm.ALL))
        self.assertEqual(len(pm.pick_for_files(["README.md", "settings.gradle"])), len(pm.ALL))

    def test_unknown_build_folder_selects_everything(self):
        self.assertEqual(len(pm.pick_for_files(["versions/mc27.0/forge/build.gradle"])), len(pm.ALL))

    def test_diff_failure_selects_everything(self):
        self.assertEqual(len(pm.pick_for_files(None)), len(pm.ALL))
        self.assertIsNone(pm.changed_files("0123456789abcdef0123456789abcdef01234567", "also-not-a-commit"))

    def test_matrix_fields(self):
        item = pm.matrix_for(pm.ALL[:1], False)["include"][0]
        self.assertEqual(sorted(item), ["dir", "java", "loader", "mc", "name"])
        item = pm.matrix_for(pm.ALL[:1], True)["include"][0]
        self.assertIn("cf_game_versions", item)


class ReleaseMode(unittest.TestCase):
    def make_tree(self, versions):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        for folder, version in versions.items():
            os.makedirs(os.path.join(tmp.name, folder))
            with open(os.path.join(tmp.name, folder, "gradle.properties"), "w", encoding="utf-8") as fh:
                fh.write(f"# comment\nmod_version={version}\n")
        return tmp.name

    def test_only_matching_versions(self):
        v = {f: "2026.11.2" for f in FOLDERS}
        v["versions/mc26.3/forge"] = "2026.11.5"
        v["versions/mc26.3/neoforge"] = "2026.11.5"
        root = self.make_tree(v)
        self.assertEqual(folders(pm.pick_for_release("2026.11.5", root)), ["versions/mc26.3/forge", "versions/mc26.3/neoforge"])
        self.assertEqual(len(pm.pick_for_release("2026.11.2", root)), 8)
        self.assertEqual(pm.pick_for_release("2026.11.9", root), [])
        self.assertEqual(pm.newest_version(root), "2026.11.5")

    def test_all_modules_carry_the_tag(self):
        root = self.make_tree({f: "2026.12.1" for f in FOLDERS})
        self.assertEqual(len(pm.pick_for_release("2026.12.1", root)), 10)

    def test_newest_version_sorts_numerically(self):
        v = {f: "2026.11.9" for f in FOLDERS}
        v["versions/mc26.3/forge"] = "2026.11.10"
        self.assertEqual(pm.newest_version(self.make_tree(v)), "2026.11.10")


class MainCli(unittest.TestCase):
    def run_cli(self, *args):
        r = subprocess.run([sys.executable, "-I", os.path.join(HERE, "plan_matrix.py"), *args], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        return r.stdout

    def test_build_with_files(self):
        out = self.run_cli("build", "--files", "README.md", ".github/workflows/build.yml")
        self.assertIn("any=false", out)
        out = self.run_cli("build", "--files", "versions/mc26.2/forge/src/x.java")
        self.assertIn("any=true", out)
        self.assertIn("Forge 26.2", out)

    def test_bad_tag_selects_nothing(self):
        self.assertIn("any=false", self.run_cli("release", "--tag", "v1999.1.1"))
        self.assertIn("any=false", self.run_cli("release", "--tag", "v2026.11.1-rc"))

    def test_build_with_unusable_range_selects_everything(self):
        out = self.run_cli("build", "--before", "0123456789abcdef0123456789abcdef01234567", "--sha", "HEAD")
        self.assertIn("builds: Forge 1.20.1", out)
        self.assertIn("any=true", out)


class RealCommits(unittest.TestCase):
    def check(self, sha, expected):
        if not commit_exists(sha):
            self.skipTest(f"commit {sha} is not in this clone")
        self.assertEqual(folders(pm.pick_for_files(real_diff(sha))), sorted(expected))

    def test_changelog_only(self):
        self.check("6c0526d", [])

    def test_release_workflow_only(self):
        self.check("825462d", [])

    def test_workflow_gitignore_and_logs(self):
        self.check("355273a", [])

    def test_tracked_run_logs_removed(self):
        self.check("f939123", [])

    def test_single_module(self):
        self.check("b0789e7", ["versions/mc1.20.1/neoforge"])

    def test_one_minecraft_version(self):
        self.check("4987b04", ["versions/mc26.3/forge", "versions/mc26.3/neoforge"])

    def test_six_older_builds(self):
        self.check("323cc06", [e for e in FOLDERS if "mc26.2" not in e and "mc26.3" not in e])


class CurrentTree(unittest.TestCase):
    def test_every_module_has_a_version(self):
        for e in pm.ALL:
            self.assertTrue(pm.module_version(os.path.join(ROOT, e[1])), e[1])

    def test_matrix_matches_the_repo_folders(self):
        on_disk = sorted(f"versions/{a}/{b}" for a in os.listdir(os.path.join(ROOT, "versions"))
                         for b in ("forge", "neoforge") if os.path.isfile(os.path.join(ROOT, "versions", a, b, "gradle.properties")))
        self.assertEqual(sorted(FOLDERS), on_disk)


if __name__ == "__main__":
    unittest.main()
