package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumHandSide;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 不朽护符 - 提供完全伤害免疫和敌对生物无视
 * moremod Mod
 */
@Mod.EventBusSubscriber(modid = "moremod")
public class ImmortalAmulet extends Item implements IBauble {

    public static final ImmortalAmulet INSTANCE = new ImmortalAmulet();

    public ImmortalAmulet() {
        super();
        this.setMaxStackSize(1);
        this.setTranslationKey("immortal_amulet");
        this.setRegistryName("moremod", "immortal_amulet");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
    }

    // ==================== Baubles API 实现 ====================

    @Override
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.AMULET;
    }

    @Override
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        // 可选：添加粒子效果或其他视觉效果
    }

    @Override
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "不朽护符的力量笼罩着你"));
        }
    }

    @Override
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) {
        if (!player.world.isRemote && player instanceof EntityPlayer) {
            ((EntityPlayer) player).sendMessage(new TextComponentString(
                    TextFormatting.DARK_RED + "不朽护符的保护消失了"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.GOLD + "佩戴时:");
        tooltip.add(TextFormatting.GREEN + "  • 免疫所有伤害");
        tooltip.add(TextFormatting.GREEN + "  • 敌对生物无法察觉你");
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "" + TextFormatting.ITALIC + "远古的神器，赋予佩戴者不朽之力");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    // ==================== 物品注册 ====================

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        IForgeRegistry<Item> registry = event.getRegistry();
        registry.register(INSTANCE);
    }

    // ==================== 工具方法 ====================

    private static boolean hasImmortalAmulet(EntityPlayer player) {
        IItemHandler baubles = BaublesApi.getBaublesHandler(player);
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == INSTANCE) {
                return true;
            }
        }
        return false;
    }

    // ==================== 事件处理器 ====================

    /**
     * 处理伤害事件 - 最早阶段
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (hasImmortalAmulet(player)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 处理伤害事件 - 计算阶段
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (hasImmortalAmulet(player)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 处理攻击事件 - 最早阶段
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (hasImmortalAmulet(player)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 阻止敌对生物锁定玩家
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSetAttackTarget(LivingSetAttackTargetEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) && event.getTarget() instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) event.getTarget();

            if (hasImmortalAmulet(targetPlayer)) {
                EntityLivingBase attacker = event.getEntityLiving();

                if (attacker instanceof EntityLiving) {
                    ((EntityLiving) attacker).setAttackTarget(null);

                    // 设置假目标，防止生物继续追踪
                    if (targetPlayer.equals(attacker.getRevengeTarget())) {
                        attacker.setRevengeTarget(new DummyTarget(attacker.world));
                    }
                }
            }
        }
    }

    /**
     * 假目标实体类 - 用于迷惑敌对生物
     */
    public static class DummyTarget extends EntityLivingBase {
        public DummyTarget(World world) {
            super(world);
            this.setInvisible(true);
            this.setSize(0.0F, 0.0F);
            this.setEntityInvulnerable(true);
        }

        @Override
        public void onUpdate() {
            // 假实体不需要更新
            this.isDead = true;
        }

        @Override
        public ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack) {
            // 不做任何事
        }

        @Override
        public Iterable<ItemStack> getArmorInventoryList() {
            return java.util.Collections.emptyList();
        }

        @Override
        public EnumHandSide getPrimaryHand() {
            return EnumHandSide.RIGHT;
        }
    }
}