package com.moremod.util;

import com.moremod.item.ItemEnergySword;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 能量剑客户端工具类 - 处理显示相关逻辑
 * 
 * 此类被 ClientEventHandler 的模型属性重写所引用
 */
@SideOnly(Side.CLIENT)
public class EnergySwordClientUtils {

    /**
     * 获取剑的状态值（用于模型属性重写）
     * 
     * 在 ClientEventHandler 中通过以下方式调用：
     * <pre>
     * RegisterItem.ENERGY_SWORD.addPropertyOverride(
     *     new ResourceLocation("moremod", "sword_state"),
     *     (stack, world, entity) -> EnergySwordClientUtils.getSwordState(stack)
     * );
     * </pre>
     * 
     * @param stack 能量剑物品堆
     * @return 0.0 = 无能量, 1.0 = 有能量但未出鞘, 2.0 = 高能出鞘状态
     */
    public static float getSwordState(ItemStack stack) {
        IEnergyStorage storage = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (storage == null) return 0.0f;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        if (player == null) return 0.0f;

        ItemStack held = player.getHeldItemMainhand();
        if (held.isEmpty() || !ItemStack.areItemStacksEqual(held, stack)) return 0.0f;

        NBTTagCompound tag = stack.getTagCompound();
        boolean allow = tag != null && tag.getBoolean("CanUnsheathe");

        if (storage.getEnergyStored() <= 0) return 0.0f;
        return allow ? 2.0f : 1.0f;
    }

    /**
     * 添加Tooltip信息
     */
    public static void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        IEnergyStorage st = stack.getCapability(CapabilityEnergy.ENERGY, null);
        if (st != null) {
            tip.add(TextFormatting.GRAY + "能量: " + st.getEnergyStored() + " / " + st.getMaxEnergyStored() + " RF");
            
            if (st.getEnergyStored() > 0) {
                NBTTagCompound nbt = stack.getTagCompound();
                boolean allow = nbt != null && nbt.getBoolean("CanUnsheathe");
                
                if (allow) {
                    tip.add(TextFormatting.RED + "高能状态：按住左键自动连续攻击");
                } else {
                    tip.add(TextFormatting.YELLOW + "✗ 条件不足：需机械核心等级 ≥ 30");
                }
            } else {
                tip.add(TextFormatting.DARK_GRAY + "能量耗尽：失去所有特殊效果");
            }
        }
    }
}