**A Steam-style in-game overlay for Minecraft with a real web browser.**
Press **Shift + Tab** (or any combo you like) and a sleek overlay slides in over the game:
browser tabs, a clock, session stats and quick links. Watch a guide on YouTube, look something up
on the wiki, or keep music playing while you build.

## Features

- **Real Chromium browser** (via [MCEF Modern](https://modrinth.com/mod/mcef-modern)): YouTube, Twitch, wikis,
  anything. Up to 8 tabs, an address bar that also searches, back/forward/reload/home.
- **Tabs keep running when the overlay is closed**, so music and videos keep playing in the background.
- **Multi-key bind**: the default is `Left Shift + Tab`. Record any combo of up to **3 buttons**, and you can mix keyboard
  and mouse (e.g. `Ctrl + Mouse 4`, `Mouse 4 + Mouse 5`).
- **Side panel**: big clock and date, world/server, session time, FPS, coordinates, dimension, health.
- **Quick links dock**: YouTube, Google, Minecraft Wiki, Twitch, Modrinth and GitHub (editable in the config).
- **Browser hotkeys**: `Ctrl+T` new tab, `Ctrl+W` close tab, `Ctrl+L` address bar, `Ctrl+Tab` next tab, `F5` reload,
  middle-click closes a tab, `Esc` closes the overlay.
- **Typing in web pages works properly**: Backspace, Enter, arrows, Delete, Ctrl+A/C/V and so on (Cube Overlay fixes the
  special-key input that MCEF Modern on its own does not deliver on Windows).
- **Its own UI scale** that doesn't depend on Minecraft's GUI scale (Auto / 1× to 4×).
- Doesn't pause the game by default, just like Steam. You can turn pausing on in the settings.

## Requirements

- Fabric Loader 0.18.4+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [MCEF Modern](https://modrinth.com/mod/mcef-modern) for the same Minecraft version

The first launch downloads Chromium (~150 MB) once, in the background. The overlay shows the progress.

Client-side only. Works in singleplayer and on any server.

## Settings

Click ⚙ in the top-right corner of the overlay to change the bind, home page, pause, UI size and
background dim. Everything is stored in `config/cube-overlay.json` (quick links too).

## Credits

- Browser: [MCEF Modern](https://github.com/DimasKama/mcef-modern) by DimasKama (LGPL-2.1), based on java-cef / CEF.
- Built with the help of AI (Claude by Anthropic).

---

## 🇷🇺 По-русски

Оверлей как в Steam: жмёшь **Shift + Tab**, и поверх игры появляется браузер с вкладками, часы, статистика и
быстрые ссылки. Вкладки продолжают работать, когда оверлей закрыт, так что музыка не прерывается.
Бинд можно поставить на любую комбинацию из 1–3 кнопок клавиатуры и мыши. Нужны Fabric API и MCEF Modern.
