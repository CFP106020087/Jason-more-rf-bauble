// ============================================
// ItemVillagerCapsule.java - 修复村民消失问题
// 位置: com/moremod/item/ItemVillagerCapsule.java
// ============================================
package com.moremod.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 🏪 村民膠囊 - 用於捕捉和釋放村民/商人
 *
 * 支持的实体类型:
 * - EntityVillager (原版村民)
 * - EntityWanderingTrader (流浪商人 - Traders mod)
 * - 任何实现 IMerchant 接口的 EntityLivingBase
 */
public class ItemVillagerCapsule extends Item {

    public ItemVillagerCapsule() {
        setTranslationKey("villager_capsule");
        setRegistryName("villager_capsule");
        setMaxStackSize(1);
    }

    /**
     * 右鍵點擊實體（捕捉村民/商人）
     */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player,
                                            EntityLivingBase target, EnumHand hand) {
        // ⚠️ 只在服务端执行
        if (player.world.isRemote) {
            return true; // 客户端返回 true 表示消耗交互
        }

        System.out.println("[VillagerCapsule] 开始捕捉流程...");

        // 检查是否是可交易实体 (IMerchant)
        if (!(target instanceof IMerchant)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "只能捕捉村民或商人！"));
            return false;
        }

        // 检查胶囊是否已经有村民
        if (hasMerchant(stack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "胶囊已经包含一个村民/商人！"));
            return false;
        }

        IMerchant merchant = (IMerchant) target;
        String entityTypeName = getMerchantTypeName(target);

        System.out.println("[VillagerCapsule] 商人信息:");
        System.out.println("  - 类型: " + entityTypeName);
        System.out.println("  - 实体类: " + target.getClass().getName());
        System.out.println("  - 位置: " + target.getPosition());
        System.out.println("  - UUID: " + target.getUniqueID());

        // 🔥 重要：先保存数据，再移除实体
        boolean success = captureMerchant(stack, target);

        if (!success) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "捕捉失败！无法保存数据。"));
            return false;
        }

        // ✅ 验证数据是否保存
        if (!hasMerchant(stack)) {
            System.err.println("[VillagerCapsule] ❌ 数据保存失败！");
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "捕捉失败！数据未正确保存。"));
            return false;
        }

        System.out.println("[VillagerCapsule] ✅ 商人数据已保存到胶囊");

        // 播放粒子效果
        if (player.world instanceof WorldServer) {
            WorldServer worldServer = (WorldServer) player.world;
            worldServer.spawnParticle(
                    EnumParticleTypes.CLOUD,
                    target.posX,
                    target.posY + 1.0,
                    target.posZ,
                    20, // 数量
                    0.5, 0.5, 0.5, // 范围
                    0.05 // 速度
            );
        }

        // 移除实体
        target.setDead();
        System.out.println("[VillagerCapsule] 商人实体已移除");

        // 提示信息
        player.sendMessage(new TextComponentString(
                TextFormatting.GREEN + "✓ 成功捕捉！类型: " + entityTypeName));

        // 🔥 强制更新玩家手中的物品
        player.setHeldItem(hand, stack);
        player.inventoryContainer.detectAndSendChanges();

        System.out.println("[VillagerCapsule] 捕捉完成！");

        return true;
    }

    /**
     * 右鍵點擊方塊（釋放村民/商人）
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

        // 检查是否有商人
        if (!hasMerchant(stack)) {
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "胶囊是空的！"));
            return EnumActionResult.FAIL;
        }

        // 释放商人
        Entity entity = releaseMerchant(stack, world);

        if (entity != null) {
            // 设置位置（在方块上方）
            BlockPos spawnPos = pos.offset(facing);
            entity.setPosition(
                    spawnPos.getX() + 0.5,
                    spawnPos.getY(),
                    spawnPos.getZ() + 0.5
            );

            // 生成到世界
            boolean spawned = world.spawnEntity(entity);

            if (!spawned) {
                System.err.println("[VillagerCapsule] ❌ 实体生成失败！");
                player.sendMessage(new TextComponentString(
                        TextFormatting.RED + "释放失败！无法生成实体。"));
                return EnumActionResult.FAIL;
            }

            System.out.println("[VillagerCapsule] ✅ 商人已生成:");
            System.out.println("  - 类型: " + entity.getClass().getSimpleName());
            System.out.println("  - 位置: " + entity.getPosition());

            // 播放粒子效果
            if (world instanceof WorldServer) {
                WorldServer worldServer = (WorldServer) world;
                worldServer.spawnParticle(
                        EnumParticleTypes.EXPLOSION_NORMAL,
                        entity.posX,
                        entity.posY + 1.0,
                        entity.posZ,
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
                    TextFormatting.GREEN + "✓ 成功释放！"));

            System.out.println("[VillagerCapsule] 释放完成！");

            return EnumActionResult.SUCCESS;
        } else {
            System.err.println("[VillagerCapsule] ❌ 无法从NBT创建实体");
            player.sendMessage(new TextComponentString(
                    TextFormatting.RED + "释放失败！数据损坏。"));
            return EnumActionResult.FAIL;
        }
    }

    /**
     * 添加提示信息
     */
    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        if (hasMerchant(stack)) {
            tooltip.add(TextFormatting.GREEN + "✓ 包含商人");

            NBTTagCompound tag = stack.getTagCompound();
            if (tag != null) {
                // 显示实体类型
                if (tag.hasKey("MerchantType")) {
                    String typeName = tag.getString("MerchantType");
                    tooltip.add(TextFormatting.AQUA + "类型: " + typeName);
                }

                // 显示职业 (村民特有)
                if (tag.hasKey("ProfessionName")) {
                    String profession = tag.getString("ProfessionName");
                    tooltip.add(TextFormatting.GRAY + "职业: " + profession);
                }

                // 显示交易数量
                if (tag.hasKey("TradeCount")) {
                    int tradeCount = tag.getInteger("TradeCount");
                    tooltip.add(TextFormatting.GRAY + "交易数量: " + tradeCount);
                }

                // 显示等级 (村民特有)
                if (tag.hasKey("MerchantData")) {
                    NBTTagCompound merchantData = tag.getCompoundTag("MerchantData");
                    if (merchantData.hasKey("CareerLevel")) {
                        int level = merchantData.getInteger("CareerLevel");
                        tooltip.add(TextFormatting.GRAY + "等级: " + level);
                    }
                }
            }

            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "右键方块释放");
        } else {
            tooltip.add(TextFormatting.RED + "✗ 空胶囊");
            tooltip.add("");
            tooltip.add(TextFormatting.YELLOW + "右键村民/商人捕捉");
        }
    }

    // ========== 静态工具方法 ==========

    /**
     * 捕捉商人到物品（返回是否成功）
     * 支持 EntityVillager 和任何 IMerchant 实体
     */
    public static boolean captureMerchant(ItemStack stack, EntityLivingBase entity) {
        if (!(entity instanceof IMerchant)) {
            System.err.println("[VillagerCapsule] ❌ 实体不是商人！");
            return false;
        }

        try {
            NBTTagCompound tag = stack.hasTagCompound() ?
                    stack.getTagCompound() : new NBTTagCompound();

            // 保存实体完整数据
            NBTTagCompound merchantData = new NBTTagCompound();
            entity.writeToNBT(merchantData);

            // ⚠️ 验证数据是否有效
            if (merchantData.isEmpty()) {
                System.err.println("[VillagerCapsule] ❌ 商人NBT数据为空！");
                return false;
            }

            // 保存实体类型ID (用于正确重建实体)
            ResourceLocation entityId = EntityList.getKey(entity);
            if (entityId != null) {
                tag.setString("EntityId", entityId.toString());
            } else {
                // 如果没有注册ID，使用类名
                tag.setString("EntityClass", entity.getClass().getName());
            }

            tag.setTag("MerchantData", merchantData);

            // 保存显示用的类型名称
            String typeName = getMerchantTypeName(entity);
            tag.setString("MerchantType", typeName);

            // 保存交易数量
            IMerchant merchant = (IMerchant) entity;
            MerchantRecipeList recipes = merchant.getRecipes(null);
            if (recipes != null) {
                tag.setInteger("TradeCount", recipes.size());
            }

            // 如果是村民，额外保存职业信息
            if (entity instanceof EntityVillager) {
                EntityVillager villager = (EntityVillager) entity;
                String professionName = getProfessionName(villager);
                tag.setString("ProfessionName", professionName);
            }

            stack.setTagCompound(tag);

            // 🔥 验证保存是否成功
            if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey("MerchantData")) {
                System.err.println("[VillagerCapsule] ❌ NBT保存验证失败！");
                return false;
            }

            System.out.println("[VillagerCapsule] ✅ 商人数据保存成功");
            System.out.println("  - 类型: " + typeName);
            System.out.println("  - NBT大小: " + merchantData.getSize() + " 标签");

            return true;

        } catch (Exception e) {
            System.err.println("[VillagerCapsule] ❌ 捕捉商人时发生异常:");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从物品释放商人
     */
    @Nullable
    public static Entity releaseMerchant(ItemStack stack, World world) {
        if (!hasMerchant(stack)) {
            System.err.println("[VillagerCapsule] 胶囊中没有商人数据");
            return null;
        }

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey("MerchantData")) {
            System.err.println("[VillagerCapsule] NBT数据无效");
            return null;
        }

        try {
            Entity entity = null;

            // 尝试通过EntityId创建实体
            if (tag.hasKey("EntityId")) {
                String entityIdStr = tag.getString("EntityId");
                ResourceLocation entityId = new ResourceLocation(entityIdStr);
                entity = EntityList.createEntityByIDFromName(entityId, world);
            }

            // 备用：通过类名创建 (主要用于原版村民)
            if (entity == null) {
                entity = new EntityVillager(world);
            }

            // 从NBT恢复数据
            NBTTagCompound merchantData = tag.getCompoundTag("MerchantData");
            entity.readFromNBT(merchantData);

            System.out.println("[VillagerCapsule] ✅ 商人从NBT恢复成功");
            System.out.println("  - 类型: " + entity.getClass().getSimpleName());

            return entity;

        } catch (Exception e) {
            System.err.println("[VillagerCapsule] ❌ 释放商人失败:");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查物品是否包含商人
     */
    public static boolean hasMerchant(ItemStack stack) {
        return stack.hasTagCompound() &&
                stack.getTagCompound().hasKey("MerchantData");
    }

    /**
     * 兼容旧版：检查是否包含村民 (别名)
     */
    public static boolean hasVillager(ItemStack stack) {
        // 兼容旧版NBT格式
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            return tag.hasKey("MerchantData") || tag.hasKey("VillagerData");
        }
        return false;
    }

    /**
     * 获取商人数据（用于交易机）
     */
    @Nullable
    public static NBTTagCompound getMerchantData(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        // 兼容新旧格式
        if (tag.hasKey("MerchantData")) {
            return tag.getCompoundTag("MerchantData");
        }
        if (tag.hasKey("VillagerData")) {
            return tag.getCompoundTag("VillagerData");
        }
        return null;
    }

    /**
     * 兼容旧版：获取村民数据 (别名)
     */
    @Nullable
    public static NBTTagCompound getVillagerData(ItemStack stack) {
        return getMerchantData(stack);
    }

    /**
     * 获取存储的实体类型ID
     */
    @Nullable
    public static String getStoredEntityId(ItemStack stack) {
        if (!stack.hasTagCompound()) return null;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag.hasKey("EntityId")) {
            return tag.getString("EntityId");
        }
        return null;
    }

    /**
     * 获取商人类型显示名称
     */
    private static String getMerchantTypeName(EntityLivingBase entity) {
        if (entity instanceof EntityVillager) {
            return "村民";
        }

        // 检查是否是流浪商人 (通过类名判断，避免硬依赖)
        String className = entity.getClass().getSimpleName();
        if (className.contains("WanderingTrader") || className.contains("Wandering")) {
            return "流浪商人";
        }

        // 其他IMerchant实现
        return className;
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