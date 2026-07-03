package com.persona.skin;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.persona.PersonaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.SkinTextures;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves another player's skin from the Mojang API and hands it to the vanilla
 * skin provider (which downloads the texture and detects slim/classic automatically).
 *
 * <p>All network work is async; the returned future completes with an empty optional
 * on any failure (unknown username, offline, rate-limited, ...).</p>
 */
public final class SkinResolver {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String PROFILE_BY_NAME = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private SkinResolver() {}

    /**
     * Resolve a skin (and the player's own cape) from either a username or a raw UUID.
     *
     * <p>A UUID may be dashed ({@code 069a79f4-44e9-4726-a5be-fca90e38aaf5}) or undashed
     * (32 hex chars). When a UUID is given the username lookup is skipped and the
     * sessionserver profile — which carries both the SKIN and CAPE textures — is fetched
     * directly.</p>
     */
    public static CompletableFuture<Optional<SkinTextures>> resolve(String source) {
        if (source == null || source.isBlank()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        String trimmed = source.trim();
        return CompletableFuture
                .supplyAsync(() -> {
                    UUID uuid = tryParseUuid(trimmed);
                    return uuid != null ? fetchProfileByUuid(uuid) : fetchProfileByName(trimmed);
                })
                .thenCompose(profileOpt -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    // Skin provider only exists once the client is fully constructed;
                    // during very early startup it is still null.
                    if (profileOpt.isEmpty() || mc.getSkinProvider() == null) {
                        return CompletableFuture.completedFuture(Optional.<SkinTextures>empty());
                    }
                    return mc.getSkinProvider().fetchSkinTextures(profileOpt.get());
                })
                .exceptionally(t -> {
                    PersonaClient.LOGGER.warn("[Persona] Skin resolution failed for '{}': {}", trimmed, t.toString());
                    return Optional.empty();
                });
    }

    /** username -> UUID -> GameProfile with the "textures" property populated. */
    private static Optional<GameProfile> fetchProfileByName(String username) {
        try {
            String idJson = get(PROFILE_BY_NAME + username);
            if (idJson == null) {
                return Optional.empty();
            }
            JsonObject idObj = JsonParser.parseString(idJson).getAsJsonObject();
            String undashed = idObj.get("id").getAsString();
            String name = idObj.has("name") ? idObj.get("name").getAsString() : username;
            return fetchProfile(dashUuid(undashed), undashed, name);
        } catch (Exception e) {
            PersonaClient.LOGGER.warn("[Persona] Could not fetch profile for '{}': {}", username, e.toString());
            return Optional.empty();
        }
    }

    /** UUID -> GameProfile with the "textures" property populated (skin + cape). */
    private static Optional<GameProfile> fetchProfileByUuid(UUID uuid) {
        String undashed = uuid.toString().replace("-", "");
        try {
            return fetchProfile(uuid, undashed, undashed);
        } catch (Exception e) {
            PersonaClient.LOGGER.warn("[Persona] Could not fetch profile for UUID '{}': {}", uuid, e.toString());
            return Optional.empty();
        }
    }

    /**
     * Fetch the signed sessionserver profile and turn it into a {@link GameProfile}.
     * {@code fallbackName} is used when the profile response omits a name.
     */
    private static Optional<GameProfile> fetchProfile(UUID uuid, String undashed, String fallbackName) throws Exception {
        String profJson = get(SESSION_PROFILE + undashed + "?unsigned=false");
        if (profJson == null) {
            return Optional.empty();
        }
        JsonObject profObj = JsonParser.parseString(profJson).getAsJsonObject();
        String name = profObj.has("name") ? profObj.get("name").getAsString() : fallbackName;

        // GameProfile(uuid, name) uses the shared immutable PropertyMap.EMPTY, so we
        // must build a mutable map and pass it via the 3-arg constructor.
        Multimap<String, Property> mm = LinkedHashMultimap.create();
        if (profObj.has("properties")) {
            profObj.getAsJsonArray("properties").forEach(el -> {
                JsonObject p = el.getAsJsonObject();
                String pname = p.get("name").getAsString();
                String value = p.get("value").getAsString();
                String signature = p.has("signature") ? p.get("signature").getAsString() : null;
                mm.put(pname,
                        signature != null ? new Property(pname, value, signature) : new Property(pname, value));
            });
        }
        return Optional.of(new GameProfile(uuid, name, new PropertyMap(mm)));
    }

    /** Returns the parsed UUID if {@code s} is a dashed or undashed UUID, else null. */
    private static UUID tryParseUuid(String s) {
        try {
            if (s.length() == 36 && s.indexOf('-') == 8) {
                return UUID.fromString(s);
            }
            if (s.length() == 32 && s.matches("\\p{XDigit}{32}")) {
                return dashUuid(s);
            }
        } catch (IllegalArgumentException ignored) {
            // not a valid UUID; fall through to name resolution
        }
        return null;
    }

    private static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
            return resp.body();
        }
        return null; // 204 (no such user) or any error
    }

    private static UUID dashUuid(String undashed) {
        String dashed = undashed.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(dashed);
    }
}
