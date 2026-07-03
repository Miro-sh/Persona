package com.persona.mixin;

import com.persona.identity.IdentityManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides the local player's name in the TAB player list. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void persona$overrideTabName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && entry != null && entry.getProfile() != null
                && entry.getProfile().id().equals(mc.player.getUuid())) {
            Text override = IdentityManager.get().overrideNameForLocal(true);
            if (override != null) {
                cir.setReturnValue(override);
            }
        }
    }
}
