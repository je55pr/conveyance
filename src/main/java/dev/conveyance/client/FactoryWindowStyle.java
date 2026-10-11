package dev.conveyance.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import dev.conveyance.player.PlayerStorageLayout;

/** Shared palette and drawing primitives; edit here to restyle every factory window. */
public final class FactoryWindowStyle {
    public static final int BORDER = 0xFF607080, BACKGROUND = 0xFF202A35, HEADER = 0xFF303D4A;
    public static final int SLOT_BORDER = 0xFF596674, SLOT_BACKGROUND = 0xFF111820;
    public static final int TEXT = 0xFFE7EEF6, MUTED = 0xFFB5C5D2, ACCENT = 0xFF8AD8A0, WARNING = 0xFFE3B44F;
    public static final int FUEL_PANEL_WIDTH = 140, STORAGE_PANEL_WIDTH = 8 * PlayerStorageLayout.CELL + 14;
    public static final int RECIPE_PANEL_WIDTH = PlayerStorageLayout.PANEL_WIDTH;
    public static final int COMPACT_WIDTH = PlayerStorageLayout.MACHINE_X + RECIPE_PANEL_WIDTH;
    public static final int COMPACT_HEIGHT = PlayerStorageLayout.MIN_HEIGHT;
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
        graphics.fill(x, y, x + width, y + height, BACKGROUND);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 20, HEADER);
    }
    public static void slot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_BORDER);
        graphics.fill(x, y, x + 16, y + 16, SLOT_BACKGROUND);
    }
    public static void progress(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color) {
        graphics.fill(x, y, x + width, y + height, SLOT_BACKGROUND);
        float progress = Float.isFinite(fraction) ? Mth.clamp(fraction, 0, 1) : 0;
        int filled = (int) (width * progress);
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, color);
    }
    private FactoryWindowStyle() {}
}
