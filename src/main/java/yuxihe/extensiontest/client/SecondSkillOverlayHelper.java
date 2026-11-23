package yuxihe.extensiontest.client;

import com.atsuishio.superbwarfare.client.overlay.OverlayTraceHandler;
import com.atsuishio.superbwarfare.client.overlay.RenderContext;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.VectorToolKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

    /** SBW 各 HUD 绘制诱饵时统一的缩放系数（Land/Heli/Air/Kirov/OldAir 均为 0.75）。 */
    private static final float DECOY_SCALE = 0.75F;
    /** 诱饵文字绘制的局部 y；下一行 = 局部 y + 字体行高。 */
    private static final int DECOY_LOCAL_Y = 1;

    /**
     * 复现 SBW 准星射线落点的屏幕投影 p，与 VehicleCrosshairOverlay 的准星锚点完全同源。
     */
    private static Vec3 crosshairScreenPos(VehicleEntity vehicle, Entity player, float partialTick) {
        Vec3 shootPos = vehicle.getShootPosForHud(player, partialTick);
        Vec3 shootDir = vehicle.getShootDirectionForHud(player, partialTick);

        BlockHitResult blockResult = OverlayTraceHandler.blockMaxRangeResult;
        Vec3 hitPos = blockResult != null ? blockResult.getLocation() : shootPos;
        double distance = shootPos.distanceTo(hitPos);

        Entity looking = vehicle.getPlayerLookAtEntityOnVehicle(player, 512.0, partialTick);
        if (looking != null) {
            distance = shootPos.distanceTo(looking.position());
        }

        Vec3 aimPos = shootPos.add(shootDir.scale(distance));
        return VectorToolKt.worldToScreen(aimPos);
    }

    /**
     * 把第二技能提示画在 SBW 诱饵指示的正下方一行，字体大小与诱饵完全一致。
     *
     * <p>SBW 的诱饵文字并非画在绝对坐标，而是在各 HUD 内
     * {@code translate(p.x, p.y[+50]) + scale(0.75)} 之后用局部坐标绘制的：
     * <ul>
     *   <li>第三人称（VehicleCrosshairOverlay）：translate(p.x, p.y)，诱饵局部 (30, 1)</li>
     *   <li>第一人称（Aircraft/HelicopterHud）：translate(p.x, p.y + 50)，诱饵局部 (-length/2, 1)</li>
     * </ul>
     * 本叠层注入在主 HUD 的 RETURN（poseStack 已 pop），因此这里手动重建相同的变换：
     * 同样的 font、同样的 scale(0.75)、同样的 x 基准，y 取诱饵局部 y + 一个行高，
     * 于是字号、横向对齐、行距都与 SBW 诱饵一致。</p>
     *
     * @param vehicle      载具
     * @param guiGraphics  GuiGraphics（绝对坐标系）
     * @param player       本地玩家
     * @param partialTick  frame delta
     * @param firstPerson  是否第一人称
     * @param color        文字颜色
     * @param context      渲染上下文，用于取屏幕宽高（第一人称绝对定位）
     */
    public static void renderBelowDecoy(VehicleEntity vehicle, GuiGraphics guiGraphics,
                                        Entity player, float partialTick, boolean firstPerson,
                                        int color, RenderContext context) {
        if (!isDriver(vehicle)) {
            return;
        }

        Component text = buildText(vehicle);
        var font = Minecraft.getInstance().font;
        var h = font.lineHeight;

        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        if (firstPerson) {
            // 第一人称：按屏幕尺寸的绝对坐标绘制，不做 translate/scale。
            // drawString 的 x/y 直接就是屏幕像素坐标。
            int sw = context.getScreenWidth();
            int sh = context.getScreenHeight();
            guiGraphics.drawString(font, text, sw / 2 - 165, sh / 2 - 26, color, false);
        } else {
            // 第三人称：仍跟随 SBW 诱饵准星锚点 + scale(0.75)，左对齐 (30, 1+h)
            Vec3 p = crosshairScreenPos(vehicle, player, partialTick);
            poseStack.translate((float) p.x, (float) p.y, 0F);
            poseStack.scale(DECOY_SCALE, DECOY_SCALE, 1F);
            guiGraphics.drawString(font, text, 30, DECOY_LOCAL_Y + h, color, false);
        }
        poseStack.popPose();
    }

    /**
     * 第一人称：SBW 的诱饵提示在屏幕中心偏下、水平居中绘制。
     * 由于本叠层注入在主 HUD 的 RETURN（poseStack 已 pop，绝对坐标系），
     * 这里用 screenWidth 在绝对坐标里做等价的水平居中，不能沿用相对坐标 -width/2。
     *
     * @param screenWidth 屏幕宽度，用于水平居中
     * @param y           屏幕绝对 y
     */
    public static void renderFirstPersonCentered(VehicleEntity vehicle, GuiGraphics guiGraphics,
                                                 int screenWidth, int y, int color) {
        if (!isDriver(vehicle)) {
            return;
        }
        Component text = buildText(vehicle);
        int width = Minecraft.getInstance().font.width(text);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                text,
                (screenWidth - width) / 2,
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
