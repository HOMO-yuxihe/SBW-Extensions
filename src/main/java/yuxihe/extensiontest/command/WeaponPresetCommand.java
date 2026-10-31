package yuxihe.extensiontest.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import yuxihe.extensiontest.data.WeaponPresetEntry;
import yuxihe.extensiontest.data.WeaponPresetManager;

import java.util.List;

public class WeaponPresetCommand {

    // 注册命令树
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weaponpreset") // 根命令
                .then(Commands.literal("reload") // 子命令 /weaponpreset reload
                        .executes(context -> {
                            // 这里可以调用你的 reload 逻辑，例如通过服务端资源管理器重新加载
                            context.getSource().sendSuccess(
                                    () -> Component.literal("武器预设配置已重新加载！"), false);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
        dispatcher.register(Commands.literal("weaponpreset")
                .then(Commands.literal("weapon")
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.entity()) // 目标载具
                                        .then(Commands.argument("slot", StringArgumentType.word()) // 武器槽位，如 Cannon
                                                .then(Commands.argument("presetIndex", IntegerArgumentType.integer(0)) // 预设索引
                                                        .executes(WeaponPresetCommand::executeSet)
                                                )
                                        )
                                )
                        )
                )
        );
    }
    private static int executeSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            // 1. 拿到目标实体
            Entity target = EntityArgument.getEntity(context, "target");

            // 2. 验证是不是卓越前线的载具
            if (!(target instanceof com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity vehicle)) {
                source.sendFailure(Component.literal("目标不是卓越前线的载具实体"));
                return 0;
            }

            // 3. 拿到参数
            String slot = StringArgumentType.getString(context, "slot");
            int presetIndex = IntegerArgumentType.getInteger(context, "presetIndex");

            // 4. 从管理器查这个载具、这个槽位的备选列表
            String vehicleId = vehicle.getType().builtInRegistryHolder().key().location().toString(); // 如 "superbwarfare:ztz_99a"
            List<WeaponPresetEntry> presets = WeaponPresetManager.INSTANCE.getSlot(vehicleId, slot);

            if (presets == null || presets.isEmpty()) {
                source.sendFailure(Component.literal("载具 " + vehicleId + " 的槽位 " + slot + " 没有注册任何备选武器"));
                return 0;
            }

            if (presetIndex < 0 || presetIndex >= presets.size()) {
                source.sendFailure(Component.literal("预设索引越界，有效范围 0 ~ " + (presets.size() - 1)));
                return 0;
            }

            // 5. 拿到选中的预设，写入 GunData
            WeaponPresetEntry selected = presets.get(presetIndex);
            vehicle.modifyGunData(slot, gunData -> {
                // 直接往 GunData 的 NBT 里写 Override
                CompoundTag tag = gunData.stack.getOrCreateTag();
                if (!tag.contains("GunData")) {
                    tag.put("GunData", new CompoundTag());
                }
                CompoundTag gunDataTag = tag.getCompound("GunData");
                String overrideJson = selected.getOverrideJson();
                if (overrideJson != null) {
                    gunDataTag.putString("Override", overrideJson);
                } else {
                    gunDataTag.remove("Override");
                }
            });

            // 6. 记录当前选中的预设名，供 UI 显示和读档恢复
            vehicle.getPersistentData().putString("Preset_" + slot, selected.getName());

            // 7. 反馈
            source.sendSuccess(() -> Component.literal(
                    "已为载具 " + vehicleId + " 的 " + slot + " 槽位应用预设: " + selected.getName()
            ), true);
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(Component.literal("命令执行失败: " + e.getMessage()));
            return 0;
        }
    }
}
