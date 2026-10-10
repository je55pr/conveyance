package dev.conveyance.client;

import dev.conveyance.player.ExtraInventory;
import dev.conveyance.player.LinkedHotbar;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ClientInventoryState {
    public static void assemblySelected(int containerId, ResourceLocation recipe, boolean accepted) {
        if (Minecraft.getInstance().screen instanceof ProcessingScreen screen && screen.getMenu().containerId == containerId)
            screen.selectionConfirmed(recipe, accepted);
    }
    public static void accept(List<ItemStack> items, List<ResourceLocation> ids, int selected, ResourceLocation temporary) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        ExtraInventory.of(player).replace(items);
        var links = new ListTag();
        for (ResourceLocation id : ids.subList(1, ids.size())) links.add(StringTag.valueOf(id.toString()));
        player.getPersistentData().put(LinkedHotbar.DATA_KEY, links);
        LinkedHotbar.select(player, selected);
        if (selected == 0 && !temporary.toString().equals("minecraft:air")) player.getPersistentData().putString(LinkedHotbar.TEMP_KEY, temporary.toString());
    }
    private ClientInventoryState() {}
}
