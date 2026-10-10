package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.MachineBlock;
import dev.conveyance.factory.MachineEntity;
import dev.conveyance.factory.MachineKind;
import dev.conveyance.factory.MinerBlock;
import dev.conveyance.factory.MinerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Casings stay in the chunk mesh; only downward-facing drill rotors render here. */
@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MachineModelRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    public MachineModelRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryContent.MINER_ENTITY.get(), MachineModelRenderer::new);
        event.registerBlockEntityRenderer(FactoryContent.MACHINE_ENTITY.get(), MachineModelRenderer::new);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        for (String name : new String[] {"miner", "burner_miner"}) event.register(model(name));
    }
    @Override public boolean shouldRender(T entity, Vec3 camera) {
        return (entity instanceof MinerEntity || entity instanceof MachineEntity machine && machine.kind() == MachineKind.BURNER_MINER)
                && BlockEntityRenderer.super.shouldRender(entity, camera);
    }
    @Override public void render(T entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (entity instanceof MinerEntity miner) rotor("miner", MinerBlock.WIDTH, entity.getBlockState().getValue(MinerBlock.FACING), miner.drillMotion().angle(partialTick), pose, buffers, light, overlay);
        else if (entity instanceof MachineEntity machine && machine.kind() == MachineKind.BURNER_MINER)
            rotor("burner_miner", machine.kind().width, entity.getBlockState().getValue(MachineBlock.FACING), machine.drillMotion().angle(partialTick), pose, buffers, light, overlay);
    }
    public static void preview(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (state.getBlock() instanceof MinerBlock && state.getValue(MinerBlock.PART) == 0)
            rotor("miner", MinerBlock.WIDTH, state.getValue(MinerBlock.FACING), 0, pose, buffers, light, overlay);
        else if (state.getBlock() instanceof MachineBlock block && block.kind == MachineKind.BURNER_MINER && state.getValue(MachineBlock.PART) == 0)
            rotor("burner_miner", block.kind.width, state.getValue(MachineBlock.FACING), 0, pose, buffers, light, overlay);
    }
    private static ResourceLocation model(String name) { return ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "block/machines/" + name + "/drill"); }
    private static void rotor(String name, int width, Direction facing, float angle, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var game = Minecraft.getInstance();
        float turn = switch (facing) { case EAST -> -90; case SOUTH -> 180; case WEST -> 90; default -> 0; };
        pose.pushPose();
        pose.translate(.5, 0, .5); pose.mulPose(Axis.YP.rotationDegrees(turn)); pose.translate(-.5, 0, -.5);
        pose.translate(width / 2.0, 0, width / 2.0); pose.mulPose(Axis.YP.rotationDegrees(angle)); pose.translate(-.5, 0, -.5);
        game.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.solid()), null,
                game.getModelManager().getModel(model(name)), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
