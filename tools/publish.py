"""
Заливает все jar из dist/ на Modrinth и CurseForge.

    python tools/publish.py            # пробный прогон: показывает, что и куда уйдёт
    python tools/publish.py --upload   # реально загружает

Переменные окружения (токены не печатаются и никуда, кроме API сайтов, не уходят):
    MODRINTH_TOKEN      Personal Access Token с правами "Create versions" (modrinth.com/settings/pats)
    MODRINTH_PROJECT    slug или id проекта на Modrinth (например cube-overlay)
    CURSEFORGE_TOKEN    API-токен автора (legacy.curseforge.com/account/api-tokens)
    CURSEFORGE_PROJECT  числовой Project ID с CurseForge (справа на странице проекта)
    CURSEFORGE_MCEF_SLUG  необязательно: slug MCEF Modern на CurseForge, если он там есть
Без переменных площадка просто пропускается.
"""
import json
import os
import re
import sys
from pathlib import Path

import requests

# консоль Windows по умолчанию в cp1252 и падает на русском тексте
sys.stdout.reconfigure(encoding="utf-8")

ROOT = Path(__file__).resolve().parent.parent
DIST = ROOT / "dist"
CHANGELOG = (ROOT / "publish" / "CHANGELOG.md").read_text(encoding="utf-8")
UA = "cube-overlay-publisher/1.0"

# под какие версии игры помечать каждый jar и какая Java нужна
TARGETS = {
    "1.21.11": (["1.21.11"], "Java 21"),
    "26.1.2": (["26.1", "26.1.1", "26.1.2"], "Java 25"),
    "26.2": (["26.2"], "Java 25"),
    "26.3": (["26.3"], "Java 25"),
}
MODRINTH_DEPS = [
    {"project_id": "P7dR8mSH", "dependency_type": "required"},  # Fabric API
    {"project_id": "YJFfu5pH", "dependency_type": "required"},  # MCEF Modern
]


def jars():
    for jar in sorted(DIST.glob("cube-overlay-*.jar")):
        m = re.match(r"cube-overlay-(.+)\+mc(.+)\.jar", jar.name)
        if not m or m.group(2) not in TARGETS:
            sys.exit(f"не понимаю версию в имени {jar.name}")
        mod_version, mc = m.groups()
        game_versions, java = TARGETS[mc]
        yield jar, mod_version, mc, game_versions, java


def modrinth(upload: bool):
    token, project = os.environ.get("MODRINTH_TOKEN"), os.environ.get("MODRINTH_PROJECT")
    if not token or not project:
        print("Modrinth: нет MODRINTH_TOKEN / MODRINTH_PROJECT — пропускаю")
        return
    headers = {"Authorization": token, "User-Agent": UA}
    r = requests.get(f"https://api.modrinth.com/v2/project/{project}", headers=headers, timeout=30)
    r.raise_for_status()
    project_id = r.json()["id"]
    existing = {v["version_number"] for v in
                requests.get(f"https://api.modrinth.com/v2/project/{project_id}/version", headers=headers, timeout=30).json()}
    for jar, mod_version, mc, game_versions, _ in jars():
        number = f"{mod_version}+mc{mc}"
        if number in existing:
            print(f"Modrinth: {number} уже есть — пропускаю")
            continue
        data = {
            "project_id": project_id,
            "name": f"Cube Overlay {mod_version} for {mc}",
            "version_number": number,
            "changelog": CHANGELOG,
            "dependencies": MODRINTH_DEPS,
            "game_versions": game_versions,
            "version_type": "release",
            "loaders": ["fabric"],
            "featured": mc == "26.3",
            "file_parts": ["file"],
            "primary_file": "file",
        }
        print(f"Modrinth: {jar.name} -> {game_versions}")
        if upload:
            with jar.open("rb") as f:
                r = requests.post("https://api.modrinth.com/v2/version", headers=headers, timeout=120,
                                  data={"data": json.dumps(data)},
                                  files={"file": (jar.name, f, "application/java-archive")})
            if r.status_code >= 300:
                sys.exit(f"Modrinth ответил {r.status_code}: {r.text}")
            print(f"  готово: https://modrinth.com/mod/{project}/version/{r.json()['id']}")


def curseforge(upload: bool):
    token, project = os.environ.get("CURSEFORGE_TOKEN"), os.environ.get("CURSEFORGE_PROJECT")
    if not token or not project:
        print("CurseForge: нет CURSEFORGE_TOKEN / CURSEFORGE_PROJECT — пропускаю")
        return
    api = "https://minecraft.curseforge.com/api"
    headers = {"X-Api-Token": token, "User-Agent": UA}
    versions = requests.get(f"{api}/game/versions", headers=headers, timeout=30)
    versions.raise_for_status()
    by_name = {}
    for v in versions.json():
        by_name.setdefault(v["name"], v["id"])

    relations = [{"slug": "fabric-api", "type": "requiredDependency"}]
    if os.environ.get("CURSEFORGE_MCEF_SLUG"):
        relations.append({"slug": os.environ["CURSEFORGE_MCEF_SLUG"], "type": "requiredDependency"})

    for jar, mod_version, mc, game_versions, java in jars():
        names = game_versions + ["Fabric", "Client", java]
        ids, missing = [], []
        for n in names:
            (ids if n in by_name else missing).append(by_name.get(n, n))
        if missing:
            print(f"CurseForge: для {jar.name} нет версий {missing} на сайте — пропускаю этот jar")
            continue
        meta = {
            "changelog": CHANGELOG,
            "changelogType": "markdown",
            "displayName": f"Cube Overlay {mod_version} for {mc}",
            "gameVersions": ids,
            "releaseType": "release",
            "relations": {"projects": relations},
        }
        print(f"CurseForge: {jar.name} -> {names}")
        if upload:
            with jar.open("rb") as f:
                r = requests.post(f"{api}/projects/{project}/upload-file", headers=headers, timeout=120,
                                  data={"metadata": json.dumps(meta)},
                                  files={"file": (jar.name, f, "application/java-archive")})
            if r.status_code >= 300:
                sys.exit(f"CurseForge ответил {r.status_code}: {r.text}")
            print(f"  готово, file id {r.json().get('id')}")


if __name__ == "__main__":
    do_upload = "--upload" in sys.argv
    if not do_upload:
        print("Пробный прогон (добавь --upload, чтобы реально залить)\n")
    modrinth(do_upload)
    curseforge(do_upload)
