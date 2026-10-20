package yuxihe.extensiontest.entity.projectile

import com.atsuishio.superbwarfare.entity.projectile.BasicGeoProjectileEntity
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.init.ModSounds
import com.atsuishio.superbwarfare.tools.EntityFindUtil
import com.atsuishio.superbwarfare.tools.RangeTool.calculateFiringSolution
import com.atsuishio.superbwarfare.tools.angleTo
import com.mojang.logging.LogUtils
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.item.Item
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max


open class RadarMissile(type: EntityType<out RadarMissile>, level: Level) :
        MissileProjectile(type, level), BasicGeoProjectileEntity {

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    var searchRange=48.0

    init {
        this.noCulling = true
    }

    override fun tick() {
        super.tick()

        mediumTrail()
        if (level().isClientSide) return

        fun hasLineOfSight(from: Vec3, to: Vec3): Boolean {
            val result = level().clip(
                ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,   // 检测有碰撞箱的方块
                    ClipContext.Fluid.NONE,        // 忽略流体
                    this                           // 射线追踪的执行者
                )
            )
            return result.type == HitResult.Type.MISS  // MISS = 没有阻挡
        }

        if((isLostTarget() || (getTargetUUID() == "none"))){
            val candidates = level().getEntitiesOfClass(
                Entity::class.java,
                boundingBox.inflate(searchRange).move(deltaMovement.scale(8.0))
            ) { entity ->
                entity.isAlive
                        && entity != this
                        && entity != owner                              // 排除发射者
                        && entity != owner?.vehicle                     // 排除发射者所在的载具
                        && !entity.getPassengers().contains(owner)      // 排除载着发射者的实体
                        && deltaMovement.angleTo(entity.boundingBox.center.subtract(boundingBox.center)) <= 45
                        && hasLineOfSight(boundingBox.center,entity.boundingBox.center)
                        && entity !is EnderDragonPart
            }
            val nearest = candidates.minByOrNull { deltaMovement.angleTo(it.boundingBox.center.subtract(boundingBox.center)) }
            if (nearest != null) {
                setTargetUuid(nearest.stringUUID)
                setLostTarget(false)

                LOGGER.debug(Component.literal("锁定目标: ").append(nearest.displayName).append(Component.literal("距离: ${distanceTo(nearest)}")).getString())
            }
        }else{

            val entity=EntityFindUtil.findEntity(level(),getTargetUUID())

            if (entity == null) {
                LOGGER.debug(Component.literal("丢失目标: ${getTargetUUID()}").getString())
                setLostTarget(true)
                setTargetUuid("none")
                return
            }

            if (getTargetUUID() != "none") {
                if (!hasLineOfSight(boundingBox.center,entity.boundingBox.center) or (deltaMovement.angleTo(entity.boundingBox.center.subtract(boundingBox.center)) >= 120)){
                    LOGGER.debug(Component.literal("丢失目标: ").append(entity.displayName).getString())
                    setLostTarget(true)
                    setTargetUuid("none")
                    return
                }

                LOGGER.debug(
                    Component.literal("追踪目标: ").append(entity.displayName)
                        .append(Component.literal("距离: ${distanceTo(entity)}")).getString()
                )

                (entity as? LivingEntity)?.addEffect(MobEffectInstance(MobEffects.GLOWING, 4, 0))

                if ((entity.getPassengers().isNotEmpty() || entity is VehicleEntity)
                    && entity.tickCount % (max(0.04 * this.distanceTo(entity), 2.0).toInt()) == 0
                ) {
                    entity.level().playSound(
                        null,
                        entity.onPos,
                        if (entity is Pig) SoundEvents.PIG_HURT else ModSounds.MISSILE_WARNING.get(),
                        SoundSource.PLAYERS,
                        2f,
                        1f
                    )
                }
                val targetPos = Vec3(
                    entity.x,
                    entity.y + 0.5f * entity.bbHeight + (if (entity is EnderDragon) -3 else 0),
                    entity.z
                )
                val toVec = calculateFiringSolution(
                    boundingBox.center,
                    targetPos,
                    entity.deltaMovement,
                    deltaMovement.length(),
                    0.0
                )
                turn(toVec, 15f)
                this.deltaMovement = this.deltaMovement.scale(0.05).add(lookAngle.scale(6.0))
            }
        }
    }

    override fun getDefaultItem(): Item {
        return ModItems.MEDIUM_ANTI_AIR_MISSILE.get()
    }

    override fun getSound(): SoundEvent {
        return ModSounds.ROCKET_FLY.get()
    }

    override fun getVolume(): Float {
        return 0.4f
    }
}