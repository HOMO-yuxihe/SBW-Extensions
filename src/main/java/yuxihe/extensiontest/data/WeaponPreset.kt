package yuxihe.extensiontest.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.logging.LogUtils
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller

data class WeaponPresetEntry(
    val name: String,
    val overrideJson: String?   // Override 序列化后的字符串；null = 默认武器
)

data class VehicleWeaponPresets(
    val vehicleId: String,
    val slots: Map<String, List<WeaponPresetEntry>>
)

object WeaponPresetManager {
    private val presets = mutableMapOf<String, VehicleWeaponPresets>()  // key = vehicleId

    fun clear() = presets.clear()
    fun register(vehicleId: String, data: VehicleWeaponPresets) { presets[vehicleId] = data }
    fun get(vehicleId: String): VehicleWeaponPresets? = presets[vehicleId]

    /** 拿某个载具某个槽位的全部备选 */
    fun getSlot(vehicleId: String, slot: String): List<WeaponPresetEntry>? =
        presets[vehicleId]?.slots?.get(slot)

    fun size() = presets.size
}

class WeaponPresetLoader : SimpleJsonResourceReloadListener(
    Gson(), "weapon_presets"
) {
    companion object {
        private val LOGGER = LogUtils.getLogger()
    }

    override fun apply(
        objectMap: Map<ResourceLocation, JsonElement>,
        resourceManager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        WeaponPresetManager.clear()

        for ((location, element) in objectMap) {
            try {
                val obj = element.asJsonObject
                val vehicleId = obj.get("id")?.asString ?: location.toString()
                LOGGER.debug("尝试读取 {} 的武器配置",vehicleId)
                val weaponsObj = obj.getAsJsonObject("Weapons")
                if(weaponsObj == null) {
                    LOGGER.warn("载具 {} 缺少 Weapons 字段，跳过", vehicleId)
                    continue
                }

                val slots = mutableMapOf<String, List<WeaponPresetEntry>>()

                for ((slotName, slotArray) in weaponsObj.entrySet()) {
                    if (!slotArray.isJsonArray) {
                        LOGGER.warn("载具 {} 的槽位 {} 不是数组，跳过", vehicleId, slotName)
                        continue
                    }

                    val entries = mutableListOf<WeaponPresetEntry>()
                    for (entryElement in slotArray.asJsonArray) {
                        if (!entryElement.isJsonObject) continue
                        val entryObj = entryElement.asJsonObject
                        val name = entryObj.get("name")?.asString ?: continue

                        // Override 缺失 = 默认武器；存在 = 备选武器
                        val overrideJson = entryObj.getAsJsonObject("Override")?.toString()

                        entries.add(WeaponPresetEntry(name, overrideJson))
                    }
                    slots[slotName] = entries
                }

                WeaponPresetManager.register(vehicleId, VehicleWeaponPresets(vehicleId, slots))
            } catch (e: Exception) {
                LOGGER.error("载具武器配置 {} 读取失败，原因：{}", location, e)
            }
        }
        LOGGER.info("读取 {} 个载具的武器备选配置", WeaponPresetManager.size())
    }
}