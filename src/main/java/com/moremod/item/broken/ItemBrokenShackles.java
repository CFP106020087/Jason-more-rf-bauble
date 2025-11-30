package com.moremod.item.broken;

import baubles.api.BaubleType;
import com.moremod.config.BrokenGodConfig;
import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.system.ascension.BrokenGodHandler;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 破碎_枷锁 (Broken Shackles)
 *
 * 终局饰品 - 绝对领域
 *
 * 能力1: 时空冻结
 *   - 10格内敌人移速归零（属性修改）
 *
 * 能力2: 混乱领域
 *   - 范围内敌人陷入混乱
 *
 * 能力3: 坚守代价
 *   - 自身移速 -30%
 *   - 所受伤害 -50%
 *
 * 不可卸下，右键自动替换槽位饰品
 */
public class ItemBrokenShackles extends ItemBrokenBaubleBase {

    private static final UUID SELF_SPEED_UUID = UUID.fromString("d1234567-89ab-cdef-0123-456789abcdef");
    private static final UUID FREEZE_MODIFIER_UUID = UUID.fromString("d2345678-9abc-def0-1234-56789abcdef0");

    // 追踪已经被冻结的实体
    private static final Map<Integer, Long> frozenEntities = new HashMap<>();

    /**
     * 清理所有静态状态（防止跨存档污染）
     * 在世界卸载时调用
     */
    public static void clearAllState() {
        frozenEntities.clear();
    }

    public ItemBrokenShackles() {
        setRegistryName("broken_shackles");
        setTranslationKey("broken_shackles");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    @Override
    public BaubleType getBaubleType(ItemStack itemstack) {
        return BaubleType.BELT;
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            applySpeedReduction((EntityPlayer) player);
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer) {
            removeSpeedReduction((EntityPlayer) player);
        }
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase entity) {
        if (!(entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) entity;

        if (player.world.isRemote) return;
        if (!BrokenGodHandler.isBrokenGod(player)) return;

        // 确保速度减益存在
        if (entity.ticksExisted % 100 == 0) {
            applySpeedReduction(player);
        }

        // 每5tick应用时空冻结光环
        if (entity.ticksExisted % 5 == 0) {
            applyFreezeAura(player);
        }
    }

    /**
     * 应用时空冻结光环 - 使用属性修改而非药水
     */
    private void applyFreezeAura(EntityPlayer player) {
        double range = BrokenGodConfig.shacklesAuraRange;

        AxisAlignedBB aabb = player.getEntityBoundingBox().grow(range);

        List<EntityLivingBase> entities = player.world.getEntitiesWithinAABB(
                EntityLivingBase.class, aabb,
                e -> e != player && e instanceof IMob && e.isEntityAlive()
        );

        long currentTime = player.world.getTotalWorldTime();

        for (EntityLivingBase target : entities) {
            IAttributeInstance speedAttr = target.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
            if (speedAttr == null) continue;

            // 应用冻结修改器（将速度设为0）
            AttributeModifier freezeMod = speedAttr.getModifier(FREEZE_MODIFIER_UUID);
            if (freezeMod == null) {
                // 使用 -1.0 和 operation 2（乘法）将速度归零
                speedAttr.applyModifier(new AttributeModifier(
                        FREEZE_MODIFIER_UUID,
                        "Broken Shackles Freeze",
                        -1.0,
                        2 // 乘法 = 速度 × 0
                ));
            }

            // 记录冻结时间
            frozenEntities.put(target.getEntityId(), currentTime);

            // 应用混乱效果（随机移动方向）
            if (target.ticksExisted % 20 == 0) {
                // 随机改变目标的朝向
                target.rotationYaw += (player.world.rand.nextFloat() - 0.5f) * 180;
            }
        }

        // 清理离开范围的实体的冻结效果
        frozenEntities.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > 10) {
                // 超过10tick没更新，说明已离开范围
                net.minecraft.entity.Entity e = player.world.getEntityByID(entry.getKey());
                if (e instanceof EntityLivingBase) {
                    IAttributeInstance speedAttr = ((EntityLivingBase) e).getEntityAttribute(
                            SharedMonsterAttributes.MOVEMENT_SPEED);
                    if (speedAttr != null) {
                        speedAttr.removeModifier(FREEZE_MODIFIER_UUID);
                    }
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 应用自身速度减少
     */
    private void applySpeedReduction(EntityPlayer player) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SELF_SPEED_UUID);
        if (existing == null) {
            double reduction = -BrokenGodConfig.shacklesSelfSlow;
            speedAttr.applyModifier(new AttributeModifier(
                    SELF_SPEED_UUID,
                    "Broken Shackles Self Slow",
                    reduction,
                    2 // 乘法
            ));
        }
    }

    /**
     * 移除速度减少
     */
    private void removeSpeedReduction(EntityPlayer player) {
        IAttributeInstance speedAttr = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        AttributeModifier existing = speedAttr.getModifier(SELF_SPEED_UUID);
        if (existing != null) {
            speedAttr.removeModifier(existing);
        }
    }

    /**
     * 获取伤害减免比例（由事件处理器调用）
     */
    public static float getDamageReduction() {
        return (float) BrokenGodConfig.shacklesDamageReduction;
    }

    /**
     * 获取光环范围
     */
    public static float getAuraRange() {
        return (float) BrokenGodConfig.shacklesAuraRange;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.RED + "" + TextFormatting.BOLD + "破碎_枷锁");
        tooltip.add(TextFormatting.BLUE + "◆ 冻结: " + TextFormatting.GRAY + (int) BrokenGodConfig.shacklesAuraRange + "格内敌人定身");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "◆ 混乱: " + TextFormatting.GRAY + "范围敌人陷入混乱");
        tooltip.add(TextFormatting.YELLOW + "◆ 代价: " + TextFormatting.RED + "-" + (int)(BrokenGodConfig.shacklesSelfSlow * 100) + "%移速 " + TextFormatting.GREEN + "-" + (int)(BrokenGodConfig.shacklesDamageReduction * 100) + "%受伤");
        tooltip.add(TextFormatting.DARK_RED + "⚠ 无法卸除");
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}
