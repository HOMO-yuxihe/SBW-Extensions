package yuxihe.extensiontest.mixin;

import com.atsuishio.superbwarfare.entity.projectile.SmokeDecoyEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuxihe.extensiontest.skill.ISecondSkillVehicle;
import yuxihe.extensiontest.skill.SecondSkillConstants;

/**
 * 载具端核心：模拟 SBW 的 hasDecoy()/releaseDecoy() 链路，把第二技能接进
 * processInput -> baseTick 的调用链。
 *
 * <p>调用链（与 SBW 诱饵机制完全同构）：</p>
 * <pre>
 * 客户端 KeyMapping(G)
 *   -> ClientEventHandlerMixin 把第 9 位写入 keys
 *   -> SBW 自带 VehicleMovementMessage 发往服务端（驾驶员校验也在其中）
 *   -> VehicleEntity.processInput(keys)  [本类 @Inject 解析 bit]
 *   -> VehicleEntity.baseTick()          [本类 @Inject：上升沿 + 冷却 + 执行]
 *   -> 修理 / 主动防御烟幕
 *   -> 同步 EntityData 冷却给客户端 HUD
 * </pre>
 */
@Mixin(VehicleEntity.class)
public abstract class VehicleEntityMixin implements ISecondSkillVehicle {

    /** 冷却的同步数据；SBW 用同样方式同步 DecoyReady，这里照做。 */
    @Unique
    private static final EntityDataAccessor<Integer> DATA_SECOND_SKILL_CD =
            SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    /** 按键下降/上升沿检测用的上一帧状态，仅服务端使用。 */
    @Unique
    private boolean extensiontest$secondSkillInput = false;

    @Unique
    private boolean extensiontest$secondSkillPrevInput = false;

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void extensiontest$defineSyncedData(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        self.getEntityData().define(DATA_SECOND_SKILL_CD, 0);
    }

    // 使用修理技能后修理时长内每刻触发
    private void repairTick(VehicleEntity vehicle){
        vehicle.setHealth(Mth.clamp(
                vehicle.getHealth() + SecondSkillConstants.REPAIR_AMOUNT,
                0.0F,
                vehicle.getMaxHealth()));
    }

    // ---------------------------------------------------------------------
    // 1) 解析 keys 位：等价于 SBW processInput 里 setDecoyInputDown(...)
    // ---------------------------------------------------------------------
    @Inject(method = "processInput", at = @At("TAIL"), remap = false)
    private void extensiontest$onProcessInput(short keys, CallbackInfo ci) {
        boolean down = (keys & SecondSkillConstants.SECOND_SKILL_BIT) != 0;
        this.extensiontest$setSecondSkillInput(down);
    }

    // ---------------------------------------------------------------------
    // 2) tick 中处理：上升沿 + 冷却 + 执行，等价于 hasDecoy()/releaseDecoy() 分支
    // ---------------------------------------------------------------------
    @Inject(method = "baseTick", at = @At("TAIL"))
    private void extensiontest$onBaseTick(CallbackInfo ci) {
        Entity selfEntity = (Entity) (Object) this;
        VehicleEntity self = (VehicleEntity) (Object) this;

        // 只在服务端执行技能（权威侧），客户端仍会更新冷却显示用的同步值。
        if (selfEntity.level().isClientSide) {
            return;
        }

        // 冷却递减
        int cooldown = this.extensiontest$getSecondSkillCooldown();
        if (cooldown > 0) {
            this.extensiontest$setSecondSkillCooldown(cooldown - 1);

            // 按下修理后一段时间内持续修理
            if(cooldown >= SecondSkillConstants.COOLDOWN_TICKS - SecondSkillConstants.REPAIR_DURATION)repairTick(self);
        }

        boolean down = this.extensiontest$getSecondSkillInput();
        boolean justPressed = down && !this.extensiontest$secondSkillPrevInput;
        this.extensiontest$secondSkillPrevInput = down;

        if (justPressed && this.extensiontest$getSecondSkillCooldown() <= 0) {
            this.extensiontest$executeSecondSkill(self);
            this.extensiontest$setSecondSkillCooldown(SecondSkillConstants.COOLDOWN_TICKS);
        }

        // 消费按键状态，避免长按连续触发（SBW releaseSmokeDecoy 会把 decoyInputDown 置回 false）
        if (down) {
            this.extensiontest$setSecondSkillInput(false);
        }
    }

    // ---------------------------------------------------------------------
    // 3) 技能执行体：按 NBT SecondSkillType 分派
    // ---------------------------------------------------------------------
    @Unique
    private void extensiontest$executeSecondSkill(VehicleEntity vehicle) {
        String type = vehicle.getPersistentData().getString(SecondSkillConstants.NBT_SKILL_TYPE);
        if (SecondSkillConstants.SKILL_DEFENSE.equalsIgnoreCase(type)) {
            extensiontest$activeDefense(vehicle);
        } else {
            extensiontest$repair(vehicle);
        }
    }

    /** 修理：直接回血，钳制到最大生命值。 */
    @Unique
    private void extensiontest$repair(VehicleEntity vehicle) {
        //恢复车体血量
        vehicle.setHealth(Mth.clamp(
                vehicle.getHealth() + SecondSkillConstants.FIRST_REPAIR_AMOUNT,
                0.0F,
                vehicle.getMaxHealth()));

        //彻底修理所有模块
        vehicle.setTurretHealth(SecondSkillConstants.COMPONENT_MAX_HEALTH);
        vehicle.setMainEngineHealth(SecondSkillConstants.COMPONENT_MAX_HEALTH);
        vehicle.setSubEngineHealth(SecondSkillConstants.COMPONENT_MAX_HEALTH);
        vehicle.setLeftWheelHealth(SecondSkillConstants.COMPONENT_MAX_HEALTH);
        vehicle.setRightWheelHealth(SecondSkillConstants.COMPONENT_MAX_HEALTH);
    }

    /**
     * 主动防御：复用原模组的 {@link SmokeDecoyEntity}，生成一圈烟幕。
     * 这与 SBW 的 VehicleWeaponUtils.releaseSmokeDecoy 逻辑一致。
     */
    @Unique
    private void extensiontest$activeDefense(VehicleEntity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 base = vehicle.getTurretVector(1.0F);
        for (int i = 0; i < 8; i++) {
            SmokeDecoyEntity smoke = new SmokeDecoyEntity(level);
            smoke.setPos(vehicle.getX(), vehicle.getY() + vehicle.getBbHeight(), vehicle.getZ());
            smoke.decoyShoot(vehicle, base.yRot((-78.75F + 22.5F * i) * Mth.DEG_TO_RAD), 4.0F, 8.0F);
            smoke.setDeltaMovement(smoke.getDeltaMovement().add(vehicle.getDeltaMovement()));
            level.addFreshEntity(smoke);
        }
    }

    // ---------------------------------------------------------------------
    // ISecondSkillVehicle 实现
    // ---------------------------------------------------------------------
    @Override
    public boolean extensiontest$getSecondSkillInput() {
        return this.extensiontest$secondSkillInput;
    }

    @Override
    public void extensiontest$setSecondSkillInput(boolean value) {
        this.extensiontest$secondSkillInput = value;
    }

    @Override
    public int extensiontest$getSecondSkillCooldown() {
        return ((Entity) (Object) this).getEntityData().get(DATA_SECOND_SKILL_CD);
    }

    @Override
    public void extensiontest$setSecondSkillCooldown(int ticks) {
        ((Entity) (Object) this).getEntityData().set(DATA_SECOND_SKILL_CD, ticks);
    }
}
