// ============================================
// ItemVillagerCapsule.java - 修复村民消失问题
// 位置: com/moremod/item/ItemVillagerCapsule.java
// ============================================
package com.moremod.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 🏪 村民膠囊 - 用於捕捉和釋放村民
 */
public class ItemVillagerCapsule extends Item {

    public ItemVillagerCapsule() {
        setTranslationKey("villager_capsule");
        setRegistryName("villager_capsule");
        setMaxStackSize(1);
    }

    /**
     * 右鍵點擊實體（捕捉村民）
     */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player,
                                            EntityLivingBase target, EnumHand hand) {
        // ⚠️ 只在服务端执行
        if (player.world.isRemote) {
            return true; // 客户端返回 true 表示消耗交互
        }

        System.out.println("[VillagerCapsule] 开始捕捉流程...");

        // 检查是否是村民
        if (!(target instanceof EntityVillager)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "只能捕捉村民！"));
            return false;
        }

        // 检查胶囊是否已经有村民
        if (hasVillager(stack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "胶囊已经包含一个村民！"));
            return false;
        }

        EntityVillager villager = (EntityVillager) target;

        System.out.println("[VillagerCapsule] 村民信息:");
        System.out.println("  - 职业: " + getProfessionName(villager));
        System.out.println("  - 位置: " + villager.getPosition());
        System.out.println("  - UUID: " + villager.getUniqueID());

        // 🔥 重要：先保存数据，再移除实体
        boolean success = captureVillager(stack, villager);

        if (!success) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "捕捉失败！无法保存村民数据。"));
            return false;
        }

        // ✅ 验证数据是否保存
        if (!hasVillager(stack)) {
            System.err.println("[VillagerCapsule] ❌ 数据保存失败！");
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "捕捉失败！数据未正确保存。"));
            return false;
        }

        System.out.println("[VillagerCapsule] ✅ 村民数据已保存到胶囊");

        // 播放粒子效果（在村民位置）
        if (player.world instanceof WorldServer) {
            WorldServer worldServer = (WorldServer) player.world;
            worldServer.spawnParticle(
                    EnumParticleTypes.CLOUD,
                    villager.posX,
                    villager.posY + 1.0,
                    villager.posZ,
                    20, // 数量
                    0.5, 0.5, 0.5, // 范围
                    0.05 // 速度
            );
        }

        // 移除村民实体
        target.setDead();
        System.out.println("[VillagerCapsule] 村民实体已移除");

        // 提示信息
        String professionName = getProfessionName(villager);
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 成功捕捉村民！职业: " + professionName));

        // 🔥 强制更新玩家手中的物品
        player.setHeldItem(hand, stack);
        player.inventoryContainer.detectAndSendChanges();

        System.out.println("[VillagerCapsule] 捕捉完成！");

        return true;
    }

    /**
     * 右鍵點擊方塊（釋放村民）
     */
    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        // 只在服务端执行
        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = player.getHeldItem(hand);

        System.out.println("[VillagerCapsule] 开始释放流程...");

        // 检查是否有村民
        if (!hasVillager(stack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "胶囊是空的！"));
            return EnumActionResult.FAIL;
        }

        // 释放村民
        EntityVillager villager = releaseVillager(stack, world);

        if (villager != null) {
            // 设置位置（在方块上方）
            BlockPos spawnPos = pos.offset(facing);
            villager.setPosition(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5
            );

            // 生成到世界
            boolean spawned = world.spawnEntity(villager);

            if (!spawned) {
                System.err.println("[VillagerCapsule] ❌ 村民生成失败！");
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "释放失败！无法生成村民。"));
                return EnumActionResult.FAIL;
            }

            System.out.println("[VillagerCapsule] ✅ 村民已生成:");
            System.out.println("  - 职业: " + getProfessionName(villager));
            System.out.println("  - 位置: " + villager.getPosition());

            // 播放粒子效果
            if (world instanceof WorldServer) {
                WorldServer worldServer = (WorldServer) world;
                worldServer.spawnParticle(
                        EnumParticleTypes.EXPLOSION_NORMAL,
                        villager.posX,
                        villager.posY + 1.0,
                        villager.posZ,
                        30,
                        0.5, 0.5, 0.5,
                        0.1
                );
            }

            // 清空胶囊
            stack.setTagCompound(null);
            player.setHeldItem(hand, stack);
            player.inventoryContainer.detectAndSendChanges();

            // 提示信息
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + "✓ 成功释放村民！"));

            System.out.println("[VillagerCapsule] 释放完成！");

            return EnumActionResult.SUCCESS;
        } else {
            System.err.println("[VillagerCapsule] ❌ 无法从NBT创建村民");
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "释放失败！村民数据损坏。"));
            return EnumActionResult.FAIL;
        }
    }

    /**
     * 添加提示信息
     */
    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        if (hasVillager(stack)) {
            tooltip.add(TextFormatting.GREEN + "✓ 包含村民");

            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                // 显示职业
                if (tag.hasKey("ProfessionName")) {
                    String profession = tag.getString("ProfessionName");
                    tooltip.add(TextFormatting.GRAY + "职业: " + profession);
                }

                // 显示等级
                if (tag.hasKey("VillagerData")) {
                    NBTTagCompound villagerData = tag.getCompoundTag("VillagerData");
                    if (villagerData.hasKey("CareerLevel")) {
                        int level = villagerData.getInteger("CareerLevel");
                        tooltip.add(TextFormatting.GRAY + "等级: " + level);
                    }
                }
            }

            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "右键方块释放村民");
        } else {
            tooltip.add(TextFormatting.RED + "✗ 空胶囊");
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "右键村民捕捉");
        }
    }

    // ========== 静态工具方法 ==========

    /**
     * 捕捉村民到物品（返回是否成功）
     */
    public static boolean captureVillager(ItemStack stack, EntityVillager villager) {
        try {
            NBTTagCompound tag = stack.hasTagCompound() ?
                    stack.getTagCompound() : new NBTTagCompound();

            // 保存村民完整数据
            NBTTagCompound villagerData = new NBTTagCompound();
            villager.writeToNBT(villagerData);

            // ⚠️ 验证数据是否有效
            if (villagerData.isEmpty()) {
                System.err.println("[VillagerCapsule] ❌ 村民NBT数据为空！");
                return false;
            }

            tag.setTag("VillagerData", villagerData);

            // 保存职业名称（用于显示）
            String professionName = getProfessionName(villager);
            tag.setString("ProfessionName", professionName);

            // 保存职业ID（从NBT读取）
            if (villagerData.hasKey("Profession")) {
                tag.setInteger("Profession", villagerData.getInteger("Profession"));
            }

            // 保存专业ID（从NBT读取）
            if (villagerData.hasKey("Career")) {
                tag.setInteger("Career", villagerData.getInteger("Career"));
            }

            stack.setTagCompound(tag);

            // 🔥 验证保存是否成功
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("VillagerData")) {
                System.err.println("[VillagerCapsule] ❌ NBT保存验证失败！");
                return false;
            }

            System.out.println("[VillagerCapsule] ✅ 村民数据保存成功");
            System.out.println("  - 职业: " + professionName);
            System.out.println("  - NBT大小: " + villagerData.getSize() + " 标签");

            return true;

        } catch (Exception e) {
            System.err.println("[VillagerCapsule] ❌ 捕捉村民时发生异常:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从物品释放村民
     */
    @Nullable
    public static EntityVillager releaseVillager(ItemStack stack, World world) {
        if (!hasVillager(stack)) {
            System.err.println("[VillagerCapsule] 胶囊中没有村民数据");
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("VillagerData")) {
            System.err.println("[VillagerCapsule] NBT数据无效");
            return null;
        }

        try {
            // 创建村民实体
            EntityVillager villager = new EntityVillager(world);

            // 从NBT恢复数据
            NBTTagCompound villagerData = tag.getCompoundTag("VillagerData");
            villager.readFromNBT(villagerData);

            System.out.println("[VillagerCapsule] ✅ 村民从NBT恢复成功");

            return villager;

        } catch (Exception e) {
            System.err.println("[VillagerCapsule] ❌ 释放村民失败:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查物品是否包含村民
     */
    public static boolean hasVillager(ItemStack stack) {
        return stack.hasTagCompound() &&
                stack.getTagCompound().hasKey("VillagerData");
    }

    /**
     * 获取村民数据（用于交易机）
     */
    @Nullable
    public static NBTTagCompound getVillagerData(ItemStack stack) {
        if (!hasVillager(stack)) return null;
        return stack.getTagCompound().getCompoundTag("VillagerData");
    }

    /**
     * 获取村民职业名称（安全的方式）
     */
    private static String getProfessionName(EntityVillager villager) {
        try {
            // 使用公共API获取职业
            if (villager.getProfessionForge() != null) {
                return villager.getProfessionForge().getRegistryName().toString();
            }

            // 备用方案：使用原版职业ID
            int professionId = villager.getProfession();
            switch (professionId) {
                case 0: return "farmer";      // 农夫
                case 1: return "librarian";   // 图书管理员
                case 2: return "priest";      // 牧师
                case 3: return "blacksmith";  // 铁匠
                case 4: return "butcher";     // 屠夫
                case 5: return "nitwit";      // 傻子
                default: return "unknown";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
}

// ============================================
// 🐛 问题分析与修复
// ============================================
/*
问题：捕捉村民后，村民消失了但胶囊还是空的

原因分析：
1. ❌ 村民被 setDead() 后立即消失（这是正常的）
2. ❌ 但 ItemStack 的 NBT 可能没有正确保存或同步
3. ❌ 客户端和服务端的 ItemStack 不同步

修复方案：
✅ 1. 添加详细的调试日志
✅ 2. 先保存数据再移除实体
✅ 3. 验证数据是否成功保存
✅ 4. 强制更新玩家手中的物品
✅ 5. 添加粒子效果增强反馈
✅ 6. 返回 boolean 表示是否成功
✅ 7. 更多错误检查和处理

测试步骤：
1. 右键村民捕捉
2. 查看控制台日志，确认数据保存成功
3. 查看物品 Tooltip，应该显示村民信息
4. 右键方块释放村民
5. 村民应该出现在方块上方

如果还有问题，检查控制台输出的错误信息！
*/