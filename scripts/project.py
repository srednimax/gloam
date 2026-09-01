#!/usr/bin/env python3
"""Who this app is — the one place the toolchain looks it up.

Every script in `scripts/` that needs the package name or a Kotlin constant imports
it from here rather than hard-coding it. `bootstrap.py` rewrites this
file once when you clone the template, and after that nothing else has to be hunted for.

**Namespace and applicationId are parsed out of `app/build.gradle.kts`, not stored here.**
Gradle is the build's source of truth for both, and a second copy of a value that must
agree with the build is a copy that will one day disagree with it. Everything the build
file does *not* know — the names of the schema constants, say — is a plain constant
below.

Import it from a sibling script like this:

    import sys, pathlib
    sys.path.insert(0, str(pathlib.Path(__file__).parent))
    import project
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BUILD_FILE = ROOT / "app/build.gradle.kts"


def _from_build(key: str) -> str:
    """Read `key = "value"` out of app/build.gradle.kts.

    Deliberately a regex over the text rather than anything that understands Kotlin: this
    has to run in CI, on a phone-less laptop and inside a git hook, and none of those want
    to start a Gradle daemon to answer "what is the package called".
    """
    text = BUILD_FILE.read_text(encoding="utf-8")
    match = re.search(rf'^\s*{key}\s*=\s*"([^"]+)"', text, re.MULTILINE)
    if not match:
        print(f"project.py: no `{key} = \"…\"` in {BUILD_FILE}", file=sys.stderr)
        raise SystemExit(2)
    return match.group(1)


# The Kotlin package root — `app.starter`. Source lives at app/src/main/java/<this, as dirs>/.
NAMESPACE = _from_build("namespace")

# The install identity — what `adb` and the Play Console call the app. Deliberately allowed to
# differ from NAMESPACE: a Play Console package name can never be changed once the app entry
# exists, while a Kotlin package can be refactored any afternoon.
APPLICATION_ID = _from_build("applicationId")

# The debug build takes an applicationIdSuffix, so it installs alongside a Play copy of the same
# app instead of replacing it. Every adb-driving script wants *this* one, never APPLICATION_ID.
DEBUG_APPLICATION_ID = f"{APPLICATION_ID}.debug"

# The human name, for generated art and log lines. The authority for what the launcher shows is
# `app_name` in res/values/strings.xml; this is only for things outside the APK.
APP_NAME = "Gloam"


def _debug_app_name() -> str:
    """What the **debug** build's launcher label actually says.

    Parsed rather than derived, and the reason is a bug this cost a device session to find: the
    debug source set overrides `app_name` with **"Gloam debug"**, and `device-gate.py` was looking
    for `f"{APP_NAME} Debug"` — a capital D the app has never had. Every OEM settings screen lists
    an app under this string, so a scrape aimed at the wrong one finds no row at all, cannot set the
    toggle, and reports *not allowed* for an app that may well be allowed. A wrong answer that looks
    like a reading is worse than an error.
    """
    path = ROOT / "app/src/debug/res/values/strings.xml"
    if path.exists():
        match = re.search(r'<string name="app_name"[^>]*>([^<]+)</string>', path.read_text(encoding="utf-8"))
        if match:
            return match.group(1)
    return APP_NAME


# The label the launcher, and every OEM permission screen, shows for the installed debug build.
DEBUG_APP_NAME = _debug_app_name()

# --- Room, parked ------------------------------------------------------------------------------
# **There is no database** (ADR-0007), so there is no DATABASE_FILE here and nothing pulls one off
# the phone. These two survive because `schema-gate.py` survives: it reports "no database, nothing
# to gate" and passes, and it comes back whole the day a feature adds a table. Renaming these in
# Kotlin means renaming them here.
SCHEMA_VERSION_CONST = "APP_SCHEMA_VERSION"
MIGRATIONS_CONST = "APP_MIGRATIONS"

# --- Paths, all derived so a namespace change moves them all --------------------------------
PACKAGE_DIR = NAMESPACE.replace(".", "/")
MAIN_SRC = ROOT / "app/src/main/java" / PACKAGE_DIR
TEST_SRC = ROOT / "app/src/test/java" / PACKAGE_DIR
ANDROID_TEST_SRC = ROOT / "app/src/androidTest/java" / PACKAGE_DIR
DEBUG_SRC = ROOT / "app/src/debug/java" / PACKAGE_DIR
RES = ROOT / "app/src/main/res"

MAIN_ACTIVITY = f"{DEBUG_APPLICATION_ID}/{NAMESPACE}.MainActivity"


if __name__ == "__main__":
    # `python3 scripts/project.py` prints what everything else is working from. Worth running
    # first when a script reports a package that surprises you.
    for name, value in [
        ("APP_NAME", APP_NAME),
        ("DEBUG_APP_NAME", DEBUG_APP_NAME),
        ("NAMESPACE", NAMESPACE),
        ("APPLICATION_ID", APPLICATION_ID),
        ("DEBUG_APPLICATION_ID", DEBUG_APPLICATION_ID),
        ("MAIN_ACTIVITY", MAIN_ACTIVITY),
    ]:
        print(f"{name:22} {value}")
