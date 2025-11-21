package com.moremod.item;

import com.moremod.client.render.SawBladeSwordRenderer;
import com.moremod.client.render.debug.SawBladeSwordDebugRenderer;
import com.moremod.item.sawblade.SawBladeActiveSkill;
import com.moremod.item.sawblade.SawBladeNBT;
import com.moremod.item.sawblade.SawBladeStats;
import com.moremod.item.sawblade.SawBladeTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 锯刃剑 (Saw Blade Sword)
 * 
 * 带有GeckoLib 3D动画的武器 + 完整成长系统
 * 
 * 使用说明：
 * 1. 调试时：DEBUG_MODE = true，使用Debug渲染器，可通过NumPad按键实时调整
 * 2. 正式版：DEBUG_MODE = false，使用固定参数的正式渲染器
 * 
 * 成长系统：
 * - 100级成长体系
 * - 5大技能（4被动 + 1主动）
 * - 鲜血欢愉buff + 红色光晕
 * - 背刺角度判定
 * - 完整统计系统
 */
public class ItemSawBladeSword extends ItemSword implements IAnimatable {
    
    private final AnimationFactory factory = new AnimationFactory(this);
    
    // AttributeModifier的UUID（用于属性系统）
    private static final UUID ATTACK_DAMAGE_MODIFIER = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ATTACK_SPEED_MODIFIER = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");
    
    /**
     * ========================================
     *          调试模式开关
     * ========================================
     * 
     * true  = 调试模式
     *   - 使用 SawBladeSwordDebugRenderer
     *   - 可以在游戏内按NumPad *启用调试
     *   - 通过按键实时调整渲染参数
     *   - 适用于：开发、调整参数
     * 
     * false = 正式模式
     *   - 使用 SawBladeSwordRenderer
     *   - 使用固定的优化参数
     *   - 性能最佳
     *   - 适用于：测试、发布
     * 
     * ⚠️ 调试完成后记得改回 false！
     */
    private static final boolean DEBUG_MODE = false;
    
    /**
     * 构造函数
     */
    public ItemSawBladeSword(ToolMaterial material) {
        super(material);
        this.setRegistryName("saw_blade_sword");
        this.setTranslationKey("saw_blade_sword");
        this.setMaxStackSize(1);
    }
    
    // ==================== GeckoLib 动画系统 ====================
    
    /**
     * 注册动画控制器
     */
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(
            this, "controller", 0, this::predicate
        ));
    }
    
    /**
     * 动画判断逻辑
     */
    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        // 如果有动画，在这里播放
        // 例如：
        // event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
        return PlayState.CONTINUE;
    }
    
    /**
     * 获取动画工厂
     */
    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
    
    // ==================== 渲染器管理 ====================
    
    /**
     * 初始化渲染器
     * 
     * 这个方法应该在ClientProxy的init()阶段被调用
     * 它会根据DEBUG_MODE的值自动选择合适的渲染器
     * 
     * 调用示例（在ClientProxy中）：
     * ItemSawBladeSword sawBlade = ModItems.SAW_BLADE_SWORD;
     * if (sawBlade != null) {
     *     sawBlade.initModel();
     * }
     */
    @SideOnly(Side.CLIENT)
    public void initModel() {
        if (DEBUG_MODE) {
            // ===== 调试模式 =====
            this.setTileEntityItemStackRenderer(SawBladeSwordDebugRenderer.INSTANCE);
            System.out.println("[moremod] ========================================");
            System.out.println("[moremod] SawBladeSword using DEBUG renderer");
            System.out.println("[moremod] Press NumPad * to toggle debug mode");
            System.out.println("[moremod] ========================================");
        } else {
            // ===== 正式模式 =====
            this.setTileEntityItemStackRenderer(SawBladeSwordRenderer.INSTANCE);
            System.out.println("[moremod] SawBladeSword using PRODUCTION renderer");
        }
    }
    
    /**
     * 获取当前调试模式状态
     * 
     * @return true=调试模式, false=正式模式
     */
    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }
    
    // ==================== Tooltip显示 ====================
    
    /**
     * 添加Tooltip信息
     * 
     * 显示内容：
     * - 基础属性（攻击力、攻速）
     * - 等级和经验
     * - 技能信息（简化/详细）
     * - 统计数据（F3+H）
     * - Lore
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        // 初始化NBT（确保数据存在）
        SawBladeNBT.init(stack);
        
        // 获取玩家（用于潜行检测和buff状态）
        EntityPlayer player = Minecraft.getMinecraft().player;
        
        // 调用Tooltip工具类
        SawBladeTooltip.addTooltip(stack, player, tooltip, flag);
    }
    
    // ==================== 成长型属性系统 ====================
    
    /**
     * 获取武器属性（成长型）
     * 
     * 重写此方法以实现随等级变化的属性
     * - 攻击力：8.0 → 16.0
     * - 攻速：-2.4 → -2.0
     */
    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        // 初始化NBT
        SawBladeNBT.init(stack);
        
        Multimap<String, AttributeModifier> multimap = HashMultimap.create();
        
        if (slot == EntityEquipmentSlot.MAINHAND) {
            // 获取成长型数值
            float damage = SawBladeStats.getBaseDamage(stack);
            float speed = SawBladeStats.getAttackSpeed(stack);
            
            // 添加攻击力属性
            multimap.put(
                SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                new AttributeModifier(
                    ATTACK_DAMAGE_MODIFIER, 
                    "Weapon modifier", 
                    damage, 
                    0  // 0 = 加法模式
                )
            );
            
            // 添加攻速属性
            multimap.put(
                SharedMonsterAttributes.ATTACK_SPEED.getName(),
                new AttributeModifier(
                    ATTACK_SPEED_MODIFIER, 
                    "Weapon modifier", 
                    speed, 
                    0  // 0 = 加法模式
                )
            );
        }
        
        return multimap;
    }
    
    // ==================== 主动技能：处决收割 ====================
    
    /**
     * 右键使用物品
     * 
     * 潜行 + 右键 = 施放处决收割
     * - AOE处决低血量+出血目标
     * - 范围：6-12格
     * - 数量：3-8个目标
     * - 冷却：30-15秒
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        
        // 只在服务端执行
        if (!world.isRemote) {
            // 检查是否潜行
            if (player.isSneaking()) {
                // 尝试施放处决收割
                boolean success = SawBladeActiveSkill.tryCast(world, player, stack);
                
                if (success) {
                    // 播放挥剑动画
                    player.swingArm(hand);
                    return new ActionResult<>(EnumActionResult.SUCCESS, stack);
                } else {
                    // 冷却中或无目标
                    return new ActionResult<>(EnumActionResult.FAIL, stack);
                }
            }
        }
        
        return new ActionResult<>(EnumActionResult.PASS, stack);
    }
    
    // ==================== 耐久度系统 ====================
    
    /**
     * 攻击实体时的耐久度消耗
     */
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        // 初始化NBT
        SawBladeNBT.init(stack);
        
        // 消耗1点耐久
        stack.damageItem(1, attacker);
        
        return true;
    }
    
    /**
     * 是否可以被修复
     */
    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        // 可以使用铁锭修复
        return net.minecraft.init.Items.IRON_INGOT == repair.getItem() || super.getIsRepairable(toRepair, repair);
    }
    
    // ==================== 附魔光效（可选） ====================
    
    /**
     * 是否显示附魔光效
     * 
     * 当玩家有鲜血欢愉buff时，显示红色附魔光效
     * （备用方案，如果不使用GeckoLib发光层）
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        // 可以在这里添加逻辑：
        // 如果玩家有鲜血欢愉buff，返回true显示附魔光效
        
        // 注释掉以禁用附魔光效（使用GeckoLib发光层）
        /*
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null && PotionBloodEuphoria.hasEffect(player)) {
            return true;
        }
        */
        
        return super.hasEffect(stack);
    }
    
    // ==================== 物品初始化 ====================
    
    /**
     * 物品添加到背包时初始化NBT
     */
    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        super.onCreated(stack, world, player);
        
        // 初始化成长数据
        SawBladeNBT.init(stack);
    }
}