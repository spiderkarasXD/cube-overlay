package dev.cubeoverlay;

import net.minecraft.client.resources.language.I18n;

/** Перевод строк интерфейса из assets/cubeoverlay/lang/*.json — язык берётся из настроек игры. */
public final class Tr {

    private Tr() {
    }

    public static String t(String key, Object... args) {
        return I18n.get("cubeoverlay." + key, args);
    }
}
