package yuxihe.extensiontest.skill;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;

/**
 * 通过 Mixin 注入到 {@link VehicleEntity} 的状态访问接口。
 *
 * <p>扩展类无法直接给 Original 类加字段，标准做法是定义一个接口，
 * 在 Mixin 里 implements 它并用 @Unique 字段实现读写，
 * 业务代码通过 {@code ((ISecondSkillVehicle) vehicle)} 强制转换访问。</p>
 */
public interface ISecondSkillVehicle {

    /** 服务端 baseTick 中消费的按键状态（按下瞬间为 true，执行后置 false）。 */
    boolean extensiontest$getSecondSkillInput();

    void extensiontest$setSecondSkillInput(boolean value);

    /** 冷却剩余 tick，通过同步 EntityData 让客户端 HUD 可见。 */
    int extensiontest$getSecondSkillCooldown();

    void extensiontest$setSecondSkillCooldown(int ticks);
}
