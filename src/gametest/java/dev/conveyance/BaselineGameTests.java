package dev.conveyance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.event.OnDatapackSyncEvent;

@GameTestHolder(Conveyance.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaselineGameTests {
    @GameTest(template = "empty")
    public static void creaturesAndXpCannotSpawn(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var type : new EntityType<?>[] {EntityType.COW, EntityType.ZOMBIE,
                EntityType.VILLAGER, EntityType.WARDEN, EntityType.ENDER_DRAGON, EntityType.ALLAY}) {
            var entity = type.create(level);
            helper.assertTrue(!level.addFreshEntity(entity), "Creature was allowed: " + type);
        }
        helper.assertTrue(!level.addFreshEntity(new ExperienceOrb(level, 0, 100, 0, 5)),
                "An experience orb was allowed");
        var stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        helper.assertTrue(level.addFreshEntity(stand), "An ordinary armor stand was blocked");
        stand.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldGenerationIsStripped(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        for (var structure : structures) {
            if (structures.getKey(structure).getNamespace().equals("minecraft")) {
                helper.assertTrue(structure.biomes().size() == 0,
                        "Structure still has eligible biomes: " + structures.getKey(structure));
            }
        }
        var disabledFeatures = registries.registryOrThrow(Registries.PLACED_FEATURE)
                .getTagOrEmpty(net.minecraft.tags.TagKey.create(Registries.PLACED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath("conveyance", "disabled")));
        for (var biome : registries.registryOrThrow(Registries.BIOME)) {
            for (var category : net.minecraft.world.entity.MobCategory.values()) {
                helper.assertTrue(biome.getMobSettings().getMobs(category).isEmpty(),
                        "A biome still has creature spawns");
            }
            for (var feature : disabledFeatures) {
                helper.assertTrue(!biome.getGenerationSettings().hasFeature(feature.value()),
                        "A biome still contains a disabled feature");
            }
        }
        var bonusChest = registries.registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.fromNamespaceAndPath("minecraft", "bonus_chest"));
        helper.assertTrue(bonusChest.feature() == Feature.NO_OP, "Bonus chest generation remains enabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipesKeepOrdinaryCrafting(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        for (var recipe : manager.getRecipes()) {
            helper.assertTrue(!ItemPolicy.isDisabled(recipe.getResultItem(helper.getLevel().registryAccess())),
                    "Recipe makes a disabled item: " + recipe.getId());
        }
        for (String recipe : new String[] {"enchanting_table", "brewing_stand", "piston",
                "redstone", "ender_chest", "netherite_sword_smithing", "oak_button"}) {
            helper.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("minecraft", recipe)).isEmpty(),
                    "Disabled recipe was loaded: " + recipe);
        }
        helper.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("minecraft", "iron_pickaxe")).isPresent(),
                "Ordinary tools can no longer be crafted");
        helper.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("minecraft", "furnace")).isPresent(),
                "The furnace recipe was removed");

        // Simulate recipes being restored by a data reload, then use the real
        // synchronization event that runs before clients receive the new list.
        var restored = new ArrayList<>(manager.getRecipes());
        var outputLeak = ResourceLocation.fromNamespaceAndPath("conveyance", "test_disabled_output");
        var inputLeak = ResourceLocation.fromNamespaceAndPath("conveyance", "test_disabled_input");
        restored.add(new ShapelessRecipe(outputLeak, "", CraftingBookCategory.MISC, new ItemStack(Items.PISTON),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.COBBLESTONE))));
        restored.add(new ShapelessRecipe(inputLeak, "", CraftingBookCategory.MISC, new ItemStack(Items.STONE),
                NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.REDSTONE))));
        manager.replaceRecipes(restored);
        MinecraftForge.EVENT_BUS.post(new OnDatapackSyncEvent(helper.getLevel().getServer().getPlayerList(), null));
        helper.assertTrue(manager.byKey(outputLeak).isEmpty() && manager.byKey(inputLeak).isEmpty(),
                "Reload restored disabled recipes");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void playerInventoryAndLevelsAreCleaned(GameTestHelper helper) {
        ServerPlayer player = makePlayer(helper);
        player.getInventory().setItem(0, new ItemStack(Items.ENDER_PEARL));
        player.getInventory().setItem(1, new ItemStack(Items.PISTON));
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.enchant(Enchantments.BLOCK_FORTUNE, 3);
        player.getInventory().setItem(2, pickaxe);
        player.getInventory().setItem(3, new ItemStack(Items.COBBLESTONE));
        player.getEnderChestInventory().setItem(0, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.experienceLevel = 20;
        player.totalExperience = 400;
        player.experienceProgress = 0.5F;
        helper.runAfterDelay(3, () -> {
            // GameTest's mock connection is not ticked by the network listener.
            // Exercise the same vanilla player tick used by real connections.
            player.doTick();
            helper.assertTrue(player.getInventory().getItem(0).isEmpty(), "Ender pearl survived a tick");
            helper.assertTrue(player.getInventory().getItem(1).isEmpty(), "Piston survived a tick");
            helper.assertTrue(player.getInventory().getItem(2).is(Items.IRON_PICKAXE)
                    && !player.getInventory().getItem(2).isEnchanted(), "Tool was not retained without magic");
            helper.assertTrue(player.getInventory().getItem(3).is(Items.COBBLESTONE), "Ordinary item was lost");
            helper.assertTrue(player.getEnderChestInventory().getItem(0).isEmpty(), "Stored magical item survived");
            helper.assertTrue(player.experienceLevel == 0 && player.totalExperience == 0
                    && player.experienceProgress == 0, "Existing XP survived");
            player.giveExperiencePoints(100);
            player.giveExperienceLevels(5);
            helper.assertTrue(player.experienceLevel == 0 && player.totalExperience == 0, "XP can still be granted");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void dropsAndPotionsCannotRestoreMagic(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.assertTrue(!level.addFreshEntity(new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(),
                new ItemStack(Items.ENCHANTED_BOOK))), "Enchanted book drop was allowed");
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.enchant(Enchantments.BLOCK_EFFICIENCY, 5);
        var drop = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), tool);
        helper.assertTrue(level.addFreshEntity(drop), "Ordinary tool drop was blocked");
        helper.assertTrue(!drop.getItem().isEnchanted(), "Dropped tool kept its enchantment");
        drop.discard();
        helper.assertTrue(ItemPolicy.isDisabled(PotionUtils.setPotion(new ItemStack(Items.POTION),
                Potions.STRONG_HEALING)), "Healing potion was allowed");
        helper.assertTrue(!ItemPolicy.isDisabled(PotionUtils.setPotion(new ItemStack(Items.POTION),
                Potions.WATER)), "Ordinary water bottle was blocked");
        var player = helper.makeMockSurvivalPlayer();
        helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200)),
                "A magical player effect was allowed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeInventoryIsFiltered(GameTestHelper helper) {
        for (var item : new net.minecraft.world.item.Item[] {Items.PISTON, Items.REDSTONE,
                Items.CALIBRATED_SCULK_SENSOR, Items.RESPAWN_ANCHOR, Items.GLOWSTONE,
                Items.ENCHANTING_TABLE, Items.BREWING_STAND, Items.ENDER_CHEST,
                Items.TOTEM_OF_UNDYING, Items.BEACON, Items.CONDUIT, Items.ELYTRA}) {
            helper.assertTrue(ItemPolicy.isDisabled(new ItemStack(item)), "Missing removal rule: " + item);
        }
        CreativeModeTabs.tryRebuildTabContents(FeatureFlags.VANILLA_SET, true, helper.getLevel().registryAccess());
        boolean foundPickaxe = false;
        for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            for (ItemStack stack : tab.getDisplayItems()) {
                helper.assertTrue(!ItemPolicy.isDisabled(stack) && !stack.isEnchanted(),
                        "Creative exposes disabled content: " + stack);
                foundPickaxe |= stack.is(Items.IRON_PICKAXE);
            }
            for (ItemStack stack : tab.getSearchTabDisplayItems()) {
                helper.assertTrue(!ItemPolicy.isDisabled(stack), "Creative search exposes disabled content");
            }
        }
        helper.assertTrue(foundPickaxe, "Ordinary tools disappeared from Creative");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void existingMagicBlocksAndPortalsAreBlocked(GameTestHelper helper) {
        var player = makePlayer(helper);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.ENCHANTING_TABLE);
        var interaction = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        helper.assertTrue(MinecraftForge.EVENT_BUS.post(interaction), "Existing enchanting table can be used");
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(player.changeDimension(nether) == null && player.level().dimension() == Level.OVERWORLD,
                "Player entered the Nether");
        var end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(player.changeDimension(end) == null && player.level().dimension() == Level.OVERWORLD,
                "Player entered the End");
        player.discard();
        helper.succeed();
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        // Forge's current login handshake requires a Netty channel, which the
        // vanilla GameTest mock lacks. Use a real ServerPlayer with a sink for
        // outbound packets, avoiding login while retaining actual tick logic.
        var server = helper.getLevel().getServer();
        var player = new ServerPlayer(server, helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "conveyance-test"));
        player.connection = new ServerGamePacketListenerImpl(server,
                new Connection(PacketFlow.SERVERBOUND), player) {
            @Override
            public void send(Packet<?> packet) {}

            @Override
            public void send(Packet<?> packet, PacketSendListener listener) {}
        };
        return player;
    }
}
