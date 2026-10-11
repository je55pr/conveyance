package dev.conveyance.factory;

import dev.conveyance.player.ResearchState;
import dev.conveyance.pollution.PollutionData;
import dev.conveyance.pollution.PollutionRates;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ChemicalMachine {
    public static final int CAPACITY = 1000;
    private final MachineEntity owner;
    private FluidTank[] tanks = new FluidTank[5];
    private ResourceLocation selected;
    private boolean creative;
    private double work, flow;
    public enum Status { SELECT_RECIPE, NO_OIL, INPUTS, OUTPUT_FULL, NO_POWER, WORKING }
    private Status status = Status.SELECT_RECIPE;
    ChemicalMachine(MachineEntity owner) { this.owner = owner; rebuildTanks(null); }
    private void rebuildTanks(ChemicalRecipe recipe) {
        FluidKind[] kinds = new FluidKind[5]; java.util.Arrays.fill(kinds, FluidKind.EMPTY);
        if (owner.kind() == MachineKind.PUMPJACK) kinds[4] = FluidKind.CRUDE_OIL;
        if (recipe != null) { for (var cost : recipe.inputs()) kinds[cost.channel()] = cost.fluid(); for (var cost : recipe.outputs()) kinds[cost.channel()] = cost.fluid(); }
        var next = new FluidTank[5];
        for (int i = 0; i < next.length; i++) {
            next[i] = new FluidTank(CAPACITY, kinds[i]);
            if (tanks[i] != null && tanks[i].required == kinds[i]) next[i].store(tanks[i].fluid(), tanks[i].amount());
        }
        tanks = next;
    }
    public FluidTank tank(int channel) { return channel >= 0 && channel < tanks.length ? tanks[channel] : null; }
    public ChemicalRecipe recipe() {
        if (owner.getLevel() == null || selected == null) return null;
        var recipe = owner.getLevel().getRecipeManager().byKey(selected).orElse(null);
        return recipe instanceof ChemicalRecipe chemical && chemical.machine() == owner.kind() ? chemical : null;
    }
    public int recipeIndex() { var recipes = ChemicalRecipe.all(owner.getLevel(), owner.kind()); for (int i = 0; i < recipes.size(); i++) if (recipes.get(i).id().equals(selected)) return i; return -1; }
    public int progress() { var recipe = recipe(); return recipe == null ? 0 : (int) Math.min(1000, 1000 * work / recipe.ticks()); }
    public double flow() { return flow; }
    public Status status() { return status; }
    public boolean select(ResourceLocation id, boolean creative) {
        var found = owner.getLevel().getRecipeManager().byKey(id).orElse(null);
        if (!(found instanceof ChemicalRecipe recipe) || recipe.machine() != owner.kind()
                || !creative && !ResearchState.recipeUnlocked(id, ResearchState.mask(owner.getLevel()))) return false;
        if (id.equals(selected)) return true;
        FluidTank[] old = tanks;
        tanks = new FluidTank[5]; rebuildTanks(recipe);
        FluidTank[] proposed = tanks; tanks = old;
        var candidate = ports(proposed);
        boolean compatible = FluidNetwork.canConnect(owner.getLevel(), candidate);
        if (!compatible) return false;
        FluidNetwork.unregister(owner.getLevel(), owner.getBlockPos());
        selected = id; this.creative = creative; work = 0; rebuildTanks(recipe);
        FluidNetwork.register(owner.getLevel(), owner.getBlockPos()); owner.setChanged();
        owner.getLevel().sendBlockUpdated(owner.getBlockPos(), owner.getBlockState(), owner.getBlockState(), 3);
        return true;
    }
    public List<FluidEndpoint.Port> ports() {
        return ports(tanks);
    }
    private List<FluidEndpoint.Port> ports(FluidTank[] tanks) {
        var ports = new ArrayList<FluidEndpoint.Port>();
        int width = owner.kind().width, depth = owner.kind().depth;
        for (int channel = 0; channel < tanks.length; channel++) {
            FluidTank tank = tanks[channel]; if (tank.required == FluidKind.EMPTY) continue;
            int x = channel < 2 ? owner.kind() == MachineKind.REFINERY ? channel == 0 ? width - 2 : 1 : channel == 0 ? 0 : width - 1
                    : owner.kind() == MachineKind.REFINERY ? channel - 1 : channel == 2 ? 0 : width - 1;
            int z = channel < 2 ? depth - 1 : 0;
            Direction side = channel < 2 ? Direction.SOUTH : Direction.NORTH;
            var facing = owner.getBlockState().getValue(MachineBlock.FACING);
            int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
            for (int i = 0; i < turns; i++) side = side.getClockWise();
            ports.add(new FluidEndpoint.Port(MachineBlock.localPosition(owner.getBlockPos(), facing, x, 0, z), side, channel, tank));
        }
        return List.copyOf(ports);
    }
    public static List<FluidEndpoint.Port> pumpPreview(BlockPos root, Direction facing) {
        return List.of(new FluidEndpoint.Port(MachineBlock.localPosition(root, facing, 2, 0, 0), facing, 4, new FluidTank(CAPACITY, FluidKind.CRUDE_OIL)));
    }
    public boolean deposit() {
        var level = owner.getLevel(); var facing = owner.getBlockState().getValue(MachineBlock.FACING);
        for (int part = 0; part < owner.kind().width * owner.kind().depth; part++)
            if (level.getBlockState(MachineBlock.partPosition(owner.getBlockPos(), facing, owner.kind(), part).below()).is(FactoryContent.OIL_DEPOSIT.get())) return true;
        return false;
    }
    private boolean outputAvailable(ChemicalRecipe recipe) {
        var level = owner.getLevel(); var pos = owner.getBlockPos();
        double factor = 1 + owner.modules.previewBonus(recipe.id().toString(), owner.modules.productivity(true));
        for (var output : recipe.outputs()) if (FluidNetwork.room(level, pos, output.channel(), output.fluid()) + 0.000001 < output.amount() * factor) return false;
        ItemStack stored = owner.getItem(2), result = recipe.result();
        return result.isEmpty() || (stored.isEmpty() || ItemStack.isSameItemSameTags(stored, result)) && stored.getCount() + result.getCount() * factor <= result.getMaxStackSize();
    }
    public boolean hasWork() {
        if (owner.getLevel() == null) return false;
        if (owner.kind() == MachineKind.PUMPJACK) return deposit() && FluidNetwork.room(owner.getLevel(), owner.getBlockPos(), 4, FluidKind.CRUDE_OIL) > 0;
        var recipe = recipe();
        if (recipe == null || !creative && !ResearchState.recipeUnlocked(recipe.id(), ResearchState.mask(owner.getLevel())) || !outputAvailable(recipe)) return false;
        for (int slot = 0; slot < recipe.items().size(); slot++) {
            var input = recipe.items().get(slot); var stored = owner.getItem(slot);
            if (!input.ingredient().test(stored) || stored.getCount() < input.count()) return false;
        }
        for (var input : recipe.inputs()) if (FluidNetwork.amount(owner.getLevel(), owner.getBlockPos(), input.channel()) + 0.000001 < input.amount()) return false;
        return true;
    }
    public boolean accepts(int slot, ItemStack stack) { var recipe = recipe(); return recipe != null && slot < recipe.items().size() && recipe.items().get(slot).ingredient().test(stack); }
    public int inputLimit(int slot) { var recipe = recipe(); return recipe != null && slot < recipe.items().size() ? recipe.items().get(slot).count() * 2 : 0; }
    public void tick() {
        var level = owner.getLevel(); var pos = owner.getBlockPos(); flow = 0;
        if (owner.kind() == MachineKind.PUMPJACK && !deposit()) { status = Status.NO_OIL; return; }
        var recipe = recipe();
        if (owner.kind() != MachineKind.PUMPJACK && recipe == null) { status = Status.SELECT_RECIPE; return; }
        if (!hasWork()) { status = owner.kind() == MachineKind.PUMPJACK || recipe != null && !outputAvailable(recipe) ? Status.OUTPUT_FULL : Status.INPUTS; return; }
        double supplied = PowerNetwork.fraction(level, pos);
        if (supplied <= 0) { status = Status.NO_POWER; return; }
        status = Status.WORKING;
        PollutionData.emit(level, pos, PollutionRates.of(owner.getBlockState().getBlock()) * supplied * owner.modules.pollution());
        if (owner.kind() == MachineKind.PUMPJACK) {
            flow = FluidNetwork.fill(level, pos, 4, FluidKind.CRUDE_OIL, 0.5 * supplied) * 20;
            if (flow > 0) ResearchState.recordProduction(level, "crude_oil", 1);
        } else {
            work += supplied * owner.modules.speed();
            if (work + 0.000001 >= recipe.ticks()) {
                int factor = 1 + owner.modules.produce(recipe.id().toString(), owner.modules.productivity(true));
                for (int slot = 0; slot < recipe.items().size(); slot++) owner.getItem(slot).shrink(recipe.items().get(slot).count());
                for (var input : recipe.inputs()) FluidNetwork.drain(level, pos, input.channel(), input.fluid(), input.amount());
                for (var output : recipe.outputs()) FluidNetwork.fill(level, pos, output.channel(), output.fluid(), output.amount() * factor);
                if (!recipe.result().isEmpty()) {
                    if (owner.getItem(2).isEmpty()) owner.setItem(2, recipe.result().copyWithCount(recipe.result().getCount() * factor)); else owner.getItem(2).grow(recipe.result().getCount() * factor);
                }
                work = Math.max(0, work - recipe.ticks());
            }
        }
        owner.setChanged();
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); if (selected != null) tag.putString("Recipe", selected.toString());
        tag.putDouble("Work", work); tag.putBoolean("Creative", creative);
        for (int i = 0; i < tanks.length; i++) { var tank = tanks[i].save(); tank.putInt("Expected", tanks[i].required.ordinal()); tag.put("Tank" + i, tank); }
        return tag;
    }
    public void load(CompoundTag tag) {
        selected = ResourceLocation.tryParse(tag.getString("Recipe")); creative = tag.getBoolean("Creative");
        double saved = tag.getDouble("Work"); work = Double.isFinite(saved) ? Math.max(0, Math.min(12000, saved)) : 0;
        for (int i = 0; i < tanks.length; i++) {
            var data = tag.getCompound("Tank" + i);
            tanks[i] = new FluidTank(CAPACITY, owner.kind() == MachineKind.PUMPJACK && i == 4 ? FluidKind.CRUDE_OIL : FluidKind.byId(data.getInt("Expected")));
            tanks[i].load(data);
        }
        if (owner.getLevel() != null) FluidNetwork.invalidate(owner.getLevel());
    }
}
