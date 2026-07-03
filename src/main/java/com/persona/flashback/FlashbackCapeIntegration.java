package com.persona.flashback;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.persona.cape.CapeRegistry;
import com.persona.skin.SkinResolver;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persona ⇄ Flashback bridge. Adds, to Flashback's right-click entity popup, a Persona
 * section that can:
 * <ul>
 *     <li>override the entity's cape with any registered Persona cape, and</li>
 *     <li>override the entity's whole skin + cape from a player UUID (or name).</li>
 * </ul>
 *
 * <p>Both overrides are keyed by the target entity's UUID, persisted to disk, and
 * re-applied every frame through the {@code getSkin()} wrapper mixin. They are applied on
 * top of whatever skin Flashback itself resolved (vanilla, "skin from UUID", or "skin from
 * file"), so a Persona cape survives even a Flashback file-skin.</p>
 */
public final class FlashbackCapeIntegration {
    // entity UUID -> Persona cape id
    private static final Map<UUID, String> capeOverrides = new HashMap<>();
    // entity UUID -> skin source (a player UUID or name) typed by the user
    private static final Map<UUID, String> skinSources = new HashMap<>();
    // entity UUID -> resolved skin+cape (async result cache, read from the render thread)
    private static final Map<UUID, SkinTextures> resolvedSkins = new ConcurrentHashMap<>();

    private static final Path CAPE_PATH = configFile("flashback_capes.json");
    private static final Path SKIN_PATH = configFile("flashback_skins.json");
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, String>>(){}.getType();

    private static final Logger LOGGER = LoggerFactory.getLogger("Persona/Flashback");

    private static Object lastRightClickedEntity;
    // Reused Flashback ImString backing the UUID text field (imgui.moulberry90.type.ImString).
    private static Object skinInputBuffer;

    private FlashbackCapeIntegration() {}

    private static Path configFile(String name) {
        return FabricLoader.getInstance().getConfigDir().resolve("persona").resolve(name);
    }

    public static void setLastRightClickedEntity(Object entity) {
        lastRightClickedEntity = entity;
    }

    public static void init() {
        load(CAPE_PATH, capeOverrides);
        load(SKIN_PATH, skinSources);
        // Kick off resolution for any persisted skin sources.
        skinSources.forEach(FlashbackCapeIntegration::resolveSkin);
    }

    // ---- Cape overrides ---------------------------------------------------

    public static String getCapeId(UUID uuid) {
        return capeOverrides.get(uuid);
    }

    public static void setCapeId(UUID uuid, String capeId) {
        if (capeId == null || CapeRegistry.NONE_ID.equals(capeId)) {
            capeOverrides.remove(uuid);
        } else {
            capeOverrides.put(uuid, capeId);
        }
        save(CAPE_PATH, capeOverrides);
    }

    // ---- Skin overrides ---------------------------------------------------

    public static void setSkinSource(UUID entity, String source) {
        if (source == null || source.isBlank()) {
            skinSources.remove(entity);
            resolvedSkins.remove(entity);
        } else {
            String trimmed = source.trim();
            skinSources.put(entity, trimmed);
            resolveSkin(entity, trimmed);
        }
        save(SKIN_PATH, skinSources);
    }

    private static void resolveSkin(UUID entity, String source) {
        SkinResolver.resolve(source).thenAccept(opt -> opt.ifPresentOrElse(
                skin -> resolvedSkins.put(entity, skin),
                () -> LOGGER.warn("Could not resolve skin for '{}' (entity {})", source, entity)));
    }

    // ---- Applied every frame from the getSkin() wrapper -------------------

    /**
     * Layer the Persona skin override (if any) and cape override (if any) on top of the
     * skin Flashback produced for this entity. Called from the {@code getSkin()}
     * {@code @WrapMethod} mixin, so it runs no matter which skin source Flashback used.
     */
    public static SkinTextures applyOverrides(AbstractClientPlayerEntity player, SkinTextures original) {
        UUID uuid = player.getUuid();

        // 1) Skin (+ its own cape) override from a UUID / name.
        SkinTextures base = resolvedSkins.getOrDefault(uuid, original);

        // 2) Cape override from a Persona cape sits on top of whatever cape `base` has.
        AssetInfo.TextureAsset cape = base.cape();
        String capeId = capeOverrides.get(uuid);
        if (capeId != null) {
            CapeRegistry.Cape personaCape = CapeRegistry.byId(capeId);
            if (personaCape.isAvailable()) {
                AssetInfo.TextureAsset override = personaCape.toTextureAsset();
                if (override != null) {
                    cape = override;
                }
            }
        }

        if (base == original && cape == original.cape()) {
            return original; // nothing to change
        }
        return new SkinTextures(base.body(), cape, base.elytra(), base.model(), base.secure());
    }

    // ---- ImGui rendering inside Flashback's popup -------------------------

    public static void renderCapeSelector() {
        Object entityObj = lastRightClickedEntity;
        if (entityObj == null) return;
        try {
            UUID uuid = (UUID) entityObj.getClass().getMethod("getUuid").invoke(entityObj);

            Class<?> imGui = Class.forName("imgui.moulberry90.ImGui");
            Method text = imGui.getMethod("text", String.class);
            Method separator = imGui.getMethod("separator");
            Method beginCombo = imGui.getMethod("beginCombo", String.class, String.class);
            Method endCombo = imGui.getMethod("endCombo");
            Method selectable = imGui.getMethod("selectable", String.class, boolean.class);
            Method setItemDefaultFocus = imGui.getMethod("setItemDefaultFocus");

            separator.invoke(null);
            text.invoke(null, "Persona Cape:");

            String currentId = capeOverrides.getOrDefault(uuid, CapeRegistry.NONE_ID);
            CapeRegistry.Cape currentCape = CapeRegistry.byId(currentId);

            if ((boolean) beginCombo.invoke(null, "##personaCape", currentCape.displayName())) {
                for (CapeRegistry.Cape cape : CapeRegistry.all()) {
                    boolean isSelected = cape.id().equals(currentId);
                    boolean available = CapeRegistry.NONE_ID.equals(cape.id()) || cape.isAvailable();

                    if (!available) continue;

                    String label = cape.displayName();
                    if ((boolean) selectable.invoke(null, label, isSelected)) {
                        setCapeId(uuid, cape.id());
                    }
                    if (isSelected) {
                        setItemDefaultFocus.invoke(null);
                    }
                }
                endCombo.invoke(null);
            }

            renderSkinFromUuid(uuid, imGui, text, separator);

        } catch (Exception e) {
            LOGGER.error("Failed to render Persona popup section", e);
        }
    }

    /**
     * A robust "skin &amp; cape from UUID" control. Unlike Flashback's native one — which
     * calls {@code UUID.fromString} on the field every frame and hides its button whenever
     * the text is not an already-dashed UUID — this accepts a dashed UUID, an undashed
     * UUID, or a player name, and only parses on click via {@link SkinResolver}.
     */
    private static void renderSkinFromUuid(UUID uuid, Class<?> imGui, Method text, Method separator) throws Exception {
        Class<?> imGuiHelper = Class.forName("com.moulberry.flashback.editor.ui.ImGuiHelper");
        Class<?> imString = Class.forName("imgui.moulberry90.type.ImString");
        Method createImString = imGuiHelper.getMethod("createResizableImString", String.class);
        Method getString = imGuiHelper.getMethod("getString", imString);
        Method inputTextWithHint = imGui.getMethod("inputTextWithHint", String.class, String.class, imString);
        Method button = imGui.getMethod("button", String.class);

        if (skinInputBuffer == null) {
            skinInputBuffer = createImString.invoke(null, "");
        }

        separator.invoke(null);
        text.invoke(null, "Persona Skin & Cape (UUID or name):");

        inputTextWithHint.invoke(null, "##personaSkinUuid",
                "e.g. 853c80ef-3c37-49fd-aa49-938b674adae6", skinInputBuffer);

        if ((boolean) button.invoke(null, "Apply skin & cape")) {
            String source = ((String) getString.invoke(null, skinInputBuffer)).trim();
            if (!source.isEmpty()) {
                setSkinSource(uuid, source);
            }
        }

        String current = skinSources.get(uuid);
        if (current != null) {
            boolean resolved = resolvedSkins.containsKey(uuid);
            text.invoke(null, (resolved ? "Active: " : "Loading: ") + current);
            if ((boolean) button.invoke(null, "Reset Persona skin")) {
                setSkinSource(uuid, null);
            }
        }
    }

    // ---- Persistence ------------------------------------------------------

    private static void load(Path path, Map<UUID, String> into) {
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path);
                Map<UUID, String> loaded = GSON.fromJson(json, MAP_TYPE);
                if (loaded != null) {
                    into.clear();
                    into.putAll(loaded);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    private static void save(Path path, Map<UUID, String> from) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(from, MAP_TYPE);
            Files.writeString(path, json);
        } catch (Exception e) {
            // ignore
        }
    }
}
