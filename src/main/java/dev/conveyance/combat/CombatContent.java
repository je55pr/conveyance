package dev.conveyance.combat;

import dev.conveyance.Conveyance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CombatContent {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Conveyance.MOD_ID);
    public static final RegistryObject<EntityType<GrenadeEntity>> GRENADE = ENTITIES.register("grenade", () -> EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC).sized(.25F, .25F).clientTrackingRange(8).updateInterval(1).build("conveyance:grenade"));
    public static final RegistryObject<EntityType<BiterEntity>> BITER = ENTITIES.register("small_biter", () ->
            EntityType.Builder.of(BiterEntity::new, MobCategory.CREATURE).sized(.85F, .65F).clientTrackingRange(8).build("conveyance:small_biter"));
    public static final RegistryObject<EntityType<BiterNestEntity>> NEST = ENTITIES.register("biter_nest", () ->
            EntityType.Builder.of(BiterNestEntity::new, MobCategory.CREATURE).sized(2.6F, 1.3F).clientTrackingRange(10).build("conveyance:biter_nest"));
    public static boolean enemy(net.minecraft.world.entity.Entity entity) { return entity instanceof BiterEntity || entity instanceof BiterNestEntity; }
    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> { event.put(BITER.get(), BiterEntity.attributes().build()); event.put(NEST.get(), BiterNestEntity.attributes().build()); });
        CombatNetwork.register();
    }
    private CombatContent() {}
}
