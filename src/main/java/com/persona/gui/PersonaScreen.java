package com.persona.gui;

import com.persona.config.PersonaConfig;
import com.persona.identity.IdentityManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * The Persona control panel (opened with Right Shift). Two tabs, selectable with the
 * buttons at the top:
 * Custom: set name, skin and cape independently.
 * Impersonate: type one username and take their whole identity (name, skin and cape).
 * A live rotatable 3D preview sits on the right.
 */
public class PersonaScreen extends Screen {
    private static final int ACCENT      = 0xFF8B7CF6;
    private static final int ACCENT_DIM  = 0xFF5B4FA8;
    private static final int PANEL_BG_TOP = 0xE01C1830;
    private static final int PANEL_BG_BOT = 0xE0141020;
    private static final int PANEL_BORDER = 0xFF3A3355;
    private static final int LABEL        = 0xFFC9C4DE;
    private static final int LABEL_DIM    = 0xFF8B85A8;

    private static final int TAB_CUSTOM = 0;
    private static final int TAB_IMPERSONATE = 1;

    private int tab = TAB_CUSTOM;
    private String nameText;
    private String skinText;
    private String capeSel;
    private String impText = "";
    private String status = "";

    private TextFieldWidget nameField;
    private TextFieldWidget skinField;
    private TextFieldWidget capeSearch;
    private CapeListWidget capeList;
    private TextFieldWidget impersonateField;
    private PlayerPreviewWidget preview;

    private int panelX, panelY, panelW, panelH;
    private int leftX, leftW, rightX, previewW;
    private int tabX0, tabX1, tabY, tabW, tabH;
    private int contentY;
    private int nameLabelY, skinLabelY, capeLabelY, impLabelY;
    private int previewLabelY, previewBottom;

    public PersonaScreen() {
        super(Text.literal("Persona"));
        PersonaConfig cfg = IdentityManager.get().config();
        this.nameText = cfg.pseudo == null ? "" : cfg.pseudo;
        this.skinText = cfg.skinSource == null ? "" : cfg.skinSource;
        this.capeSel = cfg.capeId == null ? "none" : cfg.capeId;
    }

    @Override
    protected void init() {
        int contentW = Math.min(this.width - 24, 470);
        panelX = (this.width - contentW) / 2;
        panelW = contentW;
        panelY = 34;
        panelH = this.height - panelY - 30;

        int inner = 14;
        leftX = panelX + inner;
        previewW = 150;
        rightX = panelX + panelW - inner - previewW;
        int colGap = 16;
        leftW = rightX - colGap - leftX;

        int btnY = panelY + panelH - 26;

        tabY = panelY + 6;
        tabH = 18;
        int tabGap = 6;
        tabW = (leftW - tabGap) / 2;
        tabX0 = leftX;
        tabX1 = leftX + tabW + tabGap;
        addDrawableChild(new PersonaButton(tabX0, tabY, tabW, tabH, Text.literal("Custom"),
                () -> switchTab(TAB_CUSTOM)).style(PersonaButton.Style.TAB).selected(tab == TAB_CUSTOM));
        addDrawableChild(new PersonaButton(tabX1, tabY, tabW, tabH, Text.literal("Impersonate"),
                () -> switchTab(TAB_IMPERSONATE)).style(PersonaButton.Style.TAB).selected(tab == TAB_IMPERSONATE));

        contentY = tabY + tabH + 12;

        if (tab == TAB_CUSTOM) {
            buildCustom(btnY);
        } else {
            buildImpersonate();
        }

        previewLabelY = tabY;
        int previewTop = contentY;
        previewBottom = btnY - 6;
        preview = new PlayerPreviewWidget(rightX, previewTop, previewW, previewBottom - previewTop);
        addDrawableChild(preview);

        int bw = (leftW - 12) / 3;
        addDrawableChild(new PersonaButton(leftX, btnY, bw, 20, Text.literal("Apply"), this::onApply)
                .style(PersonaButton.Style.PRIMARY));
        addDrawableChild(new PersonaButton(leftX + bw + 6, btnY, bw, 20, Text.literal("Reset"), this::onReset));
        addDrawableChild(new PersonaButton(leftX + 2 * (bw + 6), btnY, bw, 20, Text.literal("Close"), this::close));
    }

    private void buildCustom(int btnY) {
        int y = contentY;
        nameLabelY = y;
        nameField = new TextFieldWidget(this.textRenderer, leftX, y + 10, leftW, 18, Text.literal("Name"));
        nameField.setMaxLength(48);
        nameField.setText(nameText);
        addDrawableChild(nameField);

        y += 44;
        skinLabelY = y;
        skinField = new TextFieldWidget(this.textRenderer, leftX, y + 10, leftW, 18, Text.literal("Skin"));
        skinField.setMaxLength(16);
        skinField.setPlaceholder(Text.literal("Player name..."));
        skinField.setText(skinText);
        addDrawableChild(skinField);

        y += 44;
        capeLabelY = y;
        capeSearch = new TextFieldWidget(this.textRenderer, leftX, y + 10, leftW, 16, Text.literal("Search"));
        capeSearch.setMaxLength(32);
        capeSearch.setPlaceholder(Text.literal("Search a cape..."));

        int listY = y + 30;
        int listH = (btnY - 6) - listY;
        capeList = new CapeListWidget(leftX, listY, leftW, listH);
        capeList.setSelectedById(capeSel);
        addDrawableChild(capeList);
        capeSearch.setChangedListener(q -> capeList.setFilter(q));
        addDrawableChild(capeSearch);
    }

    private void buildImpersonate() {
        impLabelY = contentY;
        impersonateField = new TextFieldWidget(this.textRenderer, leftX, contentY + 12, leftW, 18, Text.literal("Player"));
        impersonateField.setMaxLength(16);
        impersonateField.setPlaceholder(Text.literal("Player to impersonate..."));
        impersonateField.setText(impText);
        addDrawableChild(impersonateField);
    }

    private void captureInputs() {
        if (tab == TAB_CUSTOM && nameField != null) {
            nameText = nameField.getText();
            skinText = skinField.getText();
            capeSel = capeList.getSelectedId();
        } else if (tab == TAB_IMPERSONATE && impersonateField != null) {
            impText = impersonateField.getText();
        }
    }

    private void switchTab(int t) {
        if (t == tab) {
            return;
        }
        captureInputs();
        tab = t;
        status = "";
        clearAndInit();
    }

    private void onApply() {
        captureInputs();
        PersonaConfig cfg = IdentityManager.get().config();
        if (tab == TAB_CUSTOM) {
            cfg.pseudo = nameText.trim();
            cfg.skinSource = skinText.trim();
            cfg.capeId = capeSel;
            status = "Applied. Visible only to you.";
        } else {
            String who = impText.trim();
            cfg.pseudo = who;
            cfg.skinSource = who;
            cfg.capeId = "none";
            nameText = who;
            skinText = who;
            capeSel = "none";
            status = who.isEmpty() ? "Enter a player name to impersonate." : "Impersonating " + who + ".";
        }
        IdentityManager.get().applyAndSave();
    }

    private void onReset() {
        PersonaConfig cfg = IdentityManager.get().config();
        cfg.pseudo = "";
        cfg.skinSource = "";
        cfg.capeId = "none";
        nameText = "";
        skinText = "";
        capeSel = "none";
        impText = "";
        IdentityManager.get().applyAndSave();
        status = "Reset.";
        clearAndInit();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        panel(ctx, panelX, panelY, panelX + panelW, panelY + panelH);
        int divX = rightX - 8;
        ctx.fill(divX, panelY + 12, divX + 1, panelY + panelH - 12, 0x22FFFFFF);

        super.render(ctx, mouseX, mouseY, delta);

        String title = "P E R S O N A";
        int tw = this.textRenderer.getWidth(title);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(title),
                this.width / 2 - tw / 2, panelY - 22, ACCENT);
        ctx.fill(this.width / 2 - tw / 2, panelY - 10, this.width / 2 + tw / 2, panelY - 9, ACCENT_DIM);

        if (tab == TAB_CUSTOM) {
            sectionLabel(ctx, "NAME", leftX, nameLabelY);
            hint(ctx, "head, tab, chat", leftX + 48, nameLabelY);
            sectionLabel(ctx, "SKIN", leftX, skinLabelY);
            hint(ctx, "copy a player's skin", leftX + 42, skinLabelY);
            sectionLabel(ctx, "CAPE", leftX, capeLabelY);
        } else {
            sectionLabel(ctx, "IMPERSONATE A PLAYER", leftX, impLabelY);
            hint(ctx, "Takes their name, skin and cape.", leftX, impLabelY + 34);
            hint(ctx, "All in one. Visible only to you.", leftX, impLabelY + 46);
        }

        PersonaConfig cfg = IdentityManager.get().config();
        String shown = (cfg.pseudo != null && !cfg.pseudo.isBlank())
                ? cfg.pseudo
                : (this.client != null && this.client.getSession() != null ? this.client.getSession().getUsername() : "You");
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal(shown),
                rightX + previewW / 2, previewLabelY + 5, 0xFFF0EEFA);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("drag to rotate"),
                rightX + previewW / 2, previewBottom + 3, LABEL_DIM);

        if (!status.isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(status),
                    leftX, panelY + panelH + 6, 0xFF88E0A0);
        }
    }

    private void panel(DrawContext ctx, int x1, int y1, int x2, int y2) {
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, PANEL_BORDER);
        ctx.fillGradient(x1, y1, x2, y2, PANEL_BG_TOP, PANEL_BG_BOT);
        ctx.fill(x1, y1, x2, y1 + 1, ACCENT_DIM);
    }

    private void sectionLabel(DrawContext ctx, String text, int x, int y) {
        ctx.fill(x, y + 1, x + 2, y + 8, ACCENT);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(text), x + 6, y, LABEL);
    }

    private void hint(DrawContext ctx, String text, int x, int y) {
        ctx.drawTextWithShadow(this.textRenderer, Text.literal(text), x, y, LABEL_DIM);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
