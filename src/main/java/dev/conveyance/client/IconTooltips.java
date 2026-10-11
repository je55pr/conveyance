package dev.conveyance.client;

import com.mojang.datafixers.util.Either;
import dev.conveyance.Conveyance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/** Explicit icon associations keep names, styles and translations intact. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class IconTooltips {
    private static final int ICON_WIDTH = 20, ROW_HEIGHT = 18;
    public record Row(Component prefix, ItemStack icon, FormattedText text, int maxWidth) implements TooltipComponent {}

    public static Row text(Component text) { return new Row(Component.empty(), ItemStack.EMPTY, text, width()); }
    public static Row item(ItemStack item, Component text) { return item(Component.empty(), item, text); }
    public static Row item(Component prefix, ItemStack item, Component text) { return new Row(prefix, item.copyWithCount(1), text, width()); }
    private static int width() { return Math.max(40, Math.min(320, Minecraft.getInstance().getWindow().getGuiScaledWidth() - 16)); }
    public static List<Row> itemLines(ItemStack item, List<Component> lines) {
        var rows = new ArrayList<Row>();
        for (int index = 0; index < lines.size(); index++) rows.add(index == 0 && !item.isEmpty() ? item(item, lines.get(index)) : text(lines.get(index)));
        return rows;
    }
    public static void render(GuiGraphics graphics, List<Row> rows, int mouseX, int mouseY) {
        var elements = new ArrayList<Either<FormattedText, TooltipComponent>>();
        for (Row row : rows) {
            if (row.icon().isEmpty()) elements.add(Either.left(row.text()));
            else elements.add(Either.right(row));
        }
        graphics.renderComponentTooltipFromElements(Minecraft.getInstance().font, elements, mouseX, mouseY, ItemStack.EMPTY);
    }
    @SubscribeEvent public static void register(RegisterClientTooltipComponentFactoriesEvent event) { event.register(Row.class, RenderRow::new); }

    private static final class RenderRow implements ClientTooltipComponent {
        private final Row row;
        private final List<FormattedCharSequence> lines;
        private final int prefixWidth, width, height;
        RenderRow(Row row) {
            this.row = row;
            var font = Minecraft.getInstance().font;
            prefixWidth = font.width(row.prefix());
            lines = font.split(row.text(), Math.max(24, row.maxWidth() - prefixWidth - ICON_WIDTH));
            width = prefixWidth + ICON_WIDTH + lines.stream().mapToInt(font::width).max().orElse(0);
            height = Math.max(ROW_HEIGHT, lines.size() * 10);
        }
        @Override public int getHeight() { return height; }
        @Override public int getWidth(Font font) { return width; }
        @Override public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffers) {
            int textY = y + Math.max(0, (height - lines.size() * 10) / 2);
            font.drawInBatch(row.prefix().getVisualOrderText(), x, textY, 0xFFFFFFFF, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            for (int index = 0; index < lines.size(); index++) font.drawInBatch(lines.get(index), x + prefixWidth + ICON_WIDTH, textY + index * 10,
                    0xFFFFFFFF, true, matrix, buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
        @Override public void renderImage(Font font, int x, int y, GuiGraphics graphics) { graphics.renderItem(row.icon(), x + prefixWidth, y + (height - 16) / 2); }
    }

    /** Ordinary item tooltips share the same title row on every inventory screen. */
    @Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, value = Dist.CLIENT)
    public static final class ItemEvents {
        @SubscribeEvent public static void gather(RenderTooltipEvent.GatherComponents event) {
            if (event.getItemStack().isEmpty() || event.getTooltipElements().isEmpty()) return;
            var first = event.getTooltipElements().get(0).left();
            if (first.isEmpty()) return;
            int maxWidth = Math.max(40, Math.min(320, event.getScreenWidth() - 16));
            event.getTooltipElements().set(0, Either.right(new Row(Component.empty(), event.getItemStack().copyWithCount(1), first.get(), maxWidth)));
        }
    }
    private IconTooltips() {}
}
