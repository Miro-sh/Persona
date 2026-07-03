package com.persona.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/**
 * A flat, theme-matching button (dark panel + purple accent) drawn manually via
 * {@link DrawContext}, replacing the vanilla button texture so the UI stays on-theme.
 */
public class PersonaButton extends ClickableWidget {
    public enum Style { NORMAL, PRIMARY, TAB }

    private final Runnable onPress;
    private Style style = Style.NORMAL;
    private boolean selected = false;

    public PersonaButton(int x, int y, int width, int height, Text label, Runnable onPress) {
        super(x, y, width, height, label);
        this.onPress = onPress;
    }

    public PersonaButton style(Style s) {
        this.style = s;
        return this;
    }

    public PersonaButton selected(boolean s) {
        this.selected = s;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x1 = getX();
        int y1 = getY();
        int x2 = getX() + width;
        int y2 = getY() + height;
        boolean hover = isHovered() && this.active;
        boolean accent = style == Style.PRIMARY || (style == Style.TAB && selected);

        int bgTop, bgBot, border, text, highlight;
        if (!this.active) {
            bgTop = 0xFF262233; bgBot = 0xFF201C2C; border = 0xFF35304A; text = 0xFF6E688A; highlight = 0x11FFFFFF;
        } else if (accent) {
            bgTop = hover ? 0xFF9C8DFF : 0xFF7E6FEA;
            bgBot = hover ? 0xFF7A69E0 : 0xFF5F51C6;
            border = hover ? 0xFFC9BEFF : 0xFFA99CFF;
            text = 0xFFFFFFFF; highlight = 0x33FFFFFF;
        } else {
            bgTop = hover ? 0xFF383056 : 0xFF2A2442;
            bgBot = hover ? 0xFF2A2444 : 0xFF201B33;
            border = hover ? 0xFF8B7CF6 : 0xFF453D68;
            text = hover ? 0xFFFFFFFF : 0xFFCBC6E0;
            highlight = 0x22FFFFFF;
        }

        // border, body, top highlight
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, border);
        ctx.fillGradient(x1, y1, x2, y2, bgTop, bgBot);
        ctx.fill(x1, y1, x2, y1 + 1, highlight);

        // selected-tab accent bar at the bottom
        if (style == Style.TAB && selected) {
            ctx.fill(x1, y2 - 2, x2, y2, 0xFFD8CCFF);
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        int ty = y1 + (height - mc.textRenderer.fontHeight) / 2 + 1;
        ctx.drawCenteredTextWithShadow(mc.textRenderer, getMessage(), (x1 + x2) / 2, ty, text);
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        if (this.active) {
            onPress.run();
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
