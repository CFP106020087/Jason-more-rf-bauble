package com.moremod.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.moremod.core"})
@IFMLLoadingPlugin.Name("moremodCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class moremodMixinPlugin implements IFMLLoadingPlugin {

    static {
        // FermiumBooter 处理所有四个 mixin 配置
        try {
            // 先加载 villager mixin
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.villager.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.otherworldly.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.enchant.json");

            // 然后加载其他 mod 的 mixins
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.lycanites.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.parasites.json");
           FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.champion.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.silent.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.element.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.dummy.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.bauble.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.potioncore.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.ev.json");

            // 附魔增强 - 突破附魔等级上限

            // FermiumMixins 兼容 - 绕过假玩家检测
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.fermiummixins.json");

            System.out.println("[moremod] All mixins queued via FermiumBooter");
        } catch (Throwable e) {
            System.err.println("[moremod] FermiumBooter registration failed: " + e);
        }
    }

    @Override
    public String getModContainerClass() {
        // 不再需要在这里添加 mixin 配置
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "com.moremod.core.moremodTransformer" };
    }

    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getAccessTransformerClass() { return null; }
}