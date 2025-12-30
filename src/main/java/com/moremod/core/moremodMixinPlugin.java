package com.moremod.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import fermiumbooter.FermiumRegistryAPI;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.moremod.core"})
@IFMLLoadingPlugin.Name("moremodCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class moremodMixinPlugin implements IFMLLoadingPlugin {

    /**
     * 检查类是否可用（不触发完整类加载）
     */
    private static boolean isClassAvailable(String className) {
        try {
            // 使用 Class.forName 但不初始化类
            Class.forName(className, false, moremodMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static {
        // FermiumBooter 处理所有 mixin 配置
        try {
            // 先加载 villager mixin
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.villager.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.otherworldly.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.enchant.json");

            // 然后加载其他 mod 的 mixins
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.lycanites.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.parasites.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.champion.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.infernal.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.rs.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.silent.json");
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.element.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.dummy.json");

            // Baubles mixin - 只在 Baubles 可用时注册
            // 注意：Baubles 是必需依赖，但 Mixin 扫描发生在模组加载之前
            // 如果 Baubles 类不可用，跳过此 mixin 以避免 ClassNotFoundException 污染类加载器
            if (isClassAvailable("baubles.api.IBauble")) {
                FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.bauble.json");
                System.out.println("[moremod] Baubles mixin registered");
            } else {
                System.out.println("[moremod] Baubles not found in classpath, skipping bauble mixin");
            }

            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.potioncore.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.ev.json");
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.as.json");

            // Enigmatic Legacy 七圣遗物效果拦截
            FermiumRegistryAPI.enqueueMixin(true, "mixins.moremod.enigmaticlegacy.json");

            // 炼药增强 - 炼药师的术石
            FermiumRegistryAPI.enqueueMixin(false, "mixins.moremod.brewing.json");

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