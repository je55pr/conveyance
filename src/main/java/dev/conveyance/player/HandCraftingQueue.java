package dev.conveyance.player;

import dev.conveyance.factory.AssemblyRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import com.google.gson.JsonParser;

/** Ingredients and intermediates are real owned stacks, reserved outside storage. */
public final class HandCraftingQueue {
    public static final String DATA_KEY = "ConveyanceHandCrafting";
    public static final int MAX_ORDERS = 64;
    private static final int MAX_OPERATIONS = 256;
    public record OrderView(long id, ItemStack output, int batches, int remainingTicks, int totalTicks) {}
    public record Snapshot(List<OrderView> orders, ItemStack active, int elapsed, int duration, boolean blocked, long time) {}
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static Snapshot client = new Snapshot(List.of(), ItemStack.EMPTY, 0, 1, false, 0);

    private static final class Step {
        final HandCrafting.Operation operation;
        int batches;
        Step(HandCrafting.Operation operation) { this.operation = operation; batches = operation.batches(); }
    }
    private static final class Order {
        long id;
        ItemStack output;
        int batches, totalTicks;
        final List<ItemStack> pool = new ArrayList<>();
        final List<Step> steps = new ArrayList<>();
        int remaining(int current) {
            long total = current;
            for (int i = 0; i < steps.size(); i++) total += (long) steps.get(i).operation.ticks() * (steps.get(i).batches - (i == 0 ? 1 : 0));
            return (int) Math.min(Integer.MAX_VALUE, total);
        }
    }
    private static final class State {
        final List<Order> orders = new ArrayList<>();
        long nextId = 1, syncedAt = Long.MIN_VALUE;
        int remainingTicks;
        ItemStack ready = ItemStack.EMPTY;
        boolean blocked;
        CompoundTag saved;
    }
    private static State state(ServerPlayer player) { return STATES.computeIfAbsent(player, ignored -> load(player.getPersistentData().getCompound(DATA_KEY))); }
    public static boolean enqueue(ServerPlayer player, Container extra, CraftingRecipe recipe, int batches, HandCrafting.QueuePlan plan) {
        State state = state(player);
        if (state.orders.size() >= MAX_ORDERS || plan.operations().isEmpty() || plan.operations().size() > MAX_OPERATIONS) return false;
        var order = new Order();
        order.id = state.nextId++; order.output = recipe.getResultItem(player.level().registryAccess()).copy(); order.batches = batches;
        plan.reserved().forEach(item -> order.pool.add(item.copy()));
        for (var operation : plan.operations()) order.steps.add(new Step(operation));
        order.totalTicks = order.remaining(order.steps.get(0).operation.ticks());
        boolean first = state.orders.isEmpty();
        InventoryAccess.commit(player, extra, plan.remaining());
        state.orders.add(order);
        if (first) state.remainingTicks = order.steps.get(0).operation.ticks();
        save(player, state); sync(player);
        return true;
    }
    public static void tick(ServerPlayer player) {
        State state = state(player);
        if (state.orders.isEmpty() || player.isSpectator()) return;
        Order order = state.orders.get(0); Step step = order.steps.get(0);
        boolean previousBlocked = state.blocked, changed = false;
        if (state.ready.isEmpty()) {
            if (state.remainingTicks > 0) state.remainingTicks--;
            if (state.remainingTicks == 0) {
                var ingredients = new ArrayList<ItemStack>(); order.pool.forEach(item -> ingredients.add(item.copy()));
                for (var requirement : step.operation.ingredients()) {
                    if (HandCrafting.consume(ingredients, requirement.ingredient(), requirement.count()) != 0) {
                        cancel(player, order.id); player.displayClientMessage(Component.translatable("message.conveyance.craft_failed"), true); return;
                    }
                }
                ResearchState.recordCraft(player.level(), step.operation.output());
                order.pool.clear(); ingredients.stream().filter(item -> !item.isEmpty()).forEach(order.pool::add);
                if (step.operation.finished()) state.ready = step.operation.output().copy();
                else { add(order.pool, step.operation.output()); completeBatch(state, order, false); }
                changed = true;
            }
        }
        state.blocked = !state.ready.isEmpty();
        if (!state.ready.isEmpty() && InventoryAccess.give(player, state.ready)) {
            state.ready = ItemStack.EMPTY; state.blocked = false;
            completeBatch(state, order, true); changed = true;
        }
        if (order.steps.isEmpty()) {
            state.orders.remove(0);
            order.pool.forEach(item -> InventoryAccess.returnItem(player, item));
            startHead(state); changed = true;
        }
        if (changed) save(player, state);
        else if (state.saved != null) state.saved.putInt("RemainingTicks", state.remainingTicks);
        if (changed || previousBlocked != state.blocked || state.syncedAt == Long.MIN_VALUE || player.level().getGameTime() - state.syncedAt >= 4) sync(player);
    }
    private static void completeBatch(State state, Order order, boolean finished) {
        if (finished) order.batches--;
        Step step = order.steps.get(0);
        if (--step.batches == 0) order.steps.remove(0);
        state.remainingTicks = order.steps.isEmpty() ? 0 : order.steps.get(0).operation.ticks();
    }
    private static void startHead(State state) {
        state.ready = ItemStack.EMPTY; state.blocked = false;
        state.remainingTicks = state.orders.isEmpty() ? 0 : state.orders.get(0).steps.get(0).operation.ticks();
    }
    private static void add(List<ItemStack> pool, ItemStack offered) {
        ItemStack remaining = offered.copy();
        for (ItemStack item : pool) {
            if (!ItemStack.isSameItemSameTags(item, remaining)) continue;
            int count = Math.min(remaining.getCount(), item.getMaxStackSize() - item.getCount());
            if (count > 0) { item.grow(count); remaining.shrink(count); }
            if (remaining.isEmpty()) return;
        }
        while (!remaining.isEmpty()) pool.add(remaining.split(remaining.getMaxStackSize()));
    }
    public static void cancel(ServerPlayer player, long id) {
        State state = state(player);
        for (int i = 0; i < state.orders.size(); i++) {
            Order order = state.orders.get(i);
            if (order.id != id) continue;
            state.orders.remove(i);
            order.pool.forEach(item -> InventoryAccess.returnItem(player, item));
            if (i == 0) {
                if (!state.ready.isEmpty()) InventoryAccess.returnItem(player, state.ready);
                startHead(state);
            }
            save(player, state); sync(player); return;
        }
    }
    public static void clonePlayer(Player original, Player replacement) {
        replacement.getPersistentData().put(DATA_KEY, original.getPersistentData().getCompound(DATA_KEY).copy());
    }
    public static void sync(ServerPlayer player) {
        State state = state(player);
        long now = player.level().getGameTime(); state.syncedAt = now;
        var views = new ArrayList<OrderView>();
        for (int i = 0; i < state.orders.size(); i++) {
            Order order = state.orders.get(i);
            views.add(new OrderView(order.id, order.output.copy(), order.batches,
                    order.remaining(i == 0 ? state.remainingTicks : order.steps.get(0).operation.ticks()), order.totalTicks));
        }
        Step active = state.orders.isEmpty() ? null : state.orders.get(0).steps.get(0);
        int duration = active == null ? 1 : active.operation.ticks();
        InventoryNetwork.syncCrafting(player, new Snapshot(List.copyOf(views), active == null ? ItemStack.EMPTY : active.operation.output().copy(),
                duration - state.remainingTicks, duration, state.blocked, now));
    }
    private static void save(ServerPlayer player, State state) {
        var data = new CompoundTag(); var orders = new ListTag();
        data.putLong("NextId", state.nextId); data.putInt("RemainingTicks", state.remainingTicks); data.put("Ready", state.ready.save(new CompoundTag()));
        for (Order order : state.orders) {
            var tag = new CompoundTag(); tag.putLong("Id", order.id); tag.put("Output", order.output.save(new CompoundTag()));
            tag.putInt("Batches", order.batches); tag.putInt("TotalTicks", order.totalTicks);
            var pool = new ListTag(); order.pool.forEach(item -> pool.add(item.save(new CompoundTag()))); tag.put("Pool", pool);
            var steps = new ListTag();
            for (Step step : order.steps) {
                var work = new CompoundTag(); var operation = step.operation;
                work.putString("Recipe", operation.recipe().toString()); work.put("Output", operation.output().save(new CompoundTag()));
                work.putInt("Ticks", operation.ticks()); work.putInt("Batches", step.batches); work.putBoolean("Finished", operation.finished());
                var ingredients = new ListTag();
                for (var requirement : operation.ingredients()) {
                    var input = new CompoundTag(); input.putString("Ingredient", requirement.ingredient().toJson().toString()); input.putInt("Count", requirement.count()); ingredients.add(input);
                }
                work.put("Ingredients", ingredients); steps.add(work);
            }
            tag.put("Steps", steps); orders.add(tag);
        }
        data.put("Orders", orders); player.getPersistentData().put(DATA_KEY, data); state.saved = data;
    }
    private static State load(CompoundTag data) {
        var state = new State(); state.saved = data;
        state.nextId = Math.max(1, data.getLong("NextId"));
        var orders = data.getList("Orders", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(MAX_ORDERS, orders.size()); i++) {
            var tag = orders.getCompound(i); var order = new Order();
            order.id = tag.getLong("Id"); order.output = ItemStack.of(tag.getCompound("Output"));
            order.batches = Math.max(1, Math.min(256, tag.getInt("Batches"))); order.totalTicks = Math.max(1, tag.getInt("TotalTicks"));
            var pool = tag.getList("Pool", Tag.TAG_COMPOUND);
            for (int j = 0; j < pool.size(); j++) { ItemStack item = ItemStack.of(pool.getCompound(j)); if (!item.isEmpty()) order.pool.add(item); }
            var steps = tag.getList("Steps", Tag.TAG_COMPOUND);
            for (int j = 0; j < Math.min(MAX_OPERATIONS, steps.size()); j++) {
                var work = steps.getCompound(j); var inputs = new ArrayList<AssemblyRecipes.Requirement>();
                var ingredients = work.getList("Ingredients", Tag.TAG_COMPOUND);
                for (int k = 0; k < ingredients.size(); k++) {
                    var input = ingredients.getCompound(k);
                    inputs.add(new AssemblyRecipes.Requirement(Ingredient.fromJson(JsonParser.parseString(input.getString("Ingredient"))), Math.max(1, input.getInt("Count"))));
                }
                ItemStack output = ItemStack.of(work.getCompound("Output"));
                int ticks = Math.max(1, Math.min(12000, work.getInt("Ticks"))), batches = Math.max(1, work.getInt("Batches"));
                order.steps.add(new Step(new HandCrafting.Operation(ResourceLocation.parse(work.getString("Recipe")), List.copyOf(inputs), output, ticks, batches, work.getBoolean("Finished"))));
            }
            if (!order.output.isEmpty() && !order.steps.isEmpty()) state.orders.add(order);
        }
        if (!state.orders.isEmpty()) {
            state.remainingTicks = Math.max(0, Math.min(state.orders.get(0).steps.get(0).operation.ticks(), data.getInt("RemainingTicks")));
            state.ready = ItemStack.of(data.getCompound("Ready")); state.blocked = !state.ready.isEmpty();
        }
        return state;
    }
    public static Snapshot client() { return client; }
    public static void updateClient(Snapshot snapshot) { client = snapshot; }
    public static void resetClient() { client = new Snapshot(List.of(), ItemStack.EMPTY, 0, 1, false, 0); }
    private HandCraftingQueue() {}
}
