package com.persona;

import com.persona.gui.PersonaScreen;
import com.persona.identity.IdentityManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persona client side "local identity" mod.
 *
 * Everything this mod does only affects the local render on your own client. Other
 * players on a server never see your Persona name, skin or cape (the server is
 * authoritative for that data). This is a local illusion by design.
 */
public class PersonaClient implements ClientModInitializer {
    public static final String MOD_ID = "persona";
    public static final Logger LOGGER = LoggerFactory.getLogger("Persona");

    /** Default open key is Right Shift, remappable via Options then Controls. */
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        IdentityManager.get().init();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.persona.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        // The skin provider only exists once in a world, so re-resolve on join to
        // re-apply a persisted skin that could not resolve during early startup.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                IdentityManager.get().reapplySkin());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new PersonaScreen());
                }
            }
        });

        LOGGER.info("[Persona] initialised. Press Right Shift to open.");
    }
}
