package com.moremod.item;

import com.google.common.collect.Multimap;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class ItemSwordChengYueGeo extends ItemSword implements IAnimatable {

    private final AnimationFactory factory = new AnimationFactory(this);

    public ItemSwordChengYueGeo(ToolMaterial material) {
        super(material);
        setRegistryName("sword_chengyue_geo");
        setTranslationKey("sword_chengyue_geo");
        setCreativeTab(CreativeTabs.COMBAT);
    }

    // ===== GeckoLib 必要實作 =====
    @Override public void registerControllers(AnimationData data) { /* 先不加控制器，純靜態即可 */ }
    @Override public AnimationFactory getFactory() { return factory; }

    // ===== 可選：右鍵切換 Drawn NBT，之後可拿來隱藏/顯示鞘骨骼 =====
    public static boolean isDrawn(ItemStack stack) {
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.getBoolean("Drawn");
    }
    public static void setDrawn(ItemStack stack, boolean drawn) {
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setBoolean("Drawn", drawn);
    }
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        boolean now = !isDrawn(stack);
        if (!world.isRemote) {
            setDrawn(stack, now);
            world.playSound(null, player.posX, player.posY, player.posZ,
                    now ? SoundEvents.ITEM_ARMOR_EQUIP_IRON : SoundEvents.ITEM_SHIELD_BLOCK,
                    SoundCategory.PLAYERS, 0.6F, now ? 1.25F : 0.9F);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    // （保留原本屬性邏輯）
    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        return super.getItemAttributeModifiers(slot);
    }
}
