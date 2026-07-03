package com.persona.mixin.flashback;

import com.persona.flashback.FlashbackCapeIntegration;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.moulberry.flashback.editor.ui.windows.SelectedEntityPopup")
public class SelectedEntityPopupCapeMixin {

    @ModifyVariable(method = "render", at = @At("HEAD"), index = 0, argsOnly = true)
    private static Entity persona$captureEntity(Entity entity) {
        FlashbackCapeIntegration.setLastRightClickedEntity(entity);
        return entity;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private static void persona$addCapeSelector(CallbackInfo ci) {
        FlashbackCapeIntegration.renderCapeSelector();
    }
}
