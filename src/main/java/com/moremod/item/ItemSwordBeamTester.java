package com.moremod.item;

import com.moremod.entity.EntitySwordBeam;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 剑气测试器 - 完整版
 * 用于测试数据同步是否正常工作
 */
public class ItemSwordBeamTester extends Item {
    
    public ItemSwordBeamTester() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setTranslationKey("moremod.sword_beam_tester");
        this.setRegistryName("sword_beam_tester");
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        
        // 初始化NBT
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
            stack.getTagCompound().setInteger("beamType", 0);
        }
        
        if (player.isSneaking()) {
            // Shift+右键: 发射剑气
            if (!world.isRemote) {
                int typeIndex = stack.getTagCompound().getInteger("beamType");
                EntitySwordBeam.BeamType type = EntitySwordBeam.BeamType.values()[typeIndex];
                
                // 创建剑气
                EntitySwordBeam beam = new EntitySwordBeam(world, player);
                
                // 设置类型和属性
                beam.setBeamType(type)
                    .setDamage(20.0F)
                    .setPenetrate(3)
                    .setMaxLifetime(200);
                
                // 根据类型设置颜色和缩放
                switch (type) {
                    case DRAGON:
                        beam.setColor(1.0F, 0.2F, 0.2F).setScale(1.5F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.RED + "发射龙形剑气！"));
                        break;
                        
                    case PHOENIX:
                        beam.setColor(1.0F, 0.6F, 0.1F).setScale(1.3F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.GOLD + "发射凤凰剑气！"));
                        break;
                        
                    case SPIRAL:
                        beam.setColor(0.2F, 0.8F, 1.0F).setScale(1.0F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + "发射螺旋剑气！"));
                        break;
                        
                    case BALL:
                        beam.setColor(0.8F, 0.2F, 0.8F).setScale(2.0F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.LIGHT_PURPLE + "发射球形剑气！"));
                        break;
                        
                    case CRESCENT:
                        beam.setColor(0.9F, 0.9F, 1.0F).setScale(1.2F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.WHITE + "发射月牙剑气！"));
                        break;
                        
                    case CROSS:
                        beam.setColor(1.0F, 1.0F, 0.2F).setScale(1.1F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.YELLOW + "发射十字剑气！"));
                        break;
                        
                    case NORMAL:
                    default:
                        beam.setColor(1.0F, 1.0F, 1.0F).setScale(1.0F);
                        player.sendMessage(new TextComponentString(
                            TextFormatting.WHITE + "发射普通剑气！"));
                        break;
                }
                
                // 生成实体
                world.spawnEntity(beam);
                
                // 服务端调试信息
                System.out.println("[Server] Spawned " + type + " beam with ID: " + beam.getEntityId());
                
                // 播放音效
                world.playSound(null, player.posX, player.posY, player.posZ,
                    net.minecraft.init.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                    net.minecraft.util.SoundCategory.PLAYERS,
                    1.0F, 0.8F + world.rand.nextFloat() * 0.4F);
            }
        } else {
            // 右键: 切换类型
            if (!world.isRemote) {
                int currentType = stack.getTagCompound().getInteger("beamType");
                int nextType = (currentType + 1) % EntitySwordBeam.BeamType.values().length;
                stack.getTagCompound().setInteger("beamType", nextType);
                
                EntitySwordBeam.BeamType type = EntitySwordBeam.BeamType.values()[nextType];
                
                // 显示切换信息
                String color = "";
                switch (type) {
                    case DRAGON: color = TextFormatting.RED + ""; break;
                    case PHOENIX: color = TextFormatting.GOLD + ""; break;
                    case SPIRAL: color = TextFormatting.AQUA + ""; break;
                    case BALL: color = TextFormatting.LIGHT_PURPLE + ""; break;
                    case CRESCENT: color = TextFormatting.WHITE + ""; break;
                    case CROSS: color = TextFormatting.YELLOW + ""; break;
                    default: color = TextFormatting.GRAY + ""; break;
                }
                
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.GREEN + "切换到: " + color + type.name()
                ), true);
            }
        }
        
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World world, List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
        tooltip.add(TextFormatting.GOLD + "=== 剑气测试器 ===");
        tooltip.add(TextFormatting.GRAY + "右键: 切换剑气类型");
        tooltip.add(TextFormatting.GRAY + "Shift+右键: 发射剑气");
        tooltip.add("");
        
        if (stack.hasTagCompound()) {
            int typeIndex = stack.getTagCompound().getInteger("beamType");
            EntitySwordBeam.BeamType type = EntitySwordBeam.BeamType.values()[typeIndex];
            
            String color = "";
            String description = "";
            switch (type) {
                case DRAGON: 
                    color = TextFormatting.RED + "";
                    description = "龙形追踪"; 
                    break;
                case PHOENIX: 
                    color = TextFormatting.GOLD + "";
                    description = "凤凰火焰"; 
                    break;
                case SPIRAL: 
                    color = TextFormatting.AQUA + "";
                    description = "螺旋穿透"; 
                    break;
                case BALL: 
                    color = TextFormatting.LIGHT_PURPLE + "";
                    description = "能量球"; 
                    break;
                case CRESCENT: 
                    color = TextFormatting.WHITE + "";
                    description = "月牙斩"; 
                    break;
                case CROSS: 
                    color = TextFormatting.YELLOW + "";
                    description = "十字斩"; 
                    break;
                default: 
                    color = TextFormatting.GRAY + "";
                    description = "基础剑气"; 
                    break;
            }
            
            tooltip.add(TextFormatting.AQUA + "当前: " + color + type.name());
            tooltip.add(TextFormatting.DARK_GRAY + "  " + description);
        }
        
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "数据同步测试版");
    }
    
    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
}