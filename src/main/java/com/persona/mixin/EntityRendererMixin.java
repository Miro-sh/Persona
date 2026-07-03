package com.persona.mixin;

import com.persona.identity.IdentityManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Overrides the name shown on the nametag above the local player's head. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void persona$overrideName(Entity entity, CallbackInfoReturnable<Text> cir) {
        if (IdentityManager.get().isLocalPlayer(entity)) {
            Text override = IdentityManager.get().overrideNameForLocal(false);
            if (override != null) {
                cir.setReturnValue(override);
            }
        }
    }
}
