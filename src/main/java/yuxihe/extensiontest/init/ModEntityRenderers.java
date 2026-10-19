package yuxihe.extensiontest.init;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.RADAR_MISSILE.get(),
                BasicProjectileRenderer::new   // 对应 Kotlin 的 ::BasicProjectileRenderer
        );
    }
}

