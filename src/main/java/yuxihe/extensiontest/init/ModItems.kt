package yuxihe.extensiontest.init

import net.minecraft.world.item.Item
import net.minecraft.world.item.Item.Properties
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject
import yuxihe.extensiontest.ExtensionTest

@Suppress("unused")
object ModItems {

    // 与 SBW 原模组一致的两个重载
    private fun registerAmmo(id: String): RegistryObject<Item> = registerAmmo(id) { Item(Properties()) }
    private fun registerAmmo(id: String, ammo: () -> Item): RegistryObject<Item> = AMMO.register(id, ammo)

    @JvmField
    val AMMO: DeferredRegister<Item> = DeferredRegister.create(ForgeRegistries.ITEMS, ExtensionTest.MODID)

    // 对应配置里的 extensiontest:radar_missile（导弹按 SBW 惯例 stacksTo(4)）
    @JvmField
    val RADAR_MISSILE: RegistryObject<Item> = registerAmmo("radar_missile") { Item(Properties().stacksTo(4)) }
}
