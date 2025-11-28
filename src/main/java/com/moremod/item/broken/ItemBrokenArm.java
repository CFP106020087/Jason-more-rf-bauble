package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.moremod;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 破碎_臂 (Broken Arm)
 *
 * 终局饰品 - 精准与毁灭的极致
 *
 * 能力1: 绝对暴击
 *   - 所有攻击必定暴击
 *   - 暴击伤害 ×3
 *
 * 能力2: 极限延伸
 *   - 攻击距离 +3 格
 *
 * 能力3: 护甲粉碎领域
 *   - 周围敌人护甲归零（属性修改光环）
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenArm extends ItemBrokenBaubleBase {

    // 追踪已经被护甲粉碎的实体及其原始护甲值
    // Key: "dimensionId:entityId" 避免跨维度ID冲突
    private static final Map<String, ArmorData> shreddedEntities = new HashMap<>();

    private static class ArmorData {
        long lastUpdateTime;
        double originalArmor;
        double originalToughness;
        int dimensionId;

        ArmorData(long time, double armor, double toughness, int dimension) {
            this.lastUpdateTime = time;
            this.originalArmor = armor;
            this.originalToughness = toughness;
            this.dimensionId = dimension;
        }
    }

    /**
     * 生成实体的唯一键
     */
    private static String getEntityKey(EntityLivingBase entity) {
        return entity.world.provider.getDimension() + ":" + entity.getEntityId();
    }

    public ItemBrokenArm() {
        setRegistryName("broken_arm");
        setTranslationKey("broken_arm");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.RING;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        // 无特殊效果
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (player.world.isRemote) return;
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 每5tick应用护甲粉碎光环
        if (entity.ticksExisted % 5 == 0) {
            applyArmorShredAura(player);
        }
    }

    /**
     * 应用护甲粉碎光环 - 直接设置护甲和韧性为0
     */
    private void applyArmorShredAura(EntityPlayer player) {
        double range = BrokenGodConfig.armArmorShredRange;
        int dimensionId = player.world.provider.getDimension();

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range);

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e instanceof IMob && e.isEntityAlive()
        );

        long currentTime = player.world.getTotalWorldTime();

        for (EntityLivingBase target : entities) {
            String entityKey = getEntityKey(target);
            IAttributeInstance armorAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
            IAttributeInstance toughnessAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

            // 如果是新目标，保存原始值
            if (!shreddedEntities.containsKey(entityKey)) {
                double originalArmor = armorAttr != null ? armorAttr.getBaseValue() : 0;
                double originalToughness = toughnessAttr != null ? toughnessAttr.getBaseValue() : 0;
                shreddedEntities.put(entityKey, new ArmorData(currentTime, originalArmor, originalToughness, dimensionId));

                // 设置护甲和韧性为0
                if (armorAttr != null) {
                    armorAttr.setBaseValue(0);
                }
                if (toughnessAttr != null) {
                    toughnessAttr.setBaseValue(0);
                }
            } else {
                // 更新时间
                shreddedEntities.get(entityKey).lastUpdateTime = currentTime;
            }
        }

        // 清理离开范围的实体，恢复其护甲
        Iterator<Map.Entry<String, ArmorData>> iterator = shreddedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ArmorData> entry = iterator.next();
            ArmorData data = entry.getValue();

            // 只处理当前维度的实体
            if (data.dimensionId != dimensionId) continue;

            if (currentTime - data.lastUpdateTime > 10) {
                // 超过10tick没更新，说明已离开范围
                // 解析 entityId
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    try {
                        int entityId = Integer.parseInt(parts[1]);
                        net.minecraft.entity.Entity e = player.world.getEntityByID(entityId);
                        if (e instanceof EntityLivingBase) {
                            restoreArmor((EntityLivingBase) e, data);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                iterator.remove();
            }
        }
    }

    /**
     * 恢复实体护甲值
     */
    private static void restoreArmor(EntityLivingBase target, ArmorData data) {
        IAttributeInstance armorAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR);
        IAttributeInstance toughnessAttr = target.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS);

        if (armorAttr != null) {
            armorAttr.setBaseValue(data.originalArmor);
        }
        if (toughnessAttr != null) {
            toughnessAttr.setBaseValue(data.originalToughness);
        }
    }

    /**
     * 清理指定实体（实体死亡时调用）
     */
    public static void cleanupEntity(EntityLivingBase entity) {
        String key = getEntityKey(entity);
        shreddedEntities.remove(key);
    }

    /**
     * 清理指定维度的所有实体（世界卸载时调用）
     */
    public static void cleanupDimension(int dimensionId) {
        shreddedEntities.entrySet().removeIf(entry -> entry.getValue().dimensionId == dimensionId);
    }

    /**
     * 清理所有追踪的实体
     */
    public static void cleanupAll() {
        shreddedEntities.clear();
    }

    /**
     * 获取基础伤害倍率
     */
    public static float getDamageMultiplier() {
        return (float) BrokenGodConfig.armDamageMultiplier;
    }

    /**
     * 获取暴击伤害倍率
     */
    public static float getCritMultiplier() {
        return (float) BrokenGodConfig.armCritMultiplier;
    }

    /**
     * 获取护甲粉碎光环范围
     */
    public static float getArmorShredRange() {
        return (float) BrokenGodConfig.armArmorShredRange;
    }

    /**
     * 获取攻击距离延长
     */
    public static float getRangeExtension() {
        return (float) BrokenGodConfig.armRangeExtension;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_臂");
        tooltip.add(TextFormatting.DARK_GRAY + "Broken Arm");
        tooltip.add("");
        tooltip.add(TextFormatting.GOLD + "◆ 绝对暴击");
        tooltip.add(TextFormatting.GRAY + "  所有攻击必定暴击");
        tooltip.add(TextFormatting.YELLOW + "  伤害 ×" + BrokenGodConfig.armDamageMultiplier);
        tooltip.add(TextFormatting.YELLOW + "  暴击伤害 ×" + BrokenGodConfig.armCritMultiplier);
        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 极限延伸");
        tooltip.add(TextFormatting.GRAY + "  攻击距离 +" + (int) BrokenGodConfig.armRangeExtension + " 格");
        tooltip.add("");
        tooltip.add(TextFormatting.RED + "◆ 护甲粉碎领域");
        tooltip.add(TextFormatting.GRAY + "  " + (int) BrokenGodConfig.armArmorShredRange + " 格内敌人");
        tooltip.add(TextFormatting.AQUA + "  护甲归零");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"机械之臂，粉碎一切防御\"");
        tooltip.add(TextFormatting.DARK_GRAY + "" + TextFormatting.ITALIC + "\"在此领域内，护甲毫无意义\"");
        tooltip.add(TextFormatting.DARK_RED + "═══════════════════════════");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    // ========== 事件处理器 - 清理内存泄漏 ==========

    @Mod.EventBusSubscriber(modid = moremod.MODID)
    public static class ArmorShredCleanupHandler {

        /**
         * 实体死亡时清理
         */
        @SubscribeEvent
        public static void onEntityDeath(LivingDeathEvent event) {
            if (event.getEntityLiving().world.isRemote) return;
            cleanupEntity(event.getEntityLiving());
        }

        /**
         * 世界卸载时清理该维度的所有追踪
         */
        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            if (event.getWorld().isRemote) return;
            cleanupDimension(event.getWorld().provider.getDimension());
        }
    }
}
