package dev.cubeoverlay;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Комбинация из 1–3 кнопок (клавиатура и/или мышь), например Shift + Tab или Ctrl + ПКМ.
 * Хранится как список имён в формате options.txt: "key.keyboard.left.shift", "key.mouse.right".
 */
public final class KeyCombo {

    public static final int MAX_KEYS = 3;

    private final List<InputConstants.Key> keys;

    public KeyCombo(List<InputConstants.Key> keys) {
        this.keys = List.copyOf(keys);
    }

    public static KeyCombo parse(List<String> names) {
        List<InputConstants.Key> keys = new ArrayList<>();
        for (String name : names) {
            try {
                InputConstants.Key key = InputConstants.getKey(name);
                if (key != InputConstants.UNKNOWN && !keys.contains(key)) {
                    keys.add(key);
                }
            } catch (IllegalArgumentException ignored) {
                // кривая строка в конфиге — просто пропускаем
            }
        }
        return new KeyCombo(keys);
    }

    public static KeyCombo defaultCombo() {
        return parse(List.of("key.keyboard.left.shift", "key.keyboard.tab"));
    }

    public List<InputConstants.Key> keys() {
        return keys;
    }

    public boolean isEmpty() {
        return keys.isEmpty();
    }

    public List<String> toNames() {
        return keys.stream().map(InputConstants.Key::getName).toList();
    }

    /** Зажаты ли все кнопки комбинации (pressed — то, что сейчас держит игрок). */
    public boolean isHeld(Set<InputConstants.Key> pressed) {
        return !keys.isEmpty() && pressed.containsAll(keys);
    }

    public boolean contains(InputConstants.Key key) {
        return keys.contains(key);
    }

    /** "Left Shift + Tab" — подписи берутся из языка игры. */
    public String displayName() {
        if (keys.isEmpty()) {
            return "—";
        }
        StringBuilder sb = new StringBuilder();
        for (InputConstants.Key key : keys) {
            if (!sb.isEmpty()) {
                sb.append(" + ");
            }
            sb.append(key.getDisplayName().getString());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KeyCombo other && keys.equals(other.keys);
    }

    @Override
    public int hashCode() {
        return keys.hashCode();
    }
}
