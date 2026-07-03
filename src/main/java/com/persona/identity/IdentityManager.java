package com.persona.identity;

import com.persona.cape.CapeRegistry;
import com.persona.config.PersonaConfig;
import com.persona.skin.SkinResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.AssetInfo;

/**
 * Runtime source of truth for the local identity overrides. All overrides only affect
 * what the local client renders.
 */
public final class IdentityManager {
    private static final IdentityManager INSTANCE = new IdentityManager();

    private PersonaConfig config = new PersonaConfig();
    private volatile SkinTextures resolvedSkin = null;
    private volatile String resolvedFor = null;

    private IdentityManager() {}

    public static IdentityManager get() {
        return INSTANCE;
    }

    public void init() {
        this.config = PersonaConfig.load();
        refreshSkin();
    }

    public PersonaConfig config() {
        return config;
    }

    /** Persist current config and re-resolve the skin if the source changed. */
    public void applyAndSave() {
        config.save();
        refreshSkin();
    }

    /** Re-resolve the skin (e.g. when joining a world, once the skin provider exists). */
    public void reapplySkin() {
        refreshSkin();
    }

    private void refreshSkin() {
        String source = config.skinSource == null ? "" : config.skinSource.trim();
        if (!config.applySkin || source.isEmpty()) {
            resolvedSkin = null;
            resolvedFor = null;
            return;
        }
        if (source.equals(resolvedFor) && resolvedSkin != null) {
            return; // already resolved
        }
        resolvedFor = source;
        SkinResolver.resolve(source).thenAccept(opt -> opt.ifPresent(skin -> {
            // Guard against a race where the source changed while we were fetching.
            if (source.equals(config.skinSource == null ? "" : config.skinSource.trim())) {
                resolvedSkin = skin;
            }
        }));
    }

    // ---- Identity helpers -------------------------------------------------

    private String pseudoOrNull() {
        String p = config.pseudo;
        return (p == null || p.isBlank()) ? null : p;
    }

    public boolean isLocalPlayer(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc != null && mc.player != null && entity != null
                && entity.getUuid().equals(mc.player.getUuid());
    }

    private String realUsername() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return (mc != null && mc.getSession() != null) ? mc.getSession().getUsername() : null;
    }

    /** Nametag / TAB replacement (returns null to keep the vanilla name). */
    public Text overrideNameForLocal(boolean forTab) {
        String p = pseudoOrNull();
        if (p == null) {
            return null;
        }
        boolean enabled = forTab ? config.applyTab : config.applyNametag;
        return enabled ? Text.literal(p) : null;
    }

    /** Best-effort chat rewrite: swap the real IGN for the pseudo in the message text. */
    public Text rewriteChat(Text original) {
        String p = pseudoOrNull();
        String real = realUsername();
        if (!config.applyChat || p == null || real == null || real.isBlank() || original == null) {
            return original;
        }
        String raw = original.getString();
        if (!raw.contains(real)) {
            return original;
        }
        // Note: rebuilding as literal drops styling, acceptable best effort behaviour.
        return Text.literal(raw.replace(real, p));
    }

    /** Combine the resolved skin (if any) with the selected cape for the local player. */
    public SkinTextures applyTo(SkinTextures original) {
        SkinTextures base = (config.applySkin && resolvedSkin != null) ? resolvedSkin : original;

        AssetInfo.TextureAsset cape = base.cape();
        if (config.applyCape && !CapeRegistry.NONE_ID.equals(config.capeId)) {
            AssetInfo.TextureAsset override = CapeRegistry.byId(config.capeId).toTextureAsset();
            if (override != null) {
                cape = override;
            }
        }

        if (base == original && cape == original.cape()) {
            return original; // nothing to change
        }
        return new SkinTextures(base.body(), cape, base.elytra(), base.model(), base.secure());
    }
}
