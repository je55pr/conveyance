package dev.conveyance.player;

import dev.conveyance.Conveyance;
import dev.conveyance.factory.ProcessingMenu;
import dev.conveyance.factory.FactoryActions;
import dev.conveyance.client.ClientInventoryState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class InventoryNetwork {
    private static final String VERSION = "8";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "inventory"),
            () -> VERSION, VERSION::equals, VERSION::equals);
    public record OpenInventory(int tab) {}
    public record Harvest(BlockPos pos) {}
    public record SyncResearch(int mask, int selected, int[] progress) {
        static SyncResearch capture(Level level) {
            return new SyncResearch(ResearchState.mask(level), ResearchState.selected(level), ResearchState.progressSnapshot(level));
        }
    }
    public record Craft(ResourceLocation recipe, int batches) {}
    public record SelectAssembly(int containerId, ResourceLocation recipe) {}
    public record SyncInventory(List<ItemStack> items, List<ResourceLocation> links) {}
    public record BindLink(int containerId, int slot, ResourceLocation item) {}
    public record FactoryAction(BlockPos pos, int action) {}
    public static void register() {
        CHANNEL.registerMessage(0, OpenInventory.class, (message, buffer) -> buffer.writeVarInt(message.tab), buffer -> new OpenInventory(buffer.readVarInt()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) open(context.getSender(), message.tab); });
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
        CHANNEL.registerMessage(3, Craft.class, (message, buffer) -> { buffer.writeResourceLocation(message.recipe); buffer.writeVarInt(message.batches); },
                buffer -> new Craft(buffer.readResourceLocation(), buffer.readVarInt()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.containerMenu instanceof PlayerWorkbenchMenu menu) || !menu.stillValid(player)
                        || message.batches != 0 && message.batches != 1 && message.batches != 5) return;
                if (!message.recipe.getNamespace().equals(Conveyance.MOD_ID)
                        || !player.isCreative() && !ResearchState.recipeUnlocked(message.recipe, ResearchState.mask(player.level()))) return;
                var found = player.level().getRecipeManager().byKey(message.recipe).orElse(null);
                if (found instanceof CraftingRecipe recipe && !HandCrafting.craft(player, menu.extra(), recipe, message.batches)) {
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
        CHANNEL.registerMessage(5, SyncInventory.class, (message, buffer) -> {
            message.items.forEach(buffer::writeItem); message.links.forEach(buffer::writeResourceLocation);
        }, buffer -> {
            var items = new ArrayList<ItemStack>(); var links = new ArrayList<ResourceLocation>();
            for (int slot = 0; slot < ExtraInventory.SIZE; slot++) items.add(buffer.readItem());
            for (int slot = 0; slot < 9; slot++) links.add(buffer.readResourceLocation());
            return new SyncInventory(items, links);
        }, (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientInventoryState.accept(message.items, message.links)));
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, BindLink.class, (message, buffer) -> {
            buffer.writeVarInt(message.containerId); buffer.writeVarInt(message.slot); buffer.writeResourceLocation(message.item);
        }, buffer -> new BindLink(buffer.readVarInt(), buffer.readVarInt(), buffer.readResourceLocation()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) LinkedHotbar.bind(context.getSender(), message.containerId, message.slot, message.item); });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, FactoryAction.class, (message, buffer) -> { buffer.writeBlockPos(message.pos); buffer.writeVarInt(message.action); },
                buffer -> new FactoryAction(buffer.readBlockPos(), buffer.readVarInt()), (message, supplier) -> {
            var context = supplier.get();
            context.enqueueWork(() -> { if (context.getSender() != null) FactoryActions.perform(context.getSender(), message.pos, message.action); });
            context.setPacketHandled(true);
        }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    public static void open(ServerPlayer player) {
        open(player, 0);
    }
    private static void returnCursor(ServerPlayer player) {
        var menu = player.containerMenu;
        if (menu.getCarried().isEmpty()) return;
        InventoryAccess.returnItem(player, menu.getCarried().copy());
        menu.setCarried(ItemStack.EMPTY);
        menu.broadcastChanges();
    }
    public static void open(ServerPlayer player, int tab) {
        if (!player.isAlive() || player.isSpectator() || tab < 0 || tab > 2) return;
        returnCursor(player);
        if (player.containerMenu instanceof PlayerWorkbenchMenu menu) menu.openTab(tab);
        else player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new PlayerWorkbenchMenu(id, inventory, tab), Component.translatable("menu.conveyance.inventory")));
        // A full inventory may restore a queued cursor on opening. Keep it owned while showing map/research.
        if (tab != 0) returnCursor(player);
    }
    public static void requestOpen() { requestOpen(0); }
    public static void requestOpen(int tab) { CHANNEL.sendToServer(new OpenInventory(tab)); }
    public static void requestHarvest(BlockPos pos) { CHANNEL.sendToServer(new Harvest(pos)); }
    public static void requestCraft(ResourceLocation recipe, boolean bulk) { CHANNEL.sendToServer(new Craft(recipe, bulk ? 0 : 1)); }
    public static void requestFive(ResourceLocation recipe) { CHANNEL.sendToServer(new Craft(recipe, 5)); }
    public static void requestLink(int containerId, int slot, ResourceLocation item) { CHANNEL.sendToServer(new BindLink(containerId, slot, item)); }
    public static void requestAction(BlockPos pos, int action) { CHANNEL.sendToServer(new FactoryAction(pos, action)); }
    public static void syncInventory(ServerPlayer player, List<ItemStack> items, List<ResourceLocation> links) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncInventory(items, links));
    }
    public static void requestAssemblyRecipe(int containerId, ResourceLocation recipe) { CHANNEL.sendToServer(new SelectAssembly(containerId, recipe)); }
    public static void sync(ServerPlayer player) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncResearch.capture(player.level())); }
    static void syncAll(Level level) { CHANNEL.send(PacketDistributor.ALL.noArg(), SyncResearch.capture(level)); }
    private InventoryNetwork() {}
}
