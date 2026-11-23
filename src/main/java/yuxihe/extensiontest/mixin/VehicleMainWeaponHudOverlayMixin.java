package yuxihe.extensiontest.mixin;

import com.atsuishio.superbwarfare.client.overlay.RenderContext;
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuxihe.extensiontest.client.SecondSkillOverlayHelper;

/**
 * 在 SBW 主武器 HUD 上绘制第二技能提示，一处覆盖全部载具类型。
 *
 * <p>为什么用 {@code @At("RETURN")} 而不是 {@code @At("TAIL")}：
 * 目标方法 render(RenderContext) 内有多个提前 return（源码 95、102、170、195 行），
 * 例如 99A 默认主炮没有制导信息时 seekInfo == null，会在第 195 行直接 popPose(); return。
 * TAIL 只匹配方法体最终返回点，这些路径全部跳过，导致「G 键能用但提示不显示」。
 * RETURN 会注入到每一个返回指令之前，且每个提前返回前都已 popPose()，
 * poseStack 处于干净状态，可直接用绝对坐标绘制。</p>
 *
 * <p>位置策略：第一/第三人称都画在 SBW 诱饵指示的正下方一行——复现诱饵的准星投影锚点、
 * scale(0.75) 与字体，仅把局部 y 下移一个行高。</p>
 */
@Mixin(VehicleMainWeaponHudOverlay.class)
public abstract class VehicleMainWeaponHudOverlayMixin {

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void extensiontest$renderSecondSkill(RenderContext context, CallbackInfo ci) {
        LocalPlayer player = context.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof VehicleEntity vehicleEntity)) {
            return;
        }

        // 第一人称：绝对屏幕坐标 (sw/2-165, sh/2-26)；第三人称：跟随 SBW 诱饵准星锚点。
        SecondSkillOverlayHelper.renderBelowDecoy(
                vehicleEntity,
                context.getGuiGraphics(),
                player,
                context.getPartialTick(),
                context.isFirstPerson(),
                -1,
                context
        );
    }
}
