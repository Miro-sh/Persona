package com.persona.gui;

import com.persona.cape.CapeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lightweight scrollable + searchable list of capes, drawn manually via
 * {@link DrawContext} (stable API). Selection is tracked by cape id so it survives
 * filtering. Unavailable capes (no bundled texture) are greyed and not selectable.
 */
public class CapeListWidget extends ClickableWidget {
    private static final int ROW_HEIGHT = 12;
    private static final int PAD = 2;

    private final List<CapeRegistry.Cape> all = CapeRegistry.all();
    private List<CapeRegistry.Cape> filtered = new ArrayList<>(all);
    private String selectedId = CapeRegistry.NONE_ID;
    private int scrollPx = 0;

    public CapeListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Text.literal("Capes"));
    }

    /** Filter rows by a case-insensitive substring of the display name. */
    public void setFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            filtered = new ArrayList<>(all);
        } else {
            filtered = new ArrayList<>();
            for (CapeRegistry.Cape c : all) {
                if (c.displayName().toLowerCase(Locale.ROOT).contains(q)
                        || c.id().toLowerCase(Locale.ROOT).contains(q)) {
                    filtered.add(c);
                }
            }
        }
        scrollPx = Math.max(0, Math.min(scrollPx, maxScroll()));
        int sel = selectedIndexInFiltered();
        if (sel >= 0) {
            ensureVisible(sel);
        }
    }

    public void setSelectedById(String id) {
        selectedId = id;
        int sel = selectedIndexInFiltered();
        if (sel >= 0) {
            ensureVisible(sel);
        }
    }

    public String getSelectedId() {
        return selectedId;
    }

    private int selectedIndexInFiltered() {
        for (int i = 0; i < filtered.size(); i++) {
            if (filtered.get(i).id().equals(selectedId)) {
                return i;
            }
        }
        return -1;
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() * ROW_HEIGHT - (height - PAD * 2));
    }

    private void ensureVisible(int index) {
        int rowTop = index * ROW_HEIGHT;
        int viewH = height - PAD * 2;
        if (rowTop < scrollPx) {
            scrollPx = rowTop;
        } else if (rowTop + ROW_HEIGHT > scrollPx + viewH) {
            scrollPx = rowTop + ROW_HEIGHT - viewH;
        }
        scrollPx = Math.max(0, Math.min(scrollPx, maxScroll()));
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;

        ctx.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF000000);
        ctx.fill(getX(), getY(), getX() + width, getY() + height, 0xC0101010);

        ctx.enableScissor(getX(), getY(), getX() + width, getY() + height);
        int baseY = getY() + PAD - scrollPx;
        if (filtered.isEmpty()) {
            ctx.drawText(tr, "No results", getX() + 4, getY() + 4, 0xFF808080, false);
        }
        for (int i = 0; i < filtered.size(); i++) {
            int rowY = baseY + i * ROW_HEIGHT;
            if (rowY + ROW_HEIGHT < getY() || rowY > getY() + height) {
                continue;
            }
            CapeRegistry.Cape cape = filtered.get(i);
            boolean available = cape.isAvailable();

            if (cape.id().equals(selectedId)) {
                ctx.fill(getX(), rowY, getX() + width, rowY + ROW_HEIGHT, 0x66FFFFFF);
            } else if (mouseX >= getX() && mouseX <= getX() + width && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                ctx.fill(getX(), rowY, getX() + width, rowY + ROW_HEIGHT, 0x22FFFFFF);
            }

            int color = available ? 0xFFFFFFFF : 0xFF707070;
            String label = cape.displayName() + (available ? "" : "  (unavailable)");
            ctx.drawText(tr, label, getX() + 4, rowY + 2, color, false);
        }
        ctx.disableScissor();

        int max = maxScroll();
        if (max > 0) {
            int trackH = height;
            int thumbH = Math.max(12, (int) ((long) trackH * height / (filtered.size() * ROW_HEIGHT)));
            int thumbY = getY() + (int) ((long) (trackH - thumbH) * scrollPx / max);
            ctx.fill(getX() + width - 3, thumbY, getX() + width - 1, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        int rel = (int) (click.y() - getY() - PAD + scrollPx);
        int idx = rel / ROW_HEIGHT;
        if (idx >= 0 && idx < filtered.size() && filtered.get(idx).isAvailable()) {
            selectedId = filtered.get(idx).id();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        scrollPx = Math.max(0, Math.min(scrollPx - (int) (verticalAmount * ROW_HEIGHT), maxScroll()));
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        // No narration needed for this custom widget.
    }
}
