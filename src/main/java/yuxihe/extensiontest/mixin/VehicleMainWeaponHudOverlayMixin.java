package yuxihe.extensiontest.mixin;

import com.atsuishio.superbwarfare.client.overlay.RenderContext;
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.atsuishio.superbwarfare.client.overlay.weapon.AircraftHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.ArtilleryHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.KirovHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.LandVehicleHud;
import com.atsuishio.superbwarfare.client.overlay.weapon.OldAircraftHud;
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

        int sw = context.getScreenWidth();
        int sh = context.getScreenHeight();
        String hudType = vehicleEntity.computed().getHudType();

        // 与 SBW 各武器 HUD 里 DecoyOverlayHelper 的调用坐标一致，y 再下移一行（行高 9 + 2 间距）。
        int x;
        int y;
        if (LandVehicleHud.ID.equals(hudType)) {            // 99A 等地面载具
            x = sw / 2 - 165;
            y = sh / 2 - 25;
        } else if (HelicopterHud.ID.equals(hudType)) {
            x = sw / 2 - 160;
            y = sh / 2 - 39;
        } else if (AircraftHud.ID.equals(hudType)) {
            x = 72;
            y = 11;
        } else if (KirovHud.ID.equals(hudType) || OldAircraftHud.ID.equals(hudType)) {
            x = 25;
            y = 12;
        } else {
            // ArtilleryHud 本身没有诱饵提示，沿用地面载具坐标作为兜底。
            x = sw / 2 - 165;
            y = sh / 2 - 25;
        }

        SecondSkillOverlayHelper.renderThirdPerson(
                vehicleEntity,
                context.getGuiGraphics(),
                x,
                y,
                -1
        );
    }
}
