package com.moremod.item;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.moremod.creativetab.moremodCreativeTab;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 行為分析晶片 - 根據玩家行為動態調整增益
 */
public class ItemBehaviorAnalysisChip extends Item implements IBauble {

    // ===== 配置 =====
    public static final int ANALYSIS_INTERVAL = 72000; // 1小時 = 72000 ticks
    public static final int PREDICT_MODE_HOURS = 100;
    
    // ===== 玩家類型 =====
    public enum PlayerType {
        COMBAT("戰鬥型", TextFormatting.RED, "擅長戰鬥"),
        BUILDER("建築型", TextFormatting.GOLD, "擅長建造"),
        MINER("採礦型", TextFormatting.GRAY, "擅長挖掘"),
        EXPLORER("探索型", TextFormatting.GREEN, "擅長探索"),
        FARMER("農業型", TextFormatting.YELLOW, "擅長種植"),
        BALANCED("均衡型", TextFormatting.AQUA, "平衡發展");
        
        private final String displayName;
        private final TextFormatting color;
        private final String description;
        
        PlayerType(String displayName, TextFormatting color, String description) {
            this.displayName = displayName;
            this.color = color;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public TextFormatting getColor() { return color; }
        public String getDescription() { return description; }
    }
    
    // ===== NBT Keys =====
    private static final String NBT_LAST_ANALYSIS = "LastAnalysisTime";
    private static final String NBT_PLAYER_TYPE = "PlayerType";
    private static final String NBT_EQUIP_TIME = "EquipTime"; // 裝備總時長（ticks）
    private static final String NBT_PREDICT_MODE = "PredictMode";
    
    // ===== 屬性修改器 UUID =====
    private static final UUID COMBAT_DAMAGE_UUID = UUID.fromString("ba8f3c86-2f5d-4a9d-8f4b-89dbab2a889a");
    private static final UUID COMBAT_ATTACK_SPEED_UUID = UUID.fromString("ba8f3c86-2f5d-4a9d-8f4b-89dbab2a889b");
    private static final UUID EXPLORER_SPEED_UUID = UUID.fromString("ba8f3c86-2f5d-4a9d-8f4b-89dbab2a889c");
    private static final UUID MINER_EFFICIENCY_UUID = UUID.fromString("ba8f3c86-2f5d-4a9d-8f4b-89dbab2a889d");

    public ItemBehaviorAnalysisChip() {
        setRegistryName("behavior_analysis_chip");
        setTranslationKey("behavior_analysis_chip");
        setCreativeTab(moremodCreativeTab.moremod_TAB);
        setMaxStackSize(1);
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return BaubleType.TRINKET;
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer) || wearer.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) wearer;
        
        // 初始化 NBT
        NBTTagCompound nbt = getOrCreateNBT(stack);
        if (!nbt.hasKey(NBT_LAST_ANALYSIS)) {
            nbt.setLong(NBT_LAST_ANALYSIS, player.world.getTotalWorldTime());
        }
        if (!nbt.hasKey(NBT_PLAYER_TYPE)) {
            nbt.setString(NBT_PLAYER_TYPE, PlayerType.BALANCED.name());
        }
        if (!nbt.hasKey(NBT_EQUIP_TIME)) {
            nbt.setLong(NBT_EQUIP_TIME, 0L);
        }
        
        // 執行初始分析
        analyzeAndApply(player, stack);
        
        player.sendMessage(new TextComponentString(
            TextFormatting.AQUA + "✦ 行為分析晶片已啟動，開始收集數據..."));
    }

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase wearer) {
        if (wearer instanceof EntityPlayer && !wearer.world.isRemote) {
            EntityPlayer player = (EntityPlayer) wearer;
            removeAllBuffs(player);
            player.sendStatusMessage(new TextComponentString(
                TextFormatting.GRAY + "✦ 行為分析晶片已停用"), true);
        }
    }

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase wearer) {
        if (!(wearer instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) wearer;
        if (player.world.isRemote) return;
        
        long currentTime = player.world.getTotalWorldTime();
        NBTTagCompound nbt = getOrCreateNBT(stack);
        
        // 增加裝備時長
        long equipTime = nbt.getLong(NBT_EQUIP_TIME);
        nbt.setLong(NBT_EQUIP_TIME, equipTime + 1);
        
        // 檢查是否解鎖預測模式（100小時 = 7,200,000 ticks）
        if (!nbt.getBoolean(NBT_PREDICT_MODE) && equipTime >= PREDICT_MODE_HOURS * 72000L) {
            nbt.setBoolean(NBT_PREDICT_MODE, true);
            player.sendMessage(new TextComponentString(
                TextFormatting.LIGHT_PURPLE + "✦✦✦ 預測模式已解鎖！✦✦✦"));
            player.sendMessage(new TextComponentString(
                TextFormatting.GRAY + "晶片現在可以預測你的行為並提前啟動對應模組"));
        }
        
        // 每秒檢查一次
        if (currentTime % 20 != 0) return;
        
        // 檢查是否需要重新分析（每小時）
        long lastAnalysis = nbt.getLong(NBT_LAST_ANALYSIS);
        if (currentTime - lastAnalysis >= ANALYSIS_INTERVAL) {
            analyzeAndApply(player, stack);
            nbt.setLong(NBT_LAST_ANALYSIS, currentTime);
            
            PlayerType type = PlayerType.valueOf(nbt.getString(NBT_PLAYER_TYPE));
            player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + "✦ 行為分析完成：" + 
                type.getColor() + type.getDisplayName()));
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) return false;
        return nbt.getBoolean(NBT_PREDICT_MODE);
    }

    // ===== 核心分析系統 =====
    private void analyzeAndApply(EntityPlayer player, ItemStack stack) {
        // 獲取玩家行為數據
        BehaviorData data = BehaviorDataTracker.getData(player);
        
        // 分析玩家類型
        PlayerType type = analyzePlayerType(data);
        
        // 保存類型
        NBTTagCompound nbt = getOrCreateNBT(stack);
        nbt.setString(NBT_PLAYER_TYPE, type.name());
        
        // 移除舊增益
        removeAllBuffs(player);
        
        // 應用新增益
        applyBuffs(player, type, nbt.getBoolean(NBT_PREDICT_MODE));
    }
    
    private PlayerType analyzePlayerType(BehaviorData data) {
        // 計算各類型分數
        double combatScore = data.getMobKills() * 2 + data.getPlayerKills() * 5 + data.getDamageTaken() * 0.1;
        double builderScore = data.getBlocksPlaced() * 0.5 + data.getCraftingCount() * 1.0;
        double minerScore = data.getBlocksMined() * 0.8 + data.getOresMined() * 2.0;
        double explorerScore = data.getDistanceTraveled() * 0.01 + data.getDimensionChanges() * 10;
        double farmerScore = data.getCropsHarvested() * 1.5 + data.getAnimalsBreed() * 2.0;
        
        // 找出最高分
        double maxScore = Math.max(combatScore, 
                         Math.max(builderScore, 
                         Math.max(minerScore, 
                         Math.max(explorerScore, farmerScore))));
        
        // 檢查是否均衡（最高分與次高分相差不大）
        double[] scores = {combatScore, builderScore, minerScore, explorerScore, farmerScore};
        java.util.Arrays.sort(scores);
        if (scores[4] - scores[3] < scores[4] * 0.2) {
            return PlayerType.BALANCED;
        }
        
        // 返回最高分對應的類型
        if (maxScore == combatScore) return PlayerType.COMBAT;
        if (maxScore == builderScore) return PlayerType.BUILDER;
        if (maxScore == minerScore) return PlayerType.MINER;
        if (maxScore == explorerScore) return PlayerType.EXPLORER;
        if (maxScore == farmerScore) return PlayerType.FARMER;
        
        return PlayerType.BALANCED;
    }
    
    private void applyBuffs(EntityPlayer player, PlayerType type, boolean predictMode) {
        double multiplier = predictMode ? 1.5 : 1.0; // 預測模式增益提升50%
        
        switch (type) {
            case COMBAT:
                applyCombatBuffs(player, multiplier);
                break;
            case BUILDER:
                // 建築型增益通過事件處理（見 EventHandler）
                break;
            case MINER:
                applyMinerBuffs(player, multiplier);
                break;
            case EXPLORER:
                applyExplorerBuffs(player, multiplier);
                break;
            case FARMER:
                // 農業型增益通過事件處理
                break;
            case BALANCED:
                applyBalancedBuffs(player, multiplier);
                break;
        }
    }
    
    private void applyCombatBuffs(EntityPlayer player, double multiplier) {
        // 攻擊力 +20%
        IAttributeInstance damage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damage != null) {
            AttributeModifier mod = new AttributeModifier(
                COMBAT_DAMAGE_UUID, 
                "BehaviorChipCombatDamage", 
                0.2 * multiplier, 
                2 // 乘法 (1 + x)
            ).setSaved(false);
            damage.applyModifier(mod);
        }
        
        // 攻擊速度 +15%
        IAttributeInstance attackSpeed = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED);
        if (attackSpeed != null) {
            AttributeModifier mod = new AttributeModifier(
                COMBAT_ATTACK_SPEED_UUID, 
                "BehaviorChipAttackSpeed", 
                0.15 * multiplier, 
                2
            ).setSaved(false);
            attackSpeed.applyModifier(mod);
        }
    }
    
    private void applyExplorerBuffs(EntityPlayer player, double multiplier) {
        // 移動速度 +25%
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            AttributeModifier mod = new AttributeModifier(
                EXPLORER_SPEED_UUID, 
                "BehaviorChipExplorerSpeed", 
                0.25 * multiplier, 
                2
            ).setSaved(false);
            speed.applyModifier(mod);
        }
    }
    
    private void applyMinerBuffs(EntityPlayer player, double multiplier) {
        // 挖掘效率通過事件處理
        // 這裡可以添加其他屬性，如幸運值
    }
    
    private void applyBalancedBuffs(EntityPlayer player, double multiplier) {
        // 均衡型：所有屬性小幅提升
        IAttributeInstance damage = player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damage != null) {
            AttributeModifier mod = new AttributeModifier(
                COMBAT_DAMAGE_UUID, 
                "BehaviorChipBalanced", 
                0.1 * multiplier, 
                2
            ).setSaved(false);
            damage.applyModifier(mod);
        }
    }
    
    private void removeAllBuffs(EntityPlayer player) {
        removeModifier(player, SharedMonsterAttributes.ATTACK_DAMAGE, COMBAT_DAMAGE_UUID);
        removeModifier(player, SharedMonsterAttributes.ATTACK_SPEED, COMBAT_ATTACK_SPEED_UUID);
        removeModifier(player, SharedMonsterAttributes.MOVEMENT_SPEED, EXPLORER_SPEED_UUID);
    }
    
    private void removeModifier(EntityPlayer player, net.minecraft.entity.ai.attributes.IAttribute attribute, UUID uuid) {
        IAttributeInstance attr = player.getEntityAttribute(attribute);
        if (attr != null) {
            AttributeModifier mod = attr.getModifier(uuid);
            if (mod != null) {
                attr.removeModifier(mod);
            }
        }
    }

    // ===== Tooltip =====
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tip, ITooltipFlag flag) {
        tip.add("");
        tip.add(TextFormatting.AQUA + "═══ 行為分析晶片 ═══");
        tip.add(TextFormatting.GRAY + "飾品類型: " + TextFormatting.WHITE + "飾品");
        tip.add("");
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt != null && nbt.hasKey(NBT_PLAYER_TYPE)) {
            PlayerType type = PlayerType.valueOf(nbt.getString(NBT_PLAYER_TYPE));
            tip.add(TextFormatting.YELLOW + "當前類型: " + type.getColor() + type.getDisplayName());
            tip.add(TextFormatting.GRAY + "  " + type.getDescription());
            tip.add("");
            
            // 顯示增益
            tip.add(TextFormatting.GREEN + "個性化增益:");
            switch (type) {
                case COMBAT:
                    tip.add(TextFormatting.GRAY + "  • 攻擊力 +20%");
                    tip.add(TextFormatting.GRAY + "  • 攻擊速度 +15%");
                    break;
                case EXPLORER:
                    tip.add(TextFormatting.GRAY + "  • 移動速度 +25%");
                    tip.add(TextFormatting.GRAY + "  • 夜視效果");
                    break;
                case MINER:
                    tip.add(TextFormatting.GRAY + "  • 挖掘速度 +30%");
                    tip.add(TextFormatting.GRAY + "  • 幸運 +1");
                    break;
                case BUILDER:
                    tip.add(TextFormatting.GRAY + "  • 放置速度 +25%");
                    tip.add(TextFormatting.GRAY + "  • 建築材料節省 10%");
                    break;
                case FARMER:
                    tip.add(TextFormatting.GRAY + "  • 作物生長速度 +50%");
                    tip.add(TextFormatting.GRAY + "  • 動物繁殖冷卻 -30%");
                    break;
                case BALANCED:
                    tip.add(TextFormatting.GRAY + "  • 全屬性 +10%");
                    break;
            }
            
            // 裝備時長
            long equipTime = nbt.getLong(NBT_EQUIP_TIME);
            long hours = equipTime / 72000L;
            tip.add("");
            tip.add(TextFormatting.AQUA + "裝備時長: " + TextFormatting.WHITE + hours + " 小時");
            
            // 預測模式
            if (nbt.getBoolean(NBT_PREDICT_MODE)) {
                tip.add("");
                tip.add(TextFormatting.LIGHT_PURPLE + "✦ 預測模式已啟動 ✦");
                tip.add(TextFormatting.GRAY + "增益效果 +50%");
            } else {
                long remaining = PREDICT_MODE_HOURS - hours;
                if (remaining > 0) {
                    tip.add(TextFormatting.DARK_GRAY + "預測模式解鎖: " + remaining + " 小時");
                }
            }
        } else {
            tip.add(TextFormatting.YELLOW + "尚未分析");
            tip.add(TextFormatting.GRAY + "裝備後開始收集數據");
        }
        
        tip.add("");
        tip.add(TextFormatting.DARK_PURPLE + "按住 Shift 查看詳情");
        
        if (GuiScreen.isShiftKeyDown()) {
            tip.add("");
            tip.add(TextFormatting.GOLD + "說明:");
            tip.add(TextFormatting.GRAY + "• 自動追蹤你的遊戲行為");
            tip.add(TextFormatting.GRAY + "• 每小時重新分析並調整增益");
            tip.add(TextFormatting.GRAY + "• 改變遊玩風格會改變增益類型");
            tip.add(TextFormatting.GRAY + "• 100 小時後解鎖預測模式");
            tip.add("");
            tip.add(TextFormatting.DARK_GRAY + "了解你，才能幫助你");
        }
    }
    
    // ===== 右鍵快速裝備 =====
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (world.isRemote) return new ActionResult<>(EnumActionResult.PASS, held);

        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles != null) {
            for (int i = 0; i < baubles.getSlots(); i++) {
                if (baubles.getStackInSlot(i).isEmpty() &&
                        baubles.isItemValidForSlot(i, held, player)) {
                    
                    baubles.setStackInSlot(i, held.splitStack(1));
                    player.sendStatusMessage(new TextComponentString(
                        TextFormatting.AQUA + "✦ 已裝備行為分析晶片"
                    ), true);
                    return new ActionResult<>(EnumActionResult.SUCCESS, held);
                }
            }
        }
        return new ActionResult<>(EnumActionResult.PASS, held);
    }
    
    // ===== Helper =====
    private NBTTagCompound getOrCreateNBT(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }
    
    // ===== 工具方法：檢查玩家是否裝備此晶片 =====
    public static boolean hasChipEquipped(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return false;
        
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBehaviorAnalysisChip) {
                return true;
            }
        }
        return false;
    }
    
    public static PlayerType getPlayerType(EntityPlayer player) {
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) return PlayerType.BALANCED;
        
        for (int i = 0; i < baubles.getSlots(); i++) {
            ItemStack stack = baubles.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemBehaviorAnalysisChip) {
                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt != null && nbt.hasKey(NBT_PLAYER_TYPE)) {
                    return PlayerType.valueOf(nbt.getString(NBT_PLAYER_TYPE));
                }
            }
        }
        return PlayerType.BALANCED;
    }
}
