package dev.conveyance.client;

import dev.conveyance.Conveyance;
import dev.conveyance.combat.BiterEntity;
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
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Conveyance.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BiterRenderer extends MobRenderer<BiterEntity, BiterRenderer.Model> {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Conveyance.MOD_ID, "small_biter"), "main");
    public BiterRenderer(EntityRendererProvider.Context context) { super(context, new Model(context.bakeLayer(LAYER)), .45F); }
    @Override public ResourceLocation getTextureLocation(BiterEntity enemy) { return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/brown_terracotta.png"); }
    @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(CombatContent.BITER.get(), BiterRenderer::new); event.registerEntityRenderer(CombatContent.GRENADE.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new); }
    @SubscribeEvent public static void layer(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, Model::layer); }
    public static final class Model extends HierarchicalModel<BiterEntity> {
        private final ModelPart root, head;
        private final ModelPart[] legs = new ModelPart[6];
        Model(ModelPart root) { this.root = root; head = root.getChild("head"); for (int i = 0; i < 6; i++) legs[i] = root.getChild("leg" + i); }
        static LayerDefinition layer() {
            var mesh = new MeshDefinition(); var root = mesh.getRoot();
            root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -5, -1, 8, 6, 10).addBox(-3, -7, 1, 6, 2, 6), PartPose.offset(0, 19, 0));
            root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -5, 6, 5, 5).addBox(-4, 0, -8, 2, 2, 4).addBox(2, 0, -8, 2, 2, 4), PartPose.offset(0, 19, -1));
            for (int i = 0; i < 6; i++) {
                boolean left = i % 2 == 0;
                root.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(0, 0).addBox(left ? -7 : 0, 0, -1, 7, 2, 2)
                        .addBox(left ? -8 : 6, 1, -1, 2, 5, 2), PartPose.offset(left ? -2 : 2, 18, -1 + i / 2 * 4));
            }
            return LayerDefinition.create(mesh, 16, 16);
        }
        @Override public ModelPart root() { return root; }
        @Override public void setupAnim(BiterEntity enemy, float walk, float amount, float age, float yaw, float pitch) {
            head.yRot = yaw * .008F; head.xRot = pitch * .008F;
            for (int i = 0; i < legs.length; i++) { float phase = walk * 1.4F + (i % 2 == 0 ? 0 : Mth.PI) + i / 2 * 1.8F; legs[i].yRot = Mth.cos(phase) * amount * .6F; legs[i].zRot = Mth.sin(phase) * amount * .18F; }
        }
    }
}
