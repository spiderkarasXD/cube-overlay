# Что заполнить при публикации

## Modrinth: modrinth.com → Create a project

| Поле | Значение |
|---|---|
| Name | `Cube Overlay` |
| URL | `cube-overlay` |
| Visibility | Public |
| Summary | `Steam-style in-game overlay with a real Chromium browser. Open it with any key combo (Shift+Tab by default): tabs, YouTube/Twitch, clock, FPS and coordinates. Tabs keep playing music when the overlay is closed.` |

Дальше в настройках проекта:

| Раздел | Значение |
|---|---|
| General → Icon | `publish/icon.png` |
| General → Environment | Client: **Required**, Server: **Unsupported** |
| Description | весь `publish/description.md` |
| Tags → Categories | `utility`, `social`, `technology` (featured: `utility`) |
| License | MIT (`MIT`) |
| Links | Source / Issues / Discord, если есть, иначе пусто |
| Gallery | `publish/gallery/overlay.png`, Title `Overlay`, отметить Featured |

Версии заливает `tools/publish.py`. Вручную (Versions → Create version) заполняется так:

| Поле | 1.21.11 | 26.1.x | 26.2 | 26.3 |
|---|---|---|---|---|
| File | `cube-overlay-1.0.0+mc1.21.11.jar` | `cube-overlay-1.0.0+mc26.1.2.jar` | `cube-overlay-1.0.0+mc26.2.jar` | `cube-overlay-1.0.0+mc26.3.jar` |
| Version number | `1.0.0+mc1.21.11` | `1.0.0+mc26.1.2` | `1.0.0+mc26.2` | `1.0.0+mc26.3` |
| Name | `Cube Overlay 1.0.0 for 1.21.11` | `… for 26.1.2` | `… for 26.2` | `… for 26.3` |
| Game versions | 1.21.11 | 26.1, 26.1.1, 26.1.2 | 26.2 | 26.3 |
| Loaders | Fabric | Fabric | Fabric | Fabric |
| Release channel | Release | Release | Release | Release |
| Dependencies | Fabric API (required), MCEF Modern (required) | то же | то же | то же |
| Changelog | `publish/CHANGELOG.md` | то же | то же | то же |

Токен для скрипта: Settings → Personal access tokens → Create, права **Create versions** (+ Read projects).

## CurseForge: authors.curseforge.com → Create Project

| Поле | Значение |
|---|---|
| Game / Class | Minecraft / **Mods** |
| Project name | `Cube Overlay` |
| Summary | `Steam-style in-game overlay with a real Chromium browser, tabs and custom key combos (Shift+Tab by default).` |
| Description | `publish/description.md` (переключить редактор в Markdown) |
| Main category | Utility & QoL |
| Additional categories | Cosmetic, Map and Information (если есть в списке) |
| License | MIT License |
| Logo / Avatar | `publish/icon.png` |
| Images | `publish/gallery/overlay.png` |
| Source / Issues / Wiki / Discord | по желанию |

Upload File (для каждой версии):

| Поле | Значение |
|---|---|
| File | jar из `dist/` |
| Display name | `Cube Overlay 1.0.0 for <версия>` |
| Release type | Release |
| Game versions | версия Minecraft (как в таблице Modrinth) + **Fabric** + **Client** + **Java 21** (для 1.21.11) или **Java 25** (для 26.x) |
| Related projects | Fabric API: Required Dependency. MCEF Modern: Required Dependency, если он есть на CurseForge |
| Changelog | `publish/CHANGELOG.md`, тип Markdown |

Токен для скрипта: legacy.curseforge.com/account/api-tokens. Project ID показан справа на странице проекта.

## Залить все версии скриптом

```bash
set MODRINTH_TOKEN=...
set MODRINTH_PROJECT=cube-overlay
set CURSEFORGE_TOKEN=...
set CURSEFORGE_PROJECT=123456
python tools/publish.py           # проверить
python tools/publish.py --upload  # залить
```
