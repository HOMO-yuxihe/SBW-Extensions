package tech.lq0.extensiontest.init;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {

//    @SubscribeEvent
//    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
//
//        // Register entity render for tom7
//        event.registerEntityRenderer(ModEntities.TOM_7.get(), Tom7Renderer::new);
//    }
}

