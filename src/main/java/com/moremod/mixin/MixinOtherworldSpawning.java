package com.moremod.mixin;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

/**
 * 异界纤维 - 生物生成Mixin
 */
@Mixin(WorldServer.class)
public class MixinOtherworldSpawning {

    /**
     * 修改生物生成tick - 异界布料增加刷怪率
     * 使用SRG名称 func_72835_b 对应 tick
     */
    @Inject(
            method = "func_72835_b",  // tick的SRG名
            at = @At("TAIL")
    )
    private void otherworld_modifySpawnRate(CallbackInfo ci) {
        WorldServer world = (WorldServer)(Object)this;

        // 检查所有玩家
        for (EntityPlayer player : world.playerEntities) {
            int otherworldCount = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

            if (otherworldCount > 0) {
                // 获取异界数据
                NBTTagCompound otherworldData = getOtherworldData(player);
                int insight = otherworldData.getInteger("Insight");
                int sanity = otherworldData.getInteger("Sanity");

                // 灵视越高，刷怪越多
                if (insight > 50) {
                    // 基础生成概率：每件异界装备1%
                    float spawnChance = 0.01f * otherworldCount;

                    // 灵视加成
                    if (insight > 75) {
                        spawnChance += 0.01f; // 高灵视额外+1%
                    }
                    if (insight > 90) {
                        spawnChance += 0.02f; // 极高灵视再+2%
                    }

                    // 理智惩罚：理智越低，生成越频繁
                    if (sanity < 30) {
                        spawnChance += 0.02f;
                    }
                    if (sanity < 10) {
                        spawnChance += 0.03f;
                    }

                    // 尝试生成异界生物
                    if (world.rand.nextFloat() < spawnChance) {
                        spawnOtherworldCreature(world, player, insight, sanity);
                    }
                }

                // 极低理智时的特殊事件
                if (sanity < 5 && world.rand.nextFloat() < 0.001f) {
                    triggerOtherworldEvent(world, player);
                }
            }
        }
    }

    /**
     * 生成异界生物
     */
    private void spawnOtherworldCreature(World world, EntityPlayer player, int insight, int sanity) {
        Random rand = world.rand;

        // 在玩家周围生成
        double distance = 20 + rand.nextDouble() * 15; // 20-35格距离
        double angle = rand.nextDouble() * Math.PI * 2;
        double x = player.posX + Math.cos(angle) * distance;
        double z = player.posZ + Math.sin(angle) * distance;
        BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
        double y = pos.getY();

        // 检查生成位置是否合法
        if (y <= 0 || y > 255) return;
        if (!world.isAirBlock(pos.up())) return;

        // 根据灵视选择生物类型
        EntityLiving hostile;
        if (insight > 90 && rand.nextFloat() < 0.3f) {
            // 高灵视时生成末影人
            hostile = new EntityEnderman(world);
            hostile.setCustomNameTag(TextFormatting.DARK_PURPLE + "异界使者");
        } else if (insight > 70 && rand.nextFloat() < 0.5f) {
            // 中等灵视生成强化骷髅
            hostile = new EntitySkeleton(world);
            hostile.setCustomNameTag(TextFormatting.DARK_PURPLE + "异界守卫");
        } else if (sanity < 20 && rand.nextFloat() < 0.5f) {
            // 低理智生成爬行者
            hostile = new EntityCreeper(world);
            hostile.setCustomNameTag(TextFormatting.DARK_PURPLE + "混沌之子");
        } else {
            // 默认生成强化僵尸
            hostile = new EntityZombie(world);
            hostile.setCustomNameTag(TextFormatting.DARK_PURPLE + "异界行者");
        }

        hostile.setPosition(x, y, z);

        // 异界强化
        applyOtherworldEnhancements(hostile, insight, sanity);

        // 生成粒子效果提示
        for (int i = 0; i < 10; i++) {
            double px = x + rand.nextGaussian() * 0.5;
            double py = y + rand.nextDouble() * 2;
            double pz = z + rand.nextGaussian() * 0.5;
            world.spawnParticle(net.minecraft.util.EnumParticleTypes.PORTAL, px, py, pz, 0, 0.1, 0, 0);
        }

        world.spawnEntity(hostile);

        // 通知玩家
        if (rand.nextFloat() < 0.3f) {
            player.sendStatusMessage(new TextComponentString(
                    TextFormatting.DARK_PURPLE + "你感觉到异界生物在附近出现..."), true);
        }
    }

    /**
     * 应用异界强化
     */
    private void applyOtherworldEnhancements(EntityLiving hostile, int insight, int sanity) {
        // 基础强化
        float healthMultiplier = 1.5f;
        float damageMultiplier = 1.3f;
        float speedMultiplier = 1.2f;

        // 根据灵视增强
        if (insight > 75) {
            healthMultiplier += 0.5f;
            damageMultiplier += 0.2f;
        }
        if (insight > 90) {
            healthMultiplier += 0.5f;
            damageMultiplier += 0.3f;
            speedMultiplier += 0.2f;
        }

        // 根据理智增强
        if (sanity < 20) {
            damageMultiplier += 0.3f;
            speedMultiplier += 0.1f;
        }
        if (sanity < 10) {
            damageMultiplier += 0.5f;
            speedMultiplier += 0.2f;
        }

        // 应用属性修改
        hostile.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                .setBaseValue(hostile.getMaxHealth() * healthMultiplier);

        if (hostile.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
            hostile.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE)
                    .setBaseValue(hostile.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue() * damageMultiplier);
        }

        hostile.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED)
                .setBaseValue(hostile.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue() * speedMultiplier);

        hostile.setHealth(hostile.getMaxHealth());

        // 添加发光效果
        hostile.setGlowing(true);
    }

    /**
     * 触发异界事件
     */
    private void triggerOtherworldEvent(World world, EntityPlayer player) {
        // 极低理智时的特殊事件
        Random rand = world.rand;
        int eventType = rand.nextInt(3);

        switch(eventType) {
            case 0:
                // 群体生成事件
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_RED + "§k异界之门正在打开..."));
                for (int i = 0; i < 3 + rand.nextInt(3); i++) {
                    spawnOtherworldCreature(world, player, 100, 0);
                }
                break;

            case 1:
                // 传送事件
                double tx = player.posX + (rand.nextDouble() - 0.5) * 100;
                double tz = player.posZ + (rand.nextDouble() - 0.5) * 100;
                BlockPos tpos = world.getTopSolidOrLiquidBlock(new BlockPos(tx, 0, tz));
                player.setPositionAndUpdate(tx, tpos.getY() + 1, tz);
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "§k空间扭曲将你传送到了未知之地..."));
                break;

            case 2:
                // 虚空凝视
                player.attackEntityFrom(net.minecraft.util.DamageSource.OUT_OF_WORLD, 5.0f);
                player.sendMessage(new TextComponentString(
                        TextFormatting.DARK_PURPLE + "§k深渊正在吞噬你的存在..."));
                break;
        }
    }

    /**
     * 获取玩家的异界布料数据
     */
    private NBTTagCompound getOtherworldData(EntityPlayer player) {
        for (ItemStack armor : player.getArmorInventoryList()) {
            if (FabricWeavingSystem.getFabricType(armor) == UpdatedFabricPlayerData.FabricType.OTHERWORLD) {
                return FabricWeavingSystem.getFabricData(armor);
            }
        }
        NBTTagCompound defaultData = new NBTTagCompound();
        defaultData.setInteger("Insight", 0);
        defaultData.setInteger("Sanity", 100);
        return defaultData;
    }
}