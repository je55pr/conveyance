package dev.conveyance.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.conveyance.Conveyance;
import dev.conveyance.factory.FactoryContent;
import dev.conveyance.factory.InserterBlock;
import dev.conveyance.factory.MovingInserter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InserterRenderer<T extends BlockEntity & MovingInserter> implements BlockEntityRenderer<T> {
    private final ItemRenderer items;
    public InserterRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(FactoryContent.INSERTER_ENTITY.get(), InserterRenderer::new);
        event.registerBlockEntityRenderer(FactoryContent.LONG_INSERTER_ENTITY.get(), InserterRenderer::new);
        event.registerBlockEntityRenderer(FactoryContent.BURNER_INSERTER_ENTITY.get(), InserterRenderer::new);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        for (String name : new String[] {"inserter", "long_inserter", "burner_inserter", "fast_inserter"})
            event.register(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "block/" + name + "_arm"));
    }
    @Override public void render(T arm, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        double phase = arm.motion().phase(partialTick);
        BlockState state = arm.getBlockState();
        pose.pushPose();
        transform(state, phase, pose);
        boom(state, pose, buffers, light, overlay);
        var held = arm.heldItem();
        if (!held.isEmpty()) {
            pose.translate(0, -0.14, -((InserterBlock) state.getBlock()).reach());
            pose.scale(0.38F, 0.38F, 0.38F);
            items.renderStatic(held, ItemDisplayContext.GROUND, light, overlay, pose, buffers, arm.getLevel(), 0);
        }
        pose.popPose();
    }
    public static void preview(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); transform(state, 0, pose); boom(state, pose, buffers, light, overlay); pose.popPose();
    }
    private static void transform(BlockState state, double phase, PoseStack pose) {
        double stroke = phase <= 0.5 ? phase * 2 : (1 - phase) * 2;
        double eased = stroke * stroke * (3 - 2 * stroke);
        float yaw = 180 - state.getValue(InserterBlock.FACING).toYRot() + (float) (180 * (1 - eased));
        double lift = Math.sin(Math.PI * eased) * 0.35;
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotation((float) Math.atan2(lift, ((InserterBlock) state.getBlock()).reach())));
    }
    private static void boom(BlockState state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var game = Minecraft.getInstance();
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        var model = game.getModelManager().getModel(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "block/" + name + "_arm"));
        pose.pushPose();
        pose.scale(1, 1, ((InserterBlock) state.getBlock()).reach());
        pose.translate(-0.5, -0.5, -0.5);
        game.getBlockRenderer().getModelRenderer().renderModel(pose.last(), buffers.getBuffer(RenderType.solid()), state,
                model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
        pose.popPose();
    }
}
