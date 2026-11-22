package yuxihe.extensiontest.client;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import yuxihe.extensiontest.init.ModSecondSkillKeys;
import yuxihe.extensiontest.skill.ISecondSkillVehicle;
import yuxihe.extensiontest.skill.SecondSkillConstants;

/**
 * 第二技能叠层绘制。完全仿照 SBW 的 DecoyOverlayHelper：
 * 传 VehicleEntity + GuiGraphics + 屏幕坐标 + 颜色，绘制一行文字。
 *
 * <p>这里刻意做成静态工具类，方便从 Mixin 注入点直接调用，而不需要 new 对象。</p>
 */
public final class SecondSkillOverlayHelper {

    private SecondSkillOverlayHelper() {}

    /**
     * 第三人称 HUD 用绝对坐标。
     *
     * @param x     屏幕 x
     * @param y     屏幕 y
     * @param color 文字颜色
     */
    public static void renderThirdPerson(VehicleEntity vehicle, GuiGraphics guiGraphics,
                                         int x, int y, int color) {
        if (!isDriver(vehicle)) {
            return;
        }
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                buildText(vehicle),
                x,
                y,
                color,
                false
        );
    }

    /**
     * 第一人称 HUD 用相对坐标：以 -width/2 水平居中。
     *
     * @param y 屏幕 y
     */
    public static void renderFirstPerson(VehicleEntity vehicle, GuiGraphics guiGraphics,
                                         int y, int color) {
        if (!isDriver(vehicle)) {
            return;
        }
        Component text = buildText(vehicle);
        int width = Minecraft.getInstance().font.width(text);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                text,
                -width / 2,
                y,
                color,
                false
        );
    }

    /** 只有炮塔操控者（驾驶员）才显示，与 SBW 原版一致。 */
    private static boolean isDriver(VehicleEntity vehicle) {
        return Minecraft.getInstance().player != null
                && Minecraft.getInstance().player == vehicle.getFirstPassenger();
    }

    private static Component buildText(VehicleEntity vehicle) {
        ISecondSkillVehicle ext = (ISecondSkillVehicle) vehicle;
        int cooldown = ext.extensiontest$getSecondSkillCooldown();
        String keyName = ModSecondSkillKeys.SECOND_SKILL.getKey().getDisplayName().getString();

        if (cooldown > 0) {
            return Component.translatable(
                    "tips.extensiontest.second_skill.reloading",
                    (cooldown / 20.0F),
                    keyName
            );
        }
        String type = vehicle.getPersistentData().getString(SecondSkillConstants.NBT_SKILL_TYPE);
        String nameKey = SecondSkillConstants.SKILL_DEFENSE.equalsIgnoreCase(type)
                ? "tips.extensiontest.second_skill.defense"
                : "tips.extensiontest.second_skill.repair";
        return Component.translatable(nameKey).append(Component.literal(" [" + keyName + "]"));
    }
}
