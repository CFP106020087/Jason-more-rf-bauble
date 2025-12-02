package com.moremod.fabric.client;

import com.moremod.fabric.data.UpdatedFabricPlayerData;
import com.moremod.fabric.system.FabricWeavingSystem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 布料Tooltip显示 - 最终版
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
public class FabricTooltipHandler {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (!FabricWeavingSystem.hasFabric(stack)) {
            return;
        }

        List<String> tooltip = event.getToolTip();
        EntityPlayer player = event.getEntityPlayer();

        UpdatedFabricPlayerData.FabricType type = FabricWeavingSystem.getFabricType(stack);
        NBTTagCompound fabricData = FabricWeavingSystem.getFabricData(stack);

        if (type == null) {
            tooltip.add("");
            tooltip.add("§c[布料数据损坏]");
            return;
        }

        // 添加空行
        tooltip.add("");

        // 添加分隔线
        tooltip.add("§8§m                              ");

        // 标题
        tooltip.add(getFabricTitle(type));

        // 根据不同布料显示信息
        switch (type) {
            case ABYSS:
                addAbyssTooltip(tooltip, fabricData, player);
                break;
            case TEMPORAL:
                addTemporalTooltip(tooltip, fabricData, player);
                break;
            case SPATIAL:
                addSpatialTooltip(tooltip, fabricData, player);
                break;
            case OTHERWORLD:
                addOtherworldTooltip(tooltip, fabricData, player);
                break;
        }

        // 通用信息
        tooltip.add("");
        tooltip.add("§7织入时间: §f" + getWeaveTime(fabricData));
        tooltip.add("§7布料强度: " + getPowerBar(fabricData.getInteger("FabricPower")));

        // 套装效果提示
        if (player != null) {
            int count = FabricWeavingSystem.countPlayerFabric(player, type);
            if (count > 0) {
                tooltip.add("");
                tooltip.add("§6套装效果 (" + count + "/4):");
                addSetBonus(tooltip, type, count);
            }
        }

        // 分隔线
        tooltip.add("§8§m                              ");
    }

    private static String getFabricTitle(UpdatedFabricPlayerData.FabricType type) {
        switch (type) {
            case ABYSS:
                return "§4§l⚔ 深渊织印 ⚔";
            case TEMPORAL:
                return "§b§l⏰ 时序织印 ⏰";
            case SPATIAL:
                return "§d§l◈ 时空织印 ◈";
            case OTHERWORLD:
                return "§5§l⊙ 异界织印 ⊙";
            default:
                return "§7未知织印";
        }
    }

    private static void addAbyssTooltip(List<String> tooltip, NBTTagCompound data, EntityPlayer player) {
        int kills = data.getInteger("AbyssKills");
        float power = data.getFloat("AbyssPower");

        tooltip.add("§c▸ 当前击杀: §f" + kills);
        tooltip.add("§c▸ 深渊之力: §f" + String.format("%.1f/100", power));

        // 当前加成
        if (kills > 0 || power > 0) {
            tooltip.add("");
            tooltip.add("§c当前增益:");
            tooltip.add("  §7• 攻击力 §c+" + String.format("%.1f", kills * 0.5 + power * 0.2));
            tooltip.add("  §7• 移动速度 §c+" + String.format("%.0f%%", Math.min(kills * 1.0f, 50)));
            tooltip.add("  §7• 生命窃取 §c" + String.format("%.1f", kills * 0.1f + power * 0.05f));
        }

        // 警告
        if (kills > 0) {
            long timeSinceKill = System.currentTimeMillis() - data.getLong("LastKillTime");
            if (timeSinceKill > 10000) {
                tooltip.add("");
                tooltip.add("§4§l⚠ 深渊渴望鲜血！");
            }
        }
    }

    private static void addTemporalTooltip(List<String> tooltip, NBTTagCompound data, EntityPlayer player) {
        int rewinds = data.getInteger("RewindCount");
        float energy = data.getFloat("TemporalEnergy");
        long lastTimeStop = data.getLong("LastTimeStop");

        tooltip.add("§b▸ 时间能量: §f" + String.format("%.0f/100", energy));
        tooltip.add("§b▸ 回溯次数: §f" + rewinds);

        // 时停冷却
        long timeSinceStop = System.currentTimeMillis() - lastTimeStop;
        if (timeSinceStop < 30000) {
            int cooldown = (int)((30000 - timeSinceStop) / 1000);
            tooltip.add("§b▸ 时停冷却: §c" + cooldown + "秒");
        } else {
            tooltip.add("§b▸ 时停冷却: §a就绪");
        }

        // 当前效果
        tooltip.add("");
        tooltip.add("§b当前效果:");
        if (player != null) {
            int count = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.TEMPORAL);
            tooltip.add("  §7• 时停触发率 §b" + (count * 8) + "%");
            tooltip.add("  §7• 时停持续 §b" + (count * 5) + "秒");
            tooltip.add("  §7• 时间加速 §b+100%移速/攻速 急迫XI");

            // 回溯概率
            float baseChance = Math.min(count * 0.25f, 0.75f);
            float actualChance = baseChance * (float)Math.pow(0.7, rewinds);
            tooltip.add("  §7• 致命回溯 §b" + String.format("%.1f%%", actualChance * 100));
        }

        if (rewinds > 2) {
            tooltip.add("");
            tooltip.add("§c§l⚠ 时间线紊乱！回溯概率大幅下降");
        }
    }

    private static void addSpatialTooltip(List<String> tooltip, NBTTagCompound data, EntityPlayer player) {
        float stored = data.getFloat("StoredDamage");
        float energy = data.getFloat("DimensionalEnergy");
        int phaseStrikes = data.getInteger("PhaseStrikeCount");
        long lastCollapse = data.getLong("LastCollapseTime");

        // 基础数值
        tooltip.add("§d▸ 维度能量: §f" + String.format("%.0f/100", energy));
        tooltip.add("§d▸ 存储伤害: §f" + String.format("%.1f", stored) + getStorageIndicator(stored));
        tooltip.add("§d▸ 相位打击: §f" + phaseStrikes + "次");

        // 崩塌领域状态
        if (player != null) {
            int count = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.SPATIAL);

            if (count < 3) {
                tooltip.add("§d▸ 崩塌领域: §c需要3件套");
            } else {
                long timeSinceCollapse = System.currentTimeMillis() - lastCollapse;
                if (timeSinceCollapse < 10000) {
                    int cooldown = (int)((10000 - timeSinceCollapse) / 1000);
                    tooltip.add("§d▸ 崩塌领域: §c" + cooldown + "秒");
                } else if (stored >= 30) {
                    tooltip.add("§d▸ 崩塌领域: §a§l就绪 §7(Shift+右键)");
                } else {
                    tooltip.add("§d▸ 崩塌领域: §e需要30存储");
                }
            }
        }

        // 当前效果
        tooltip.add("");
        tooltip.add("§d当前效果:");
        if (player != null) {
            int count = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.SPATIAL);

            // 相位打击
            tooltip.add("  §7• 相位打击 §d" + (count * 15) + "%几率");

            // 维度口袋
            if (count >= 2) {
                float storageRatio = 30 + (count - 2) * 20;
                tooltip.add("  §7• 维度口袋 §d" + String.format("%.0f%%吸收", storageRatio));
            }

            // 时空之壁
            if (count >= 4) {
                tooltip.add("  §7• 时空之壁 §d80%触发");
            }

            // 暴击增幅
            if (stored > 0) {
                tooltip.add("  §7• 暴击增幅 §d+" + String.format("%.0f%%", Math.min(stored, 200) / 1));
            }

            // 能量恢复
            tooltip.add("  §7• 能量恢复 §a5/秒");
        }

        if (stored > 100) {
            tooltip.add("");
            tooltip.add("§d§l⚡ 维度超载！");
        }
    }

    private static void addOtherworldTooltip(List<String> tooltip, NBTTagCompound data, EntityPlayer player) {
        int insight = data.getInteger("Insight");
        int sanity = data.getInteger("Sanity");
        int forbiddenKnowledge = data.getInteger("ForbiddenKnowledge");
        int abyssGazeStacks = data.getInteger("AbyssGazeStacks");

        // 基础状态
        tooltip.add("§5▸ 灵视: " + getInsightBar(insight));
        tooltip.add("§5▸ 理智: " + getSanityBar(sanity));

        if (forbiddenKnowledge > 0) {
            tooltip.add("§5▸ 禁忌知识: §f" + forbiddenKnowledge);
        }

        if (abyssGazeStacks > 0) {
            tooltip.add("§5▸ 深渊凝视: §c§l" + abyssGazeStacks + "层");
        }

        // 当前增益
        tooltip.add("");
        tooltip.add("§5当前增益:");

        if (player != null) {
            int count = FabricWeavingSystem.countPlayerFabric(player, UpdatedFabricPlayerData.FabricType.OTHERWORLD);

            // 基础攻击倍率
            float baseMultiplier = 1.0f + count * 0.25f;
            tooltip.add("  §7• 基础增伤 §5×" + String.format("%.1f", baseMultiplier));

            // 总攻击倍率
            float totalMultiplier = baseMultiplier;
            totalMultiplier += (insight / 10f) * 0.1f;
            totalMultiplier += forbiddenKnowledge * 0.05f;
            totalMultiplier += abyssGazeStacks * 0.15f;

            if (sanity < 50) {
                totalMultiplier += ((50f - sanity) / 50f) * 0.5f;
            }

            if (totalMultiplier > baseMultiplier) {
                tooltip.add("  §7• 总计倍率 §c×" + String.format("%.1f", totalMultiplier));
            }

            // 暴击倍率
            if (abyssGazeStacks > 0) {
                float critMultiplier = 1.5f + abyssGazeStacks * 0.5f;
                critMultiplier += (insight / 20f) * 0.2f;
                if (sanity < 50) {
                    critMultiplier += ((50f - sanity) / 50f) * 1.0f;
                }
                tooltip.add("  §7• 暴击倍率 §6×" + String.format("%.1f", critMultiplier));

                // 理论极限
                float theoretical = totalMultiplier * critMultiplier;
                if (theoretical > 10) {
                    tooltip.add("  §7• 理论输出 §4§l×" + String.format("%.0f", theoretical));
                }
            }
        }

        // 反噬风险
        tooltip.add("");
        tooltip.add("§c反噬风险:");

        // 计算每秒损失
        float dps = 0;

        if (insight > 70) {
            float erosion = ((insight - 70) / 10.0f) * 2 / 5;
            dps += erosion;
            tooltip.add("  §7• 灵视侵蚀 §c-" + String.format("%.1f", erosion) + "HP/秒");
        }

        if (sanity < 20) {
            float collapse = ((20 - sanity) / 5.0f) * 5 * 0.3f / 5;
            dps += collapse;
            tooltip.add("  §7• 理智崩溃 §c-" + String.format("%.1f", collapse) + "HP/秒");
        }

        if (insight > 90 && sanity < 10) {
            float gaze = 20 * 0.1f / 5;
            dps += gaze;
            tooltip.add("  §7• §0深渊凝视 §c-" + String.format("%.1f", gaze) + "HP/秒");
        }

        // 生存时间估算
        if (dps > 0 && player != null) {
            int survivalTime = (int)(player.getMaxHealth() / dps);
            tooltip.add("  §4• 预计存活 §c" + survivalTime + "秒");
        }

        // 警告
        if (sanity < 30) {
            tooltip.add("");
            tooltip.add(getSanityWarning(sanity));
        }

        if (dps > 3) {
            tooltip.add("§4§l⚠ 极度危险！反噬将致命！");
        }
    }

    private static void addSetBonus(List<String> tooltip, UpdatedFabricPlayerData.FabricType type, int count) {
        switch (type) {
            case ABYSS:
                if (count >= 1) tooltip.add("  §61件: §7击杀+50%攻击 生命窃取");
                if (count >= 2) tooltip.add("  §62件: §7增强生命窃取");
                if (count >= 3) tooltip.add("  §63件: §7溅射伤害30%");
                if (count >= 4) tooltip.add("  §64件: §c血之狂怒");
                break;

            case TEMPORAL:
                if (count >= 1) tooltip.add("  §61件: §7时停8% 5秒 时间加速");
                if (count >= 2) tooltip.add("  §62件: §7时停16% 10秒");
                if (count >= 3) tooltip.add("  §63件: §7时停24% 15秒");
                if (count >= 4) tooltip.add("  §64件: §b时停32% 20秒 20格范围");
                tooltip.add("  §7致命伤害触发时序回溯");
                break;

            case SPATIAL:
                if (count >= 1) tooltip.add("  §61件: §7相位打击15%");
                if (count >= 2) tooltip.add("  §62件: §7维度口袋30%");
                if (count >= 3) tooltip.add("  §63件: §d维度崩塌解锁");
                if (count >= 4) tooltip.add("  §64件: §d时空之壁80%");
                break;

            case OTHERWORLD:
                if (count >= 1) tooltip.add("  §61件: §7攻击×1.25");
                if (count >= 2) tooltip.add("  §62件: §7攻击×1.5");
                if (count >= 3) tooltip.add("  §63件: §7攻击×1.75");
                if (count >= 4) tooltip.add("  §64件: §5攻击×2.0 极限×51");
                break;
        }
    }

    private static String getWeaveTime(NBTTagCompound data) {
        if (!data.hasKey("WeaveTime")) return "未知";

        long time = System.currentTimeMillis() - data.getLong("WeaveTime");
        long hours = time / 3600000;
        if (hours > 0) return hours + "小时前";
        long minutes = time / 60000;
        if (minutes > 0) return minutes + "分钟前";
        return "刚刚";
    }

    private static String getPowerBar(int power) {
        int filled = power / 10;
        StringBuilder bar = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                bar.append("§a█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append(" §f").append(power).append("%");
        return bar.toString();
    }

    private static String getStorageIndicator(float stored) {
        if (stored >= 500) return " §5§l⟐⟐⟐";
        if (stored >= 300) return " §d§l⟐⟐";
        if (stored >= 150) return " §d⟐";
        if (stored >= 50) return " §b◈";
        return "";
    }

    private static String getInsightBar(int insight) {
        int level = insight / 20;
        String color = "§5";
        if (insight >= 80) color = "§4";
        else if (insight >= 60) color = "§c";
        else if (insight >= 40) color = "§6";
        else if (insight >= 20) color = "§e";

        StringBuilder bar = new StringBuilder(color + insight + "/100 ");
        for (int i = 0; i < Math.min(level, 5); i++) {
            bar.append("☉");
        }
        return bar.toString();
    }

    private static String getSanityBar(int sanity) {
        String color = "§a";
        if (sanity <= 30) color = "§c";
        else if (sanity <= 50) color = "§e";
        else if (sanity <= 70) color = "§6";

        return color + sanity + "/100 " + getStabilityIndicator(sanity);
    }

    private static String getStabilityIndicator(int sanity) {
        if (sanity >= 80) return "§a◆◆◆◆◆";
        if (sanity >= 60) return "§e◆◆◆◆◇";
        if (sanity >= 40) return "§6◆◆◆◇◇";
        if (sanity >= 20) return "§c◆◆◇◇◇";
        return "§4◆◇◇◇◇";
    }

    private static String getSanityWarning(int sanity) {
        if (sanity <= 10) return "§4§l⚠ 理智崩溃！";
        if (sanity <= 20) return "§c§l⚠ 严重疯狂！";
        return "§e§l⚠ 精神不稳！";
    }
}