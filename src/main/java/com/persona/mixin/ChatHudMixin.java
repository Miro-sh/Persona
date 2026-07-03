package com.persona.mixin;

import com.persona.identity.IdentityManager;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Best-effort chat rewrite: swaps the real IGN for the Persona pseudo in incoming
 * messages. Rebuilds as a literal so styling is dropped, heuristic by design.
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private Text persona$rewriteChat(Text message) {
        return IdentityManager.get().rewriteChat(message);
    }
}
