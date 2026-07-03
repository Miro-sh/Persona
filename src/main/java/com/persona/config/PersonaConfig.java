package com.persona.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.persona.PersonaClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Persisted Persona settings, serialized to {@code config/persona.json}.
 */
public class PersonaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Displayed pseudo (empty = keep real name).
    public String pseudo = "";
    // Username whose skin is copied locally (empty = keep real skin).
    public String skinSource = "";
    // Selected cape id from CapeRegistry, or "none".
    public String capeId = "none";

    // Per-feature toggles.
    public boolean applyNametag = true;
    public boolean applyTab = true;
    public boolean applyChat = true;
    public boolean applySkin = true;
    public boolean applyCape = true;

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("persona.json");
    }

    public static PersonaConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                PersonaConfig cfg = GSON.fromJson(reader, PersonaConfig.class);
                if (cfg != null) {
                    return cfg;
                }
            } catch (Exception e) {
                PersonaClient.LOGGER.error("[Persona] Failed to read config, using defaults", e);
            }
        }
        PersonaConfig cfg = new PersonaConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PersonaClient.LOGGER.error("[Persona] Failed to write config", e);
        }
    }
}
