package com.moremod.capability.framework.example;

import com.moremod.api.capability.ICapabilityContainer;
import com.moremod.capability.framework.CapabilityContainerProvider;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 能力系统使用示例
 *
 * 此文件展示如何在实际代码中使用能力系统
 */
public class ExampleUsage {

    /**
     * 示例 1：基础使用 - 获取和使用能力
     */
    public static void basicUsage(EntityPlayer player) {
        // 获取能力容器
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container == null) {
            System.out.println("Capability system not available");
            return;
        }

        // 获取能力
        IExampleCapability capability = container.getCapability(IExampleCapability.class);
        if (capability != null) {
            // 使用能力
            int currentEnergy = capability.getEnergy();
            System.out.println("Current energy: " + currentEnergy);

            // 消耗能量
            if (capability.consumeEnergy(10)) {
                System.out.println("Consumed 10 energy");
            } else {
                System.out.println("Not enough energy");
            }

            // 添加能量
            capability.addEnergy(50);
        }
    }

    /**
     * 示例 2：安全使用 - 检查能力是否存在
     */
    public static boolean safeUsage(EntityPlayer player) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container == null) {
            return false;
        }

        // 先检查能力是否存在
        if (container.hasCapability(IExampleCapability.class)) {
            IExampleCapability capability = container.getCapability(IExampleCapability.class);
            return capability.consumeEnergy(100);
        }

        return false;
    }

    /**
     * 示例 3：通过 ID 获取能力
     */
    public static void getByIdExample(EntityPlayer player) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container == null) {
            return;
        }

        // 通过 ID 获取
        if (container.hasCapability(ExampleCapabilityImpl.CAPABILITY_ID)) {
            IExampleCapability capability =
                (IExampleCapability) container.getCapability(ExampleCapabilityImpl.CAPABILITY_ID);

            if (capability != null) {
                capability.setEnergy(500);
            }
        }
    }

    /**
     * 示例 4：遍历所有能力
     */
    public static void iterateAllCapabilities(EntityPlayer player) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container == null) {
            return;
        }

        // 遍历所有能力
        container.getAllCapabilities().forEach(capability -> {
            System.out.println("Found capability: " + capability.getCapabilityId());
        });
    }

    /**
     * 示例 5：在事件中使用
     */
    public static void onPlayerAttack(EntityPlayer player) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container == null) {
            return; // Fallback 模式，能力不可用
        }

        IExampleCapability energy = container.getCapability(IExampleCapability.class);
        if (energy != null && energy.consumeEnergy(5)) {
            // 消耗能量成功，执行强力攻击
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "Used powered attack! Energy: " + energy.getEnergy() + "/" + energy.getMaxEnergy()
            ));
        } else {
            // 能量不足，普通攻击
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "Not enough energy for powered attack!"
            ));
        }
    }

    /**
     * 示例 6：Null-safe 辅助方法
     */
    public static int getPlayerEnergy(EntityPlayer player) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container != null) {
            IExampleCapability capability = container.getCapability(IExampleCapability.class);
            if (capability != null) {
                return capability.getEnergy();
            }
        }
        return 0; // 默认值
    }

    /**
     * 示例 7：修改能量的安全方法
     */
    public static void addPlayerEnergy(EntityPlayer player, int amount) {
        ICapabilityContainer<EntityPlayer> container = getContainer(player);
        if (container != null) {
            IExampleCapability capability = container.getCapability(IExampleCapability.class);
            if (capability != null) {
                capability.addEnergy(amount);
            }
        }
    }

    /**
     * 获取玩家的能力容器
     * 这是一个辅助方法，在实际代码中可以重复使用
     */
    private static ICapabilityContainer<EntityPlayer> getContainer(EntityPlayer player) {
        if (player.hasCapability(CapabilityContainerProvider.CAPABILITY, null)) {
            return player.getCapability(CapabilityContainerProvider.CAPABILITY, null);
        }
        return null;
    }

    /**
     * 示例 8：在命令中使用
     */
    public static class ExampleCommand {
        public static void executeSetEnergy(EntityPlayer player, int amount) {
            ICapabilityContainer<EntityPlayer> container = getContainer(player);
            if (container == null) {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "Capability system not available"
                ));
                return;
            }

            IExampleCapability capability = container.getCapability(IExampleCapability.class);
            if (capability != null) {
                capability.setEnergy(amount);
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "Energy set to " + amount
                ));
            } else {
                player.sendMessage(new net.minecraft.util.text.TextComponentString(
                    "Energy capability not found"
                ));
            }
        }

        public static void executeGetEnergy(EntityPlayer player) {
            int energy = getPlayerEnergy(player);
            player.sendMessage(new net.minecraft.util.text.TextComponentString(
                "Current energy: " + energy
            ));
        }
    }
}
