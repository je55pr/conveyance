package dev.conveyance.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;

/** An icon-only control with a named tooltip and accessible narration. */
public final class FactoryIconButton extends Button {
    private final ItemStack icon;
    private final String glyph;
    public FactoryIconButton(int x, int y, Component label, ItemStack icon, OnPress action) {
        super(x, y, 20, 20, label, action, DEFAULT_NARRATION);
        this.icon = icon.copy(); glyph = null; setTooltip(Tooltip.create(label));
    }
    public FactoryIconButton(int x, int y, Component label, String glyph, OnPress action) {
        super(x, y, 20, 20, label, action, DEFAULT_NARRATION);
        icon = ItemStack.EMPTY; this.glyph = glyph; setTooltip(Tooltip.create(label));
    }
    @Override protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                active && isHoveredOrFocused() ? FactoryWindowStyle.ACCENT : FactoryWindowStyle.SLOT_BORDER);
        graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, FactoryWindowStyle.BACKGROUND);
        if (glyph == null) graphics.renderItem(icon, getX() + 2, getY() + 2);
        else graphics.drawCenteredString(Minecraft.getInstance().font, glyph, getX() + getWidth() / 2, getY() + 6, FactoryWindowStyle.TEXT);
    }
}
