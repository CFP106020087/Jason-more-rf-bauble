package com.moremod.item;

import com.moremod.client.render.SawBladeSwordRenderer;
import com.moremod.client.render.debug.SawBladeSwordDebugRenderer;
import net.minecraft.item.ItemSword;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

/**
 * 锯刃剑 (Saw Blade Sword)
 * 
 * 带有GeckoLib 3D动画的武器
 * 
 * 使用说明：
 * 1. 调试时：DEBUG_MODE = true，使用Debug渲染器，可通过NumPad按键实时调整
 * 2. 正式版：DEBUG_MODE = false，使用固定参数的正式渲染器
 */
public class ItemSawBladeSword extends ItemSword implements IAnimatable {
    
    private final AnimationFactory factory = new AnimationFactory(this);
    
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
}