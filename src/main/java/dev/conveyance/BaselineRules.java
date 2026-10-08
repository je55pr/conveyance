package dev.conveyance;

import java.util.ArrayList;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BaselineRules {
    private static final Set<String> DISABLED_ENTITIES = Set.of(
            "experience_orb", "experience_bottle", "ender_pearl", "eye_of_ender",
            "end_crystal", "area_effect_cloud", "potion", "evoker_fangs",
            "fireball", "small_fireball", "dragon_fireball", "wither_skull",
            "shulker_bullet", "fishing_bobber", "hopper_minecart");

    private BaselineRules() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if ("minecraft".equals(id.getNamespace())
                && ((entity instanceof LivingEntity && !(entity instanceof Player)
                        && !(entity instanceof ArmorStand)) || DISABLED_ENTITIES.contains(id.getPath()))) {
            // Includes entities loaded from existing saves, breeding, commands,
            // spawners and buckets. Do not touch chunks inside this event.
            event.setCanceled(true);
        } else if (entity instanceof ItemEntity drop && !event.getLevel().isClientSide) {
            ItemPolicy.sanitize(drop.getItem());
            if (drop.getItem().isEmpty()) {
                event.setCanceled(true);
            } else {
                drop.setItem(drop.getItem());
            }
        }
    }

    @SubscribeEvent
    public static void onExperience(PlayerXpEvent event) {
        if (event.isCancelable()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        // Inspect equipment before vanilla applies enchantment-driven behaviour.
        boolean changed = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            changed |= ItemPolicy.sanitize(player.getInventory().getItem(slot));
        }
        for (int slot = 0; slot < player.getEnderChestInventory().getContainerSize(); slot++) {
            ItemPolicy.sanitize(player.getEnderChestInventory().getItem(slot));
        }
        ItemPolicy.sanitize(player.containerMenu.getCarried());
        if (changed) {
            player.getInventory().setChanged();
        }
        for (var effect : new ArrayList<>(player.getActiveEffects())) {
            if ("minecraft".equals(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect()).getNamespace())) {
                player.removeEffect(effect.getEffect());
            }
        }

        // A save may already have its player in a dimension that is now disabled.
        if (player.level().dimension() != Level.OVERWORLD) {
            var overworld = player.getServer().overworld();
            var spawn = overworld.getSharedSpawnPos();
            int height = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    spawn.getX(), spawn.getZ());
            player.teleportTo(overworld, spawn.getX() + 0.5, height, spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
    }

    @SubscribeEvent
    public static void onContainerOpen(PlayerContainerEvent.Open event) {
        if (!event.getEntity().level().isClientSide) {
            for (Slot slot : event.getContainer().slots) {
                if (ItemPolicy.sanitize(slot.getItem())) {
                    slot.setChanged();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player
                && "minecraft".equals(BuiltInRegistries.MOB_EFFECT
                        .getKey(event.getEffectInstance().getEffect()).getNamespace())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ItemPolicy.isDisabled(event.getItemStack())
                || event.getLevel().getBlockState(event.getPos()).is(ItemPolicy.DISABLED_BLOCKS)
                || (event.getItemStack().getItem() instanceof BlockItem item
                        && item.getBlock().defaultBlockState().is(ItemPolicy.DISABLED_BLOCKS))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        } else {
            ItemPolicy.sanitize(event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ItemPolicy.isDisabled(event.getItemStack())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        } else {
            ItemPolicy.sanitize(event.getItemStack());
        }
    }

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (ItemPolicy.isDisabled(event.getItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getPlacedBlock().is(ItemPolicy.DISABLED_BLOCKS)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDimensionTravel(EntityTravelToDimensionEvent event) {
        if (event.getDimension() != Level.OVERWORLD) {
            event.setCanceled(true);
        }
    }
}
