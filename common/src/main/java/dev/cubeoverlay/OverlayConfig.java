package dev.cubeoverlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** config/cube-overlay.json */
public final class OverlayConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cube-overlay.json");

    public List<String> combo = new ArrayList<>(KeyCombo.defaultCombo().toNames());
    public String homePage = "https://www.google.com/?hl=ru";
    /** Ставить ли одиночную игру на паузу, пока открыт оверлей. */
    public boolean pauseGame = false;
    /** Прозрачность затемнения фона, 0–255. */
    public int backgroundAlpha = 150;
    public boolean showSidebar = true;
    /** Размер интерфейса оверлея: 0 — авто (по высоте окна), 1–4 — пикселей экрана на единицу. */
    public int uiScale = 0;
    public List<QuickLink> quickLinks = new ArrayList<>(List.of(
            new QuickLink("YouTube", "https://www.youtube.com"),
            new QuickLink("Google", "https://www.google.com/?hl=ru"),
            new QuickLink("Minecraft Wiki", "https://minecraft.wiki"),
            new QuickLink("Twitch", "https://www.twitch.tv"),
            new QuickLink("Modrinth", "https://modrinth.com/mods"),
            new QuickLink("GitHub", "https://github.com")
    ));

    public record QuickLink(String name, String url) {
    }

    private transient KeyCombo cachedCombo;

    public KeyCombo combo() {
        if (cachedCombo == null) {
            cachedCombo = KeyCombo.parse(combo);
            if (cachedCombo.isEmpty()) {
                cachedCombo = KeyCombo.defaultCombo();
            }
        }
        return cachedCombo;
    }

    public void setCombo(KeyCombo newCombo) {
        combo = new ArrayList<>(newCombo.toNames());
        cachedCombo = null;
    }

    public static OverlayConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                OverlayConfig config = GSON.fromJson(reader, OverlayConfig.class);
                if (config != null) {
                    config.fixup();
                    return config;
                }
            } catch (Exception e) {
                CubeOverlay.LOGGER.warn("Не удалось прочитать {}, беру настройки по умолчанию", FILE, e);
            }
        }
        OverlayConfig config = new OverlayConfig();
        config.save();
        return config;
    }

    private void fixup() {
        if (combo == null) combo = new ArrayList<>(KeyCombo.defaultCombo().toNames());
        if (homePage == null || homePage.isBlank()) homePage = "https://www.google.com";
        if (quickLinks == null) quickLinks = new ArrayList<>();
        backgroundAlpha = Math.clamp(backgroundAlpha, 0, 255);
        uiScale = Math.clamp(uiScale, 0, 4);
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            // пишем во временный файл и подменяем, чтобы не оставить обрезанный json
            Path tmp = FILE.resolveSibling(FILE.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(this, writer);
            }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            CubeOverlay.LOGGER.error("Не удалось сохранить {}", FILE, e);
        }
    }
}
