package tech.lq0.extensiontest.init;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import tech.lq0.extensiontest.ExtensionTest;
import tech.lq0.extensiontest.entity.projectile.RadarMissile;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ExtensionTest.MODID);

    // Register Tom7
    public static final RegistryObject<EntityType<RadarMissile>> RADAR_MISSILE =
            REGISTRY.register("radar_missile", () ->
                    EntityType.Builder.<RadarMissile>of(
                                    (type, level) -> new RadarMissile(type, level),
                                    MobCategory.MISC
                            )
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("radar_missile")
            );

    private static <T extends Entity> RegistryObject<EntityType<T>> register(String name, EntityType.Builder<T> entityTypeBuilder) {
        return REGISTRY.register(name, () -> entityTypeBuilder.build(name));
    }
}
