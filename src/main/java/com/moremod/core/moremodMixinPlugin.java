package com.moremod.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"com.moremod.core"})
@IFMLLoadingPlugin.Name("moremodCore")
@IFMLLoadingPlugin.SortingIndex(1005)
public class moremodMixinPlugin implements IFMLLoadingPlugin {

    static {
        // 使用标准Mixin加载方式 (不依赖FermiumBooter)
        try {
            MixinBootstrap.init();

            // 核心Mixin配置 (已解耦外部MOD依赖)
            Mixins.addConfiguration("mixins.moremod.villager.json");
            Mixins.addConfiguration("mixins.moremod.otherworldly.json");
            Mixins.addConfiguration("mixins.moremod.enchant.json");
            Mixins.addConfiguration("mixins.moremod.silent.json");
            Mixins.addConfiguration("mixins.moremod.element.json");
            Mixins.addConfiguration("mixins.moremod.dummy.json");
            Mixins.addConfiguration("mixins.moremod.bauble.json");
            Mixins.addConfiguration("mixins.moremod.enigmaticlegacy.json");

            // 已移除的外部MOD Mixin配置 (Phase 1 解耦):
            // - mixins.moremod.lycanites.json
            // - mixins.moremod.parasites.json
            // - mixins.moremod.champion.json
            // - mixins.moremod.potioncore.json
            // - mixins.moremod.ev.json
            // - mixins.moremod.fermiummixins.json
            // - mixins.moremod.rs.json

            System.out.println("[moremod] All core mixins loaded (decoupled from external mods)");
        } catch (Throwable e) {
            System.err.println("[moremod] Mixin registration failed: " + e);
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