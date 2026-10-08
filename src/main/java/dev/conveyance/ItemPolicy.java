package dev.conveyance;

import java.util.ArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

/** Shared policy for Creative, inventories, dropped items, and interactions. */
public final class ItemPolicy {
    public static final TagKey<Item> DISABLED_ITEMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "disabled"));
    public static final TagKey<Block> DISABLED_BLOCKS = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "disabled"));

    private ItemPolicy() {}

    public static boolean isDisabled(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(DISABLED_ITEMS) || stack.getItem() instanceof SpawnEggItem) {
            return true;
        }
        // Keep ordinary water bottles for future chemistry, but no brewing bases
        // or potions, including custom-effect water bottles supplied by commands.
        return stack.is(Items.POTION) && (PotionUtils.getPotion(stack) != Potions.WATER
                || !PotionUtils.getMobEffects(stack).isEmpty());
    }

    public static boolean sanitize(ItemStack stack) {
        if (isDisabled(stack)) {
            stack.setCount(0);
            return true;
        }
        if (stack.isEmpty() || !stack.hasTag()) {
            return false;
        }
        boolean changed = stack.getTag().contains("Enchantments")
                || stack.getTag().contains("StoredEnchantments")
                || stack.getTag().contains("RepairCost");
        stack.removeTagKey("Enchantments");
        stack.removeTagKey("StoredEnchantments");
        stack.removeTagKey("RepairCost");
        return changed;
    }

    public static void onCreativeContents(BuildCreativeModeTabContentsEvent event) {
        // Collect keys first: mutating the linked map during iteration is unsafe.
        var removed = new ArrayList<ItemStack>();
        for (var entry : event.getEntries()) {
            if (isDisabled(entry.getKey()) || entry.getKey().isEnchanted()) {
                removed.add(entry.getKey());
            }
        }
        removed.forEach(event.getEntries()::remove);
    }
}
