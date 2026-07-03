package com.persona.cape;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue of official Minecraft Java capes.
 *
 * Each cape maps to a bundled texture at assets/persona/textures/capes/(id).png.
 * A cape is available only if its PNG is present, otherwise it is shown greyed out
 * as "unavailable". Add more textures with tools/fetch_capes.py.
 */
public final class CapeRegistry {
    public static final String NONE_ID = "none";

    public record Cape(String id, String displayName) {
        /** Full resource location of the bundled PNG (used to check availability). */
        public Identifier textureId() {
            return Identifier.of("persona", "textures/capes/" + id + ".png");
        }

        public boolean isAvailable() {
            if (NONE_ID.equals(id)) {
                return true;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.getResourceManager() == null) {
                return false;
            }
            return mc.getResourceManager().getResource(textureId()).isPresent();
        }

        /**
         * Vanilla texture asset for this cape, or null for "none"/missing.
         * TextureAssetInfo(id) derives texturePath = "textures/" + id.path + ".png",
         * so the id path is the bare "capes/(id)" to resolve to the bundled PNG.
         */
        public AssetInfo.TextureAsset toTextureAsset() {
            if (NONE_ID.equals(id) || !isAvailable()) {
                return null;
            }
            return new AssetInfo.TextureAssetInfo(Identifier.of("persona", "capes/" + id));
        }
    }

    private static final List<Cape> CAPES = new ArrayList<>();

    private static void add(String id, String name) {
        CAPES.add(new Cape(id, name));
    }

    static {
        add(NONE_ID, "None");

        add("pan", "Pan");
        add("migrator", "Migrator");
        add("vanilla", "Vanilla");

        add("minecon_2011", "MINECON 2011");
        add("minecon_2012", "MINECON 2012");
        add("minecon_2013", "MINECON 2013");
        add("minecon_2015", "MINECON 2015");
        add("minecon_2016", "MINECON 2016");
        add("minecraft_experience", "Minecraft Experience");
        add("moonlight_trail", "Moonlight Trail");
        add("crafter", "Crafter");
        add("founders", "Founder's");
        add("cherry_blossom", "Cherry Blossom");
        add("followers", "Follower's");
        add("purple_heart", "Purple Heart");
        add("anniversary_15th", "15th Anniversary");
        add("mcc_15th_year", "MCC 15th Year");
        add("mojang_office", "Mojang Office");
        add("home", "Home");
        add("menace", "Menace");
        add("copper", "Copper");
        add("zombie_horse", "Zombie Horse");
        add("builder", "Builder");

        add("classic_mojang", "Classic Mojang");
        add("mojang", "Mojang");
        add("mojang_studios", "Mojang Studios");

        add("bacon", "Bacon");
        add("millionth_customer", "Millionth Customer");
        add("db", "dB");
        add("snowman", "Snowman");
        add("cheapshot", "Cheapsh0t's");
        add("spade", "Spade");
        add("prismarine", "Prismarine");
        add("turtle", "Turtle");
        add("birthday", "Birthday");
        add("valentine", "Valentine");
        add("oxeye", "Oxeye");
        add("blueprint", "Blueprint");

        add("scrolls_champion", "Scrolls Champion");
        add("cobalt", "Cobalt");

        add("translator", "Translator");
        add("chinese_translator", "Chinese Translator");
        add("moderator", "Moderator");
        add("realms_mapmaker", "Realms MapMaker");
    }

    private CapeRegistry() {}

    public static List<Cape> all() {
        return CAPES;
    }

    public static Cape byId(String id) {
        for (Cape c : CAPES) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return CAPES.get(0);
    }
}
