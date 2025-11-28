package com.moremod.synergy.item;

import com.moremod.creativetab.moremodCreativeTab;
import com.moremod.synergy.core.SynergyDefinition;
import com.moremod.synergy.core.SynergyEventType;
import com.moremod.synergy.core.SynergyManager;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
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

import javax.annotation.Nullable;
import java.util.*;

/**
 * 协同效应手册 - Synergy Guide
 *
 * 一本记录所有协同效应组合的手册。
 * 右键打开查看所有可用的 Synergy 组合。
 * Shift+右键切换类别。
 */
public class ItemSynergyGuide extends Item {

    // 协同效应分类
    private static final String[] CATEGORIES = {
            "combat",      // 战斗类
            "energy",      // 能量类
            "survival",    // 生存类
            "spatial",     // 空间类
            "temporal",    // 时间类
            "entity",      // 实体类
            "domain",      // 领域类
            "mechanism",   // 机制类
            "all"          // 全部
    };

    private static final Map<String, String> CATEGORY_NAMES = new HashMap<>();
    private static final Map<String, TextFormatting> CATEGORY_COLORS = new HashMap<>();

    static {
        CATEGORY_NAMES.put("combat", "战斗协同");
        CATEGORY_NAMES.put("energy", "能量协同");
        CATEGORY_NAMES.put("survival", "生存协同");
        CATEGORY_NAMES.put("spatial", "空间协同");
        CATEGORY_NAMES.put("temporal", "时间协同");
        CATEGORY_NAMES.put("entity", "实体协同");
        CATEGORY_NAMES.put("domain", "领域协同");
        CATEGORY_NAMES.put("mechanism", "机制协同");
        CATEGORY_NAMES.put("all", "全部协同");

        CATEGORY_COLORS.put("combat", TextFormatting.RED);
        CATEGORY_COLORS.put("energy", TextFormatting.YELLOW);
        CATEGORY_COLORS.put("survival", TextFormatting.GREEN);
        CATEGORY_COLORS.put("spatial", TextFormatting.LIGHT_PURPLE);
        CATEGORY_COLORS.put("temporal", TextFormatting.AQUA);
        CATEGORY_COLORS.put("entity", TextFormatting.GOLD);
        CATEGORY_COLORS.put("domain", TextFormatting.DARK_PURPLE);
        CATEGORY_COLORS.put("mechanism", TextFormatting.BLUE);
        CATEGORY_COLORS.put("all", TextFormatting.WHITE);
    }

    public ItemSynergyGuide() {
        this.setRegistryName("synergy_guide");
        this.setTranslationKey("moremod.synergy_guide");
        this.setCreativeTab(moremodCreativeTab.moremod_TAB);
        this.setMaxStackSize(1);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        int categoryIndex = nbt.getInteger("category");
        int pageIndex = nbt.getInteger("page");

        if (player.isSneaking()) {
            // Shift+右键: 切换类别
            categoryIndex = (categoryIndex + 1) % CATEGORIES.length;
            pageIndex = 0;
            nbt.setInteger("category", categoryIndex);
            nbt.setInteger("page", pageIndex);

            String category = CATEGORIES[categoryIndex];
            TextFormatting color = CATEGORY_COLORS.getOrDefault(category, TextFormatting.WHITE);
            String categoryName = CATEGORY_NAMES.getOrDefault(category, category);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "═══════════════════════════════"));
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "【协同效应手册】" + color + " " + categoryName));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "═══════════════════════════════"));

        } else {
            // 右键: 显示当前类别的 Synergy 列表
            String category = CATEGORIES[categoryIndex];
            List<SynergyDefinition> synergies = getSynergiesForCategory(category);

            if (synergies.isEmpty()) {
                player.sendMessage(new TextComponentString(
                        TextFormatting.GRAY + "该类别暂无协同效应"));
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }

            // 分页显示（每页5个）
            int pageSize = 5;
            int totalPages = (synergies.size() + pageSize - 1) / pageSize;
            pageIndex = pageIndex % totalPages;
            nbt.setInteger("page", (pageIndex + 1) % totalPages);

            int startIndex = pageIndex * pageSize;
            int endIndex = Math.min(startIndex + pageSize, synergies.size());

            TextFormatting color = CATEGORY_COLORS.getOrDefault(category, TextFormatting.WHITE);
            String categoryName = CATEGORY_NAMES.getOrDefault(category, category);

            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "════════════════════════════════════════"));
            player.sendMessage(new TextComponentString(
                    TextFormatting.AQUA + "【协同效应手册】" + color + " " + categoryName +
                    TextFormatting.GRAY + " [" + (pageIndex + 1) + "/" + totalPages + "]"));
            player.sendMessage(new TextComponentString(
                    TextFormatting.GOLD + "════════════════════════════════════════"));

            for (int i = startIndex; i < endIndex; i++) {
                SynergyDefinition synergy = synergies.get(i);
                displaySynergyInfo(player, synergy, color);
            }

            player.sendMessage(new TextComponentString(
                    TextFormatting.DARK_GRAY + "右键翻页 | Shift+右键切换类别"));
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    /**
     * 显示单个 Synergy 的详细信息
     */
    private void displaySynergyInfo(EntityPlayer player, SynergyDefinition synergy, TextFormatting categoryColor) {
        player.sendMessage(new TextComponentString(""));

        // 名称
        player.sendMessage(new TextComponentString(
                categoryColor + "◆ " + TextFormatting.BOLD + synergy.getDisplayName() +
                TextFormatting.RESET + TextFormatting.GRAY + " [" + synergy.getId() + "]"));

        // 描述
        player.sendMessage(new TextComponentString(
                TextFormatting.WHITE + "  " + synergy.getDescription()));

        // 所需模块
        StringBuilder modules = new StringBuilder();
        modules.append(TextFormatting.YELLOW).append("  模块: ");
        List<String> requiredModules = synergy.getRequiredModules();
        for (int j = 0; j < requiredModules.size(); j++) {
            if (j > 0) modules.append(TextFormatting.GRAY).append(" + ");
            modules.append(TextFormatting.AQUA).append(formatModuleName(requiredModules.get(j)));
        }
        player.sendMessage(new TextComponentString(modules.toString()));

        // 触发事件
        StringBuilder triggers = new StringBuilder();
        triggers.append(TextFormatting.LIGHT_PURPLE).append("  触发: ");
        Set<SynergyEventType> triggerEvents = synergy.getTriggerEvents();
        boolean first = true;
        for (SynergyEventType event : triggerEvents) {
            if (!first) triggers.append(TextFormatting.GRAY).append(", ");
            triggers.append(TextFormatting.GREEN).append(getEventDisplayName(event));
            first = false;
        }
        player.sendMessage(new TextComponentString(triggers.toString()));

        // 优先级
        player.sendMessage(new TextComponentString(
                TextFormatting.DARK_GRAY + "  优先级: " + synergy.getPriority()));
    }

    /**
     * 获取指定类别的所有 Synergy
     */
    private List<SynergyDefinition> getSynergiesForCategory(String category) {
        SynergyManager manager = SynergyManager.getInstance();
        Collection<SynergyDefinition> allSynergies = manager.getAllSynergies();

        if (category.equals("all")) {
            return new ArrayList<>(allSynergies);
        }

        List<SynergyDefinition> filtered = new ArrayList<>();
        for (SynergyDefinition synergy : allSynergies) {
            String id = synergy.getId().toLowerCase();
            if (matchesCategory(id, category)) {
                filtered.add(synergy);
            }
        }
        return filtered;
    }

    /**
     * 检查 Synergy ID 是否属于某个类别
     */
    private boolean matchesCategory(String id, String category) {
        switch (category) {
            case "combat":
                return id.contains("berserker") || id.contains("hunter") || id.contains("iron_wall") ||
                       id.contains("offense") || id.contains("attack") || id.contains("damage");
            case "energy":
                return id.contains("energy") || id.contains("generator") || id.contains("perpetual") ||
                       id.contains("combat_generator") || id.contains("overload");
            case "survival":
                return id.contains("survival") || id.contains("phoenix") || id.contains("self_sufficient") ||
                       id.contains("regen") || id.contains("heal");
            case "spatial":
                return id.contains("flight") || id.contains("miner") || id.contains("extreme") ||
                       id.contains("spatial") || id.contains("phase");
            case "temporal":
                return id.contains("temporal") || id.contains("time") || id.contains("xp_master") ||
                       id.contains("fracture") || id.contains("echo");
            case "entity":
                return id.contains("area") || id.contains("void_harvest") || id.contains("speed_demon") ||
                       id.contains("entity") || id.contains("aura");
            case "domain":
                return id.contains("ultimate") || id.contains("domain") || id.contains("defense");
            case "mechanism":
                return id.contains("momentum") || id.contains("gravity") || id.contains("soul") ||
                       id.contains("chain") || id.contains("mechanism");
            default:
                return false;
        }
    }

    /**
     * 格式化模块名称（UPPER_CASE → 中文/友好名称）
     */
    private String formatModuleName(String moduleId) {
        // 简单转换，可以扩展为完整的本地化
        String[] parts = moduleId.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(part.substring(0, 1).toUpperCase()).append(part.substring(1));
        }
        return sb.toString();
    }

    /**
     * 获取事件类型的显示名称
     */
    private String getEventDisplayName(SynergyEventType event) {
        switch (event) {
            case TICK: return "每刻";
            case ATTACK: return "攻击";
            case HURT: return "受伤";
            case KILL: return "击杀";
            case DEATH: return "死亡";
            case ENERGY_CONSUME: return "消耗能量";
            case ENERGY_RECHARGE: return "充能";
            case ENERGY_FULL: return "满能量";
            case ENERGY_LOW: return "低能量";
            case CRITICAL_HIT: return "暴击";
            case ENVIRONMENTAL_DAMAGE: return "环境伤害";
            case LOW_HEALTH: return "低血量";
            case FATAL_DAMAGE: return "致命伤害";
            case SPRINT: return "疾跑";
            case JUMP: return "跳跃";
            case SNEAK: return "潜行";
            case ANY: return "任意";
            default: return event.name();
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        tooltip.add(TextFormatting.LIGHT_PURPLE + "『协同效应完全指南』");
        tooltip.add("");
        tooltip.add(TextFormatting.GRAY + "记录了所有已知的模块协同组合。");
        tooltip.add("");
        tooltip.add(TextFormatting.YELLOW + "◆ " + TextFormatting.WHITE + "右键: 查看协同列表");
        tooltip.add(TextFormatting.YELLOW + "◆ " + TextFormatting.WHITE + "Shift+右键: 切换类别");
        tooltip.add("");

        // 显示当前类别
        NBTTagCompound nbt = stack.getTagCompound();
        int categoryIndex = nbt != null ? nbt.getInteger("category") : 0;
        String category = CATEGORIES[categoryIndex % CATEGORIES.length];
        TextFormatting color = CATEGORY_COLORS.getOrDefault(category, TextFormatting.WHITE);
        String categoryName = CATEGORY_NAMES.getOrDefault(category, category);

        tooltip.add(TextFormatting.AQUA + "当前类别: " + color + categoryName);

        // 显示统计
        SynergyManager manager = SynergyManager.getInstance();
        if (manager != null) {
            int total = manager.getAllSynergies().size();
            tooltip.add(TextFormatting.DARK_GRAY + "共计 " + total + " 种协同效应");
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // 附魔光效
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.EPIC;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return TextFormatting.LIGHT_PURPLE + "" + TextFormatting.BOLD +
               super.getItemStackDisplayName(stack);
    }
}
