package com.moremod.system.humanity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * 崩解状态专用伤害源
 * Dissolution DamageSource for humanity dissolution damage
 *
 * 死亡信息: "玩家ABC 因彻底脱离常理的存在而死"
 */
public class DamageSourceDissolution extends DamageSource {

    public static final DamageSourceDissolution DISSOLUTION = new DamageSourceDissolution();

    public DamageSourceDissolution() {
        super("dissolution");
        this.setDamageBypassesArmor();  // 无视护甲
        this.setDamageIsAbsolute();      // 绝对伤害（无视抗性）
    }

    @Override
    public ITextComponent getDeathMessage(EntityLivingBase entity) {
        // 返回自定义死亡信息
        // 使用翻译键以支持多语言
        return new TextComponentTranslation("death.attack.dissolution", entity.getDisplayName());
    }

    /**
     * 检查是否为崩解伤害
     */
    public static boolean isDissolutionDamage(DamageSource source) {
        return source instanceof DamageSourceDissolution || "dissolution".equals(source.getDamageType());
    }
}
