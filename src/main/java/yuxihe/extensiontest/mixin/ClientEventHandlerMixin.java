package yuxihe.extensiontest.mixin;

import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import yuxihe.extensiontest.init.ModSecondSkillKeys;
import yuxihe.extensiontest.skill.SecondSkillConstants;

/**
 * 客户端按键注入：把第二技能按键塞进 SBW 的 keys 位掩码。
 *
 * <p>SBW 的链路是：{@code ClientEventHandler.handleControlVehicle(player, stack)}
 * 逐位拼出 {@code short keys}，keys 变化时由 VehicleMovementMessage 发往服务端，
 * 服务端 {@code processInput(keys)} 解析每一位。我们沿用这条链路，占用此前未使用的
 * 第 9 位（0b1_0000_0000 = 512；SBW 已用第 0~8 位），从而无需新增网络消息。</p>
 *
 * <p>注入点选在 keys 的初始化处（字节码第一个 ISTORE，源码 {@code var keys: Short = 0}）。
 * 关键点：后续每个按键分支都是 {@code keys = keys or X}，是"或"而非"覆盖"，
 * 因此在初始化时置入的位会被一直保留到发包比较，任何键位组合都生效。</p>
 */
@Mixin(ClientEventHandler.class)
public abstract class ClientEventHandlerMixin {

    @ModifyVariable(
            method = "handleControlVehicle",
            at = @At(value = "STORE", opcode = org.objectweb.asm.Opcodes.ISTORE, ordinal = 0),
            remap = false
    )
    private short extensiontest$injectSecondSkillBit(short keys) {
        if (Minecraft.getInstance().player != null && ModSecondSkillKeys.SECOND_SKILL.isDown()) {
            return (short) (keys | SecondSkillConstants.SECOND_SKILL_BIT);
        }
        return keys;
    }
}
