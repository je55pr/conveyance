package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.ProcessingMenu;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class InventoryNetwork {
    private static final String VERSION = "6";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "inventory"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    public record OpenInventory() {}
    public record Harvest(BlockPos pos) {}
    public record SyncResearch(int mask, int selected, int[] progress) {
        static SyncResearch capture(Level level) {
            return new SyncResearch(ResearchState.mask(level), ResearchState.selected(level), ResearchState.progressSnapshot(level));
        }
    }
    public record Craft(ResourceLocation recipe, boolean bulk) {}
    public record SelectAssembly(int containerId, ResourceLocation recipe) {}
    public static void register() {
        CHANNEL.registerMessage(0, OpenInventory.class, (message, buffer) -> {}, buffer -> new OpenInventory(), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) open(context.getSender()); });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, Harvest.class, (message, buffer) -> buffer.writeBlockPos(message.pos), buffer -> new Harvest(buffer.readBlockPos()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) TerrainHarvest.harvest(context.getSender(), message.pos); });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(2, SyncResearch.class, (message, buffer) -> {
            buffer.writeVarInt(message.mask); buffer.writeVarInt(message.selected); buffer.writeVarIntArray(message.progress);
        }, buffer -> new SyncResearch(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarIntArray(Technology.values().length)), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> ResearchState.updateClient(message.mask, message.selected, message.progress));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3, Craft.class, (message, buffer) -> { buffer.writeResourceLocation(message.recipe); buffer.writeBoolean(message.bulk); },
                buffer -> new Craft(buffer.readResourceLocation(), buffer.readBoolean()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.containerMenu instanceof PlayerWorkbenchMenu menu) || !menu.stillValid(player)) return;
                if (!message.recipe.getNamespace().equals(Conveyance.MOD_ID)
                        || !player.isCreative() && !ResearchState.recipeUnlocked(message.recipe, ResearchState.mask(player.level()))) return;
                var found = player.level().getRecipeManager().byKey(message.recipe).orElse(null);
                if (found instanceof CraftingRecipe recipe && !HandCrafting.craft(player, menu.extra(), recipe, message.bulk)) {
                    player.displayClientMessage(Component.translatable("message.conveyance.craft_failed"), true);
                }
            });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(4, SelectAssembly.class, (message, buffer) -> {
            buffer.writeVarInt(message.containerId); buffer.writeResourceLocation(message.recipe);
        }, buffer -> new SelectAssembly(buffer.readVarInt(), buffer.readResourceLocation()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null && player.containerMenu instanceof ProcessingMenu menu && menu.containerId == message.containerId) {
                    menu.selectRecipe(player, message.recipe);
                }
            });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(ServerPlayer player) {
        if (player.isAlive() && !player.isSpectator() && !(player.containerMenu instanceof PlayerWorkbenchMenu)) {
            player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PlayerWorkbenchMenu(id, inventory), Component.translatable("menu.conveyance.inventory")));
        }
    }
    public static void requestOpen() { CHANNEL.sendToServer(new OpenInventory()); }
    public static void requestHarvest(BlockPos pos) { CHANNEL.sendToServer(new Harvest(pos)); }
    public static void requestCraft(ResourceLocation recipe, boolean bulk) { CHANNEL.sendToServer(new Craft(recipe, bulk)); }
    public static void requestAssemblyRecipe(int containerId, ResourceLocation recipe) { CHANNEL.sendToServer(new SelectAssembly(containerId, recipe)); }
    public static void sync(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncResearch.capture(player.level())); }
    static void syncAll(Level level) { CHANNEL.send(PacketDistributor.ALL.noArg(), SyncResearch.capture(level)); }
    private InventoryNetwork() {}
}
