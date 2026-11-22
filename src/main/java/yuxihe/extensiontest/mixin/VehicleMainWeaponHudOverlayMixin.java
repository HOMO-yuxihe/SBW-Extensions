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
 * 叠层调用点：想在原模组 HUD 上画东西，注入到主武器 HUD 的 render 即可。
 *
 * <p>为什么选这里而不是各具体 HUD（LandVehicleHud / AircraftHud ...）：
 * VehicleMainWeaponHudOverlay.render(RenderContext) 是所有载具 HUD 的统一分发入口，
 * 一处注入即可覆盖全部载具类型；且方法末尾 poseStack 已 popPose，
 * 我们在 TAIL 直接用屏幕绝对坐标绘制，不会污染原模组的 pose 变换。</p>
 *
 * <p>若只想给某一类载具画，可改为注入具体 HUD 的 render，
 * 位置参照 DecoyOverlayHelper 的调用处（驾驶员判定之后）。</p>
 */
@Mixin(VehicleMainWeaponHudOverlay.class)
public abstract class VehicleMainWeaponHudOverlayMixin {

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void extensiontest$renderSecondSkill(RenderContext context, CallbackInfo ci) {
        LocalPlayer player = context.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof VehicleEntity vehicleEntity)) {
            return;
        }
        // 第三人称叠层：绝对坐标；按需要可换 renderFirstPerson 画在第一人称视角。
        SecondSkillOverlayHelper.renderThirdPerson(
                vehicleEntity,
                context.getGuiGraphics(),
                30,
                22,
                -1
        );
    }
}
