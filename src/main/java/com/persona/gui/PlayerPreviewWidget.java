package com.persona.gui;

import com.persona.PersonaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Live 3D preview of the local player with the current Persona overrides applied
 * (pseudo/skin/cape). Drag with the mouse to spin (full 360°) and tilt. The
 * overrides show up because we render {@code mc.player}, whose {@code getSkin} is
 * mixin-overridden for the local player.
 */
public class PlayerPreviewWidget extends ClickableWidget {
    private float yawDeg = 200f;   // start slightly turned so it reads as 3D
    private float pitchRad = 0f;

    public PlayerPreviewWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal("Preview"));
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x1 = getX();
        int y1 = getY();
        int x2 = getX() + width;
        int y2 = getY() + height;

        // Framed panel with a soft vertical gradient.
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xFF3A3355);
        ctx.fillGradient(x1, y1, x2, y2, 0xFF241F38, 0xFF120F1E);

        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractClientPlayerEntity player = mc.player;
        if (player == null) {
            String msg = "Preview available in game";
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(msg),
                    (x1 + x2) / 2, (y1 + y2) / 2 - 4, 0xFF9A93B8);
            return;
        }

        try {
            renderPlayer(ctx, mc, player, x1, y1, x2, y2);
        } catch (Throwable t) {
            PersonaClient.LOGGER.warn("[Persona] preview render failed: {}", t.toString());
            ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal("Preview unavailable"),
                    (x1 + x2) / 2, (y1 + y2) / 2 - 4, 0xFFCC6666);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderPlayer(DrawContext ctx, MinecraftClient mc, AbstractClientPlayerEntity player,
                              int x1, int y1, int x2, int y2) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);

        EntityRenderer renderer = mc.getEntityRenderDispatcher().getRenderer(player);
        EntityRenderState state = (EntityRenderState) renderer.getAndUpdateRenderState(player, tickDelta);

        // Mirror vanilla InventoryScreen.drawEntity: normalize scale then translate up
        // by half the model height so it centers in the frame. Rotation is our own
        // (bodyYaw) so we get a full 360° spin.
        float translY = 0f;
        if (state instanceof LivingEntityRenderState living) {
            living.bodyYaw = yawDeg;
            living.relativeHeadYaw = 0f;
            living.pitch = 0f;
            living.width = living.width / living.baseScale;
            living.height = living.height / living.baseScale;
            living.baseScale = 1.0f;
            translY = living.height / 2.0f;
        }

        int boxH = y2 - y1;
        float scale = boxH * 0.42f;                       // fit the model inside the frame (~vanilla ratio)
        Vector3f translation = new Vector3f(0f, translY, 0f);
        Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI); // flip upright for GUI space
        Quaternionf cameraAngle = new Quaternionf().rotateX(pitchRad);      // vertical tilt

        ctx.addEntity(state, scale, translation, rotation, cameraAngle, x1, y1, x2, y2);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        yawDeg += (float) deltaX * 2.2f;
        pitchRad += (float) deltaY * 0.02f;
        pitchRad = Math.max(-0.7f, Math.min(0.7f, pitchRad));
        return true;
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        // no-op; rotation handled by dragging
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
