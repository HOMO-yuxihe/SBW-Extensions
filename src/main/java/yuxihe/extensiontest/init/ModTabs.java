package yuxihe.extensiontest.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import yuxihe.extensiontest.ExtensionTest;

@SuppressWarnings("unused")
public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExtensionTest.MODID);

//    public static final RegistryObject<CreativeModeTab> BLOCK_TAB = TABS.register("tom7",
//            () -> CreativeModeTab.builder()
//                    .title(Component.translatable("item_group.extensiontest.tom7"))
//                    .icon(() -> new ItemStack(ModItems.CONTAINER.get()))
//                    .displayItems((param, output) -> output.accept(ContainerBlockItem.createInstance(ModEntities.TOM_7.get())))
//                    .build());
}
