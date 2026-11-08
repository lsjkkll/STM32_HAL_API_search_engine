package com.stm32hal.api.desktop;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** 收藏与系列选择的本地持久化。 */
public final class FavStore {

    private final Path file = Paths.get(System.getProperty("user.home"), ".stm32hal-api", "prefs.properties");
    private final Properties prefs = new Properties();

    public FavStore() {
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                prefs.load(in);
            } catch (IOException ignored) {
                // 配置损坏时使用空配置，不影响使用
            }
        }
    }

    public Set<String> loadFavorites() {
        Set<String> out = new LinkedHashSet<>();
        String raw = prefs.getProperty("favorites", "");
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) {
                out.add(id);
            }
        }
        return out;
    }

    public String loadSeries() {
        return prefs.getProperty("series", Model.DEFAULT_SERIES_CODE);
    }

    public void save(Set<String> favorites, String seriesCode) {
        prefs.setProperty("favorites", String.join(",", favorites));
        prefs.setProperty("series", seriesCode);
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                prefs.store(out, "STM32 HAL 速查桌面版配置");
            }
        } catch (IOException ignored) {
            // 写入失败不影响当前会话
        }
    }
}
