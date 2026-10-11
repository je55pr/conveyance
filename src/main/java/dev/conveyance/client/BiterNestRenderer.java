package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.combat.BiterNestEntity;
import dev.conveyance.combat.CombatContent;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BiterNestRenderer extends MobRenderer<BiterNestEntity, BiterNestRenderer.Model> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "biter_nest"), "main");
    public BiterNestRenderer(EntityRendererProvider.Context context) { super(context, new Model(context.bakeLayer(LAYER)), 1.3F); }
    @Override public ResourceLocation getTextureLocation(BiterNestEntity nest) { return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_terracotta.png"); }
    @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(CombatContent.NEST.get(), BiterNestRenderer::new); }
    @SubscribeEvent public static void layer(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, Model::layer); }
    public static final class Model extends HierarchicalModel<BiterNestEntity> {
        private final ModelPart root, sac;
        Model(ModelPart root) { this.root = root; sac = root.getChild("sac"); }
        static LayerDefinition layer() {
            var mesh = new MeshDefinition(); var root = mesh.getRoot();
            root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-19, -4, -16, 38, 4, 32).addBox(-16, -7, -18, 32, 3, 36), PartPose.offset(0, 24, 0));
            root.addOrReplaceChild("sac", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-13, -14, -12, 26, 14, 24).addBox(-9, -19, -8, 18, 5, 16)
                    .addBox(-16, -11, 3, 9, 11, 11).addBox(8, -10, -13, 9, 10, 11), PartPose.offset(0, 24, 0));
            root.addOrReplaceChild("vents", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-11, -19, 8, 5, 12, 5).addBox(6, -21, 5, 5, 13, 5)
                    .addBox(-5, -20, -13, 5, 9, 5), PartPose.offset(0, 24, 0));
            return LayerDefinition.create(mesh, 16, 16);
        }
        @Override public ModelPart root() { return root; }
        @Override public void setupAnim(BiterNestEntity nest, float walk, float amount, float age, float yaw, float pitch) {
            sac.yScale = 1 + (float) Math.sin(age * .06) * .035F;
        }
    }
}
