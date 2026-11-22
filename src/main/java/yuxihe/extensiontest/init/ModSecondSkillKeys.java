package yuxihe.extensiontest.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import yuxihe.extensiontest.ExtensionTest;

/**
 * 第二技能按键。仿照 SBW ModKeyMappings 的写法：object 里集中持有 KeyMapping，
 * 在 RegisterKeyMappingsEvent 里统一注册。
 */
@Mod.EventBusSubscriber(modid = ExtensionTest.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModSecondSkillKeys {

    /** 默认 G 键，与 SBW 现有按键（V/C/X/K/J/M）不冲突。 */
    public static final KeyMapping SECOND_SKILL = new KeyMapping(
            "key.extensiontest.second_skill",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.extensiontest"
    );

    private ModSecondSkillKeys() {}

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SECOND_SKILL);
    }
}
