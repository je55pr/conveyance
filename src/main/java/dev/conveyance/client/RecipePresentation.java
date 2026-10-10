package dev.conveyance.client;

import dev.conveyance.factory.AssemblyRecipes;
import dev.conveyance.factory.FactoryContent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/** Shared recipe groups, category icons and ingredient tooltips for crafting and machines. */
final class RecipePresentation {
    static final List<String> CATEGORIES = List.of("intermediates", "logistics", "production", "power", "science");
    private static final List<String> FAMILIES = List.of("miners", "inserters", "conveyors", "storage", "furnaces", "assemblers", "generators", "poles", "radars", "labs", "science", "tools", "components");
    record Row(String family, List<CraftingRecipe> recipes) {}

    static List<Row> grouped(List<CraftingRecipe> recipes, Level level, int columns) {
        var rows = new ArrayList<Row>();
        for (String group : FAMILIES) {
            var matches = recipes.stream().filter(recipe -> family(recipe, level).equals(group)).toList();
            for (int first = 0; first < matches.size(); first += columns)
                rows.add(new Row(group, matches.subList(first, Math.min(matches.size(), first + columns))));
        }
        return rows;
    }
    private static String family(CraftingRecipe recipe, Level level) {
        return switch (BuiltInRegistries.ITEM.getKey(recipe.getResultItem(level.registryAccess()).getItem()).getPath()) {
            case "miner", "burner_miner" -> "miners";
            case "inserter", "burner_inserter", "long_inserter" -> "inserters";
            case "conveyor", "slope_conveyor", "junction" -> "conveyors";
            case "wood_crate", "iron_crate" -> "storage";
            case "industrial_furnace" -> "furnaces";
            case "assembler" -> "assemblers";
            case "generator", "solar_panel" -> "generators";
            case "power_pole" -> "poles";
            case "radar" -> "radars";
            case "research_lab" -> "labs";
            case "red_science_pack", "green_science_pack" -> "science";
            case "iron_pickaxe" -> "tools";
            default -> "components";
        };
    }
    static ItemStack categoryIcon(String category) {
        return new ItemStack(switch (category) {
            case "intermediates" -> FactoryContent.GEAR_ITEM.get();
            case "logistics" -> FactoryContent.CONVEYOR_ITEM.get();
            case "production" -> FactoryContent.BURNER_MINER_ITEM.get();
            case "power" -> FactoryContent.POWER_POLE_ITEM.get();
            case "science" -> FactoryContent.RED_SCIENCE_ITEM.get();
            default -> FactoryContent.GEAR_ITEM.get();
        });
    }
    static void tooltip(GuiGraphics graphics, CraftingRecipe recipe, List<ItemStack> raw,
                        ToIntFunction<AssemblyRecipes.Requirement> ingredientColour,
                        int width, int height, int mouseX, int mouseY) {
        var minecraft = Minecraft.getInstance();
        var font = minecraft.font;
        var requirements = AssemblyRecipes.requirements(recipe);
        ItemStack output = recipe.getResultItem(minecraft.level.registryAccess());
        Component title = Component.literal(output.getCount() > 1 ? output.getCount() + " × " : "").append(output.getHoverName());
        Component rawTitle = Component.translatable("menu.conveyance.total_raw");
        int boxWidth = Math.min(width - 8, Math.max(264, Math.max(Math.max(font.width(title), font.width(rawTitle)) + 16, Math.max(requirements.size(), raw.size()) * 30 + 16)));
        var info = Screen.getTooltipFromItem(minecraft, output);
        var infoLines = new ArrayList<FormattedCharSequence>();
        for (int line = 1; line < info.size(); line++) infoLines.addAll(font.split(info.get(line), boxWidth - 16));
        int infoHeight = infoLines.size() * 10;
        int iconColumns = Math.max(1, (boxWidth - 16) / 30);
        int rawTitleY = 35 + infoHeight + 24 * Math.max(1, (requirements.size() + iconColumns - 1) / iconColumns) + 6;
        int rawIconsY = rawTitleY + 13;
        int boxHeight = rawIconsY + 24 * Math.max(1, (raw.size() + iconColumns - 1) / iconColumns) + 8;
        int x = Math.max(4, Math.min(mouseX + 12, width - boxWidth - 4));
        int y = Math.max(4, Math.min(mouseY + 12, height - boxHeight - 4));
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 450);
        graphics.fill(x - 1, y - 1, x + boxWidth + 1, y + boxHeight + 1, 0xFF718292);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xFA17212C);
        graphics.drawString(font, title, x + 8, y + 7, 0xFFE7EEF6, false);
        for (int line = 0; line < infoLines.size(); line++) graphics.drawString(font, infoLines.get(line), x + 8, y + 21 + line * 10, 0xFFBBC8D4, false);
        graphics.drawString(font, Component.translatable("menu.conveyance.ingredients"), x + 8, y + 23 + infoHeight, 0xFFA9BBC9, false);
        for (int i = 0; i < requirements.size(); i++) {
            var requirement = requirements.get(i);
            var choices = requirement.ingredient().getItems();
            if (choices.length == 0) continue;
            costIcon(graphics, choices[0], requirement.count(), x + 8 + i % iconColumns * 30,
                    y + 35 + infoHeight + i / iconColumns * 24, ingredientColour.applyAsInt(requirement));
        }
        graphics.drawString(font, rawTitle, x + 8, y + rawTitleY, 0xFFA9BBC9, false);
        for (int i = 0; i < raw.size(); i++) costIcon(graphics, raw.get(i), raw.get(i).getCount(), x + 8 + i % iconColumns * 30,
                y + rawIconsY + i / iconColumns * 24, 0xFFE7EEF6);
        graphics.pose().popPose();
    }
    private static void costIcon(GuiGraphics graphics, ItemStack item, int count, int x, int y, int colour) {
        var font = Minecraft.getInstance().font;
        graphics.renderItem(item, x, y);
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 200);
        String text = Integer.toString(count);
        graphics.drawString(font, text, x + 20 - font.width(text), y + 11, colour, true);
        graphics.pose().popPose();
    }
    private RecipePresentation() {}
}
