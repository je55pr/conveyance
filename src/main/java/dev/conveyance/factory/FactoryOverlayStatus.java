package dev.conveyance.factory;

import dev.conveyance.player.ResearchState;
import dev.conveyance.player.Technology;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/** Read-only inspection of the same server state used by actual machine ticks. */
public final class FactoryOverlayStatus {
    public enum Kind { MACHINE, POLE, INSERTER }
    public enum Status {
        WORKING(false), IDLE(false), NO_COVERAGE(true), NO_POWER(true), LOW_POWER(true), NO_FUEL(true),
        NO_RECIPE(true), INPUTS(false), OUTPUT_FULL(false), NO_RESOURCE(true), NO_WATER(true), NO_STEAM(true), NO_AMMO(true), CHARGING(false);
        public final boolean alert;
        Status(boolean alert) { this.alert = alert; }
        public Component label() { return Component.translatable("overlay.conveyance.status." + name().toLowerCase(java.util.Locale.ROOT)); }
    }
    public record Entry(BlockPos root, Kind kind, int width, int depth, int height, Direction facing,
                        int reach, ItemStack building, ItemStack product, Component operation, Status status, float power) {
        public Vec3 centre(double y) {
            double x = (width - 1) / 2.0, z = (depth - 1) / 2.0;
            return Vec3.atCenterOf(root).add(switch (facing) { case EAST -> -z; case SOUTH -> -x; case WEST -> z; default -> x; },
                    y - .5, switch (facing) { case EAST -> x; case SOUTH -> -z; case WEST -> -x; default -> z; });
        }
        public Vec3 roof() { return centre(height + .2); }
    }
    private static ItemStack icon(ItemStack stack) { return stack.isEmpty() ? ItemStack.EMPTY : new ItemStack(stack.getItem()); }
    private static Entry entry(BlockEntity entity, Kind kind, int width, int depth, int height, Direction facing, int reach,
                               ItemStack product, Component operation, Status status) {
        return new Entry(entity.getBlockPos(), kind, width, depth, height, facing, reach, new ItemStack(entity.getBlockState().getBlock()),
                icon(product), operation, status, (float) PowerNetwork.satisfaction(entity.getLevel(), entity.getBlockPos()));
    }
    private static Status power(PoweredMachineEntity machine) {
        if (!PowerNetwork.connected(machine.getLevel(), machine.getBlockPos())) return Status.NO_COVERAGE;
        double fraction = PowerNetwork.satisfaction(machine.getLevel(), machine.getBlockPos());
        return fraction <= .000001 ? Status.NO_POWER : fraction < .999 ? Status.LOW_POWER : null;
    }
    public static Entry capture(BlockEntity entity) {
        if (entity instanceof PowerPoleEntity) return entry(entity, Kind.POLE, 1, 1, PowerPoleBlock.HEIGHT, Direction.NORTH, 0,
                ItemStack.EMPTY, Component.empty(), Status.IDLE);
        if (entity instanceof MinerEntity miner) {
            var facing = entity.getBlockState().getValue(MinerBlock.FACING);
            var resource = MiningResources.find(entity.getLevel(), MinerBlock.FOUNDATION, 0, part -> MinerBlock.partPosition(entity.getBlockPos(), facing, part).below());
            Status status = power(miner);
            if (status == null) status = resource.stack().isEmpty() ? Status.NO_RESOURCE
                    : !ItemTransfer.canOutput(entity.getLevel(), entity.getBlockPos().relative(facing), facing.getOpposite(), resource.stack()) ? Status.OUTPUT_FULL : Status.WORKING;
            return entry(entity, Kind.MACHINE, miner.footprintWidth(), miner.footprintWidth(), MinerBlock.HEIGHT, facing, 0,
                    resource.stack(), resource.stack().getHoverName(), status);
        }
        if (entity instanceof InserterEntity arm || entity instanceof BurnerInserterEntity) {
            var block = (InserterBlock) entity.getBlockState().getBlock(); var facing = entity.getBlockState().getValue(InserterBlock.FACING);
            Status status = entity instanceof InserterEntity electric ? power(electric) : null;
            ItemStack held = entity instanceof InserterEntity electric ? electric.heldItem() : ((BurnerInserterEntity) entity).heldItem();
            if (status == null && entity instanceof BurnerInserterEntity burner && burner.fuelReserveMJ() <= 0 && FuelEnergy.mj(burner.getItem(0)) <= 0) status = Status.NO_FUEL;
            if (status == null) status = !held.isEmpty() && !ItemTransfer.canInserterOutput(entity.getLevel(), entity.getBlockPos().relative(facing, block.reach()), facing.getOpposite(), held)
                    ? Status.OUTPUT_FULL : held.isEmpty() ? Status.IDLE : Status.WORKING;
            return entry(entity, Kind.INSERTER, 1, 1, 1, facing, block.reach(), ItemStack.EMPTY, Component.empty(), status);
        }
        if (entity instanceof RadarEntity radar) {
            Status status = power(radar);
            return entry(entity, Kind.MACHINE, RadarBlock.WIDTH, RadarBlock.WIDTH, RadarBlock.HEIGHT, entity.getBlockState().getValue(RadarBlock.FACING), 0,
                    ItemStack.EMPTY, Component.empty(), status == null ? Status.WORKING : status);
        }
        if (!(entity instanceof MachineEntity machine)) return null;
        var kind = machine.kind(); var level = machine.getLevel(); var facing = machine.getBlockState().getValue(MachineBlock.FACING);
        ItemStack product = ItemStack.EMPTY; Component operation = Component.empty(); Status status = Status.IDLE;
        if (kind == MachineKind.BURNER_MINER) {
            var resource = MiningResources.find(level, 4, 0, part -> MachineBlock.partPosition(machine.getBlockPos(), facing, kind, part).below());
            product = resource.stack();
            status = switch (machine.miningStatus()) {
                case MINING -> Status.WORKING; case NO_DEPOSIT -> Status.NO_RESOURCE; case NO_FUEL -> Status.NO_FUEL;
                case NO_OUTPUT, OUTPUT_BLOCKED -> Status.OUTPUT_FULL;
            };
        } else if (kind.furnace()) {
            var recipe = IndustrialSmelting.find(level, machine.getItem(0));
            product = recipe != null ? recipe.getResultItem(level.registryAccess()) : machine.getItem(machine.outputSlot());
            status = machine.fuelReserveMJ() <= 0 && FuelEnergy.mj(machine.getItem(1)) <= 0 ? Status.NO_FUEL
                    : recipe == null ? Status.INPUTS : machine.getItem(0).getCount() < IndustrialSmelting.inputCount(recipe) ? Status.INPUTS
                    : fits(machine, product) ? Status.WORKING : Status.OUTPUT_FULL;
        } else if (kind.assembler()) {
            var recipe = machine.selectedRecipe();
            if (recipe == null) status = Status.NO_RECIPE;
            else {
                product = recipe.getResultItem(level.registryAccess()); status = Status.WORKING;
                var costs = AssemblyRecipes.requirements(recipe);
                for (int slot = 0; slot < costs.size(); slot++) if (!costs.get(slot).ingredient().test(machine.getItem(slot)) || machine.getItem(slot).getCount() < costs.get(slot).count()) { status = Status.INPUTS; break; }
                if (status == Status.WORKING && !machine.hasWork()) status = Status.OUTPUT_FULL;
            }
        } else if (kind == MachineKind.LAB) {
            var research = Technology.byId(ResearchState.selected(level));
            if (research != null) {
                product = research.icon(); operation = research.title(); status = Status.WORKING;
                for (int slot = 0; slot < Technology.SCIENCE_SLOTS; slot++) if (research.needsPack(slot) && !machine.getItem(slot).is(Technology.packItem(slot))) status = Status.INPUTS;
            }
        } else if (kind.chemical()) {
            var recipe = machine.chemical().recipe();
            if (recipe != null) { product = recipe.icon(); operation = Component.translatable("recipe.conveyance." + recipe.id().getPath()); }
            status = switch (machine.chemical().status()) {
                case SELECT_RECIPE -> kind == MachineKind.PUMPJACK ? Status.IDLE : Status.NO_RECIPE;
                case NO_OIL -> Status.NO_RESOURCE; case INPUTS -> Status.INPUTS; case OUTPUT_FULL -> Status.OUTPUT_FULL;
                case NO_POWER -> Status.NO_POWER; case WORKING -> Status.WORKING;
            };
        } else if (kind == MachineKind.BOILER || kind == MachineKind.STEAM_ENGINE || kind == MachineKind.OFFSHORE_PUMP) {
            if (kind == MachineKind.OFFSHORE_PUMP) { product = new ItemStack(Items.WATER_BUCKET); operation = FluidKind.WATER.title(); }
            status = switch (machine.steam().status()) {
                case NO_WATER -> Status.NO_WATER; case NO_FUEL -> Status.NO_FUEL; case NO_STEAM -> Status.NO_STEAM;
                case NO_COVERAGE -> Status.NO_COVERAGE; case OUTPUT_FULL -> Status.OUTPUT_FULL; case WORKING -> Status.WORKING; case IDLE -> Status.IDLE;
            };
        } else if (kind == MachineKind.GUN_TURRET) status = machine.turret().rounds() == 0 && !dev.conveyance.combat.CombatShots.magazine(machine.getItem(0)) ? Status.NO_AMMO : Status.IDLE;
        else if (kind == MachineKind.LASER_TURRET) status = switch (machine.laser().status()) {
            case NO_COVERAGE -> Status.NO_COVERAGE; case NO_POWER -> Status.NO_POWER; case CHARGING -> Status.CHARGING;
            case READY -> Status.IDLE; case FIRING -> Status.WORKING;
        };
        if (kind.powerKW > 0 || kind.idleKW() > 0) { Status electrical = power(machine); if (electrical != null) status = electrical; }
        if (operation.getString().isEmpty() && !product.isEmpty()) operation = product.getHoverName();
        return entry(machine, Kind.MACHINE, kind.width, kind.depth, kind.height, facing, 0, product, operation, status);
    }
    private static boolean fits(MachineEntity machine, ItemStack result) {
        if (result.isEmpty()) return true;
        ItemStack stored = machine.getItem(machine.outputSlot());
        return (stored.isEmpty() || ItemStack.isSameItemSameTags(stored, result)) && stored.getCount() + result.getCount() <= result.getMaxStackSize();
    }
    private FactoryOverlayStatus() {}
}
