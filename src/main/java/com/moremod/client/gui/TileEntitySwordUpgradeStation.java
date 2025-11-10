package com.moremod.client.gui;

import com.moremod.config.SwordMaterialData;
import com.moremod.config.UpgradeConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 剑升级站 TileEntity - 双向铁砧式系统
 * 
 * ==================== 坐标常量（第四组基准）====================
 * GUI尺寸: 256x256
 * 
 * 槽位布局：
 *  槽位 0 = OUTPUT槽 @ (122, 58)
 *           - 升级模式：显示升级后的剑（预览）
 *           - 拆除模式：放入已强化的剑（输入）
 * 
 *  槽位 1-5 = 左侧2x3材料网格（只用前5个） @ 起点(25, 36)，间距18
 *           - 升级模式：放入材料（输入）
 *           - 拆除模式：显示可拆的宝石（预览）
 * 
 *  槽位 6 = SWORD槽 @ (34, 90)
 *           - 升级模式：放入剑（输入）
 *           - 拆除模式：显示拆除后的剑（预览）
 * 
 *  槽位 7 = 未使用（保留）
 * 
 * 箭头按钮: @ [33, 15, 53, 29] （需微调）
 *           - 升级模式：统一升级
 *           - 拆除模式：拆除所有宝石
 * 
 * 玩家背包: @ (6, 140) 起点，间距18
 * ============================================================
 */
public class TileEntitySwordUpgradeStation extends TileEntity implements ITickable {

    // ==================== 槽位索引常量 ====================
    
    public static final int SLOT_OUTPUT = 0;   // 输出/拆除输入槽 @ (122, 58)
    public static final int SLOT_MAT0 = 1;     // 材料槽 @ (25+0*18, 36+0*18) = (25, 36)
    public static final int SLOT_MAT1 = 2;     // 材料槽 @ (25+1*18, 36+0*18) = (43, 36)
    public static final int SLOT_MAT2 = 3;     // 材料槽 @ (25+0*18, 36+1*18) = (25, 54)
    public static final int SLOT_MAT3 = 4;     // 材料槽 @ (25+1*18, 36+1*18) = (43, 54)
    public static final int SLOT_MAT4 = 5;     // 材料槽 @ (25+0*18, 36+2*18) = (25, 72)
    public static final int SLOT_SWORD = 6;    // 剑槽 @ (34, 90)
    public static final int SLOT_UNUSED = 7;   // 保留槽
    public static final int SLOT_COUNT = 8;

    // 材料槽范围
    public static final int MATERIAL_SLOT_START = SLOT_MAT0;
    public static final int MATERIAL_SLOT_END = SLOT_MAT4;
    public static final int MAX_INLAY_COUNT = 5; // 最多5个镶嵌

    // ==================== 操作模式 ====================
    
    public enum Mode {
        IDLE,      // 空闲
        UPGRADE,   // 升级模式：槽位6有剑 + 槽位1-5有材料 → 槽位0预览结果
        REMOVAL    // 拆除模式：槽位0有已强化的剑 → 槽位1-5预览宝石 + 槽位6预览返还剑
    }

    // ==================== 物品处理器 ====================
    
    private final ItemStackHandler items = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            updatePreview();
            markDirty();
            syncToClient();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                // 输出槽可以放入剑（用于拆除模式）
                return stack.getItem() instanceof ItemSword;
            } else if (slot >= MATERIAL_SLOT_START && slot <= MATERIAL_SLOT_END) {
                // 材料槽只在升级模式下可放入
                Mode mode = getCurrentMode();
                if (mode == Mode.UPGRADE) {
                    return UpgradeConfig.isValidMaterial(stack);
                }
                return false; // 拆除模式下材料槽是预览，不能放入
            } else if (slot == SLOT_SWORD) {
                // 剑槽只在升级模式下可放入
                Mode mode = getCurrentMode();
                if (mode == Mode.UPGRADE || mode == Mode.IDLE) {
                    return stack.getItem() instanceof ItemSword;
                }
                return false; // 拆除模式下剑槽是预览，不能放入
            }
            return false;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            Mode mode = getCurrentMode();
            
            if (slot == SLOT_OUTPUT) {
                if (mode == Mode.UPGRADE && !simulate) {
                    // 升级模式：从输出槽拿取升级后的剑
                    return onUpgradeResultTaken();
                }
                // 拆除模式：可以正常取出（这是输入槽）
                return super.extractItem(slot, amount, simulate);
            } else if (slot >= MATERIAL_SLOT_START && slot <= MATERIAL_SLOT_END) {
                if (mode == Mode.REMOVAL) {
                    // 拆除模式：材料槽是预览，不能直接取出
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            } else if (slot == SLOT_SWORD) {
                if (mode == Mode.REMOVAL) {
                    // 拆除模式：剑槽是预览，不能直接取出
                    return ItemStack.EMPTY;
                }
                return super.extractItem(slot, amount, simulate);
            }
            
            return super.extractItem(slot, amount, simulate);
        }
    };

    private long lastUpdateTick = -1L;

    // ==================== 生命周期 ====================
    
    @Override
    public void update() {
        if (world == null || world.isRemote) return;
        
        long t = world.getTotalWorldTime();
        if (lastUpdateTick < 0 || (t - lastUpdateTick) >= 10) {
            lastUpdateTick = t;
            updatePreview();
        }
    }

    // ==================== 模式检测 ====================
    
    /**
     * 检测当前操作模式
     */
    public Mode getCurrentMode() {
        ItemStack outputSlot = items.getStackInSlot(SLOT_OUTPUT);
        ItemStack swordSlot = items.getStackInSlot(SLOT_SWORD);
        
        // 拆除模式：输出槽有已强化的剑
        if (!outputSlot.isEmpty() && outputSlot.getItem() instanceof ItemSword) {
            if (hasInlays(outputSlot)) {
                return Mode.REMOVAL;
            }
        }
        
        // 升级模式：剑槽有剑 + 至少一个材料槽有材料
        if (!swordSlot.isEmpty() && swordSlot.getItem() instanceof ItemSword) {
            for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
                if (!items.getStackInSlot(i).isEmpty()) {
                    return Mode.UPGRADE;
                }
            }
        }
        
        return Mode.IDLE;
    }

    /**
     * 检查剑是否有镶嵌
     */
    private boolean hasInlays(ItemStack sword) {
        if (!sword.hasTagCompound()) return false;
        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return false;
        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return false;
        NBTTagCompound inlays = upgradeData.getCompoundTag("Inlays");
        return inlays.getInteger("Count") > 0;
    }

    // ==================== 预览更新 ====================
    
    /**
     * 根据当前模式更新预览
     */
    public void updatePreview() {
        if (world != null && world.isRemote) return;

        Mode mode = getCurrentMode();
        
        switch (mode) {
            case UPGRADE:
                updateUpgradePreview();
                break;
            case REMOVAL:
                updateRemovalPreview();
                break;
            case IDLE:
                clearAllPreviews();
                break;
        }
    }

    /**
     * 升级模式预览：槽位0显示升级后的剑
     */
    private void updateUpgradePreview() {
        ItemStack sword = items.getStackInSlot(SLOT_SWORD);
        
        if (sword.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
            return;
        }

        // 计算升级结果
        ItemStack result = calculateUpgradeResult();
        items.setStackInSlot(SLOT_OUTPUT, result);
    }

    /**
     * 拆除模式预览：槽位1-5显示宝石，槽位6显示返还的剑
     */
    private void updateRemovalPreview() {
        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        
        if (sword.isEmpty()) {
            clearAllPreviews();
            return;
        }

        // 获取镶嵌列表
        List<InlayInfo> inlays = getInlays(sword);
        
        // 在材料槽显示宝石预览
        for (int i = 0; i < MAX_INLAY_COUNT; i++) {
            int slotIndex = MATERIAL_SLOT_START + i;
            if (i < inlays.size()) {
                InlayInfo inlay = inlays.get(i);
                ItemStack gemPreview = createGemStack(inlay.materialId);
                items.setStackInSlot(slotIndex, gemPreview);
            } else {
                items.setStackInSlot(slotIndex, ItemStack.EMPTY);
            }
        }
        
        // 在剑槽显示拆除后的剑预览
        ItemStack cleanSword = removeAllInlays(sword.copy());
        items.setStackInSlot(SLOT_SWORD, cleanSword);
    }

    /**
     * 清空所有预览
     */
    private void clearAllPreviews() {
        // 注意：只清空预览槽位，不清空输入槽位
        Mode prevMode = Mode.IDLE;
        
        // 简单判断：如果输出槽有剑则可能是拆除模式
        ItemStack outputSlot = items.getStackInSlot(SLOT_OUTPUT);
        if (!outputSlot.isEmpty() && outputSlot.getItem() instanceof ItemSword) {
            // 保留输出槽，清空其他
            for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
            items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);
        } else {
            // 只清空输出槽
            items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        }
    }

    // ==================== 升级逻辑 ====================
    
    /**
     * 计算升级结果（纯计算）
     */
    private ItemStack calculateUpgradeResult() {
        ItemStack sword = items.getStackInSlot(SLOT_SWORD).copy();
        
        for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
            ItemStack material = items.getStackInSlot(i);
            if (!material.isEmpty()) {
                ItemStack upgraded = upgradeWithMaterial(sword, material);
                if (!upgraded.isEmpty()) {
                    sword = upgraded;
                }
            }
        }

        return sword;
    }

    /**
     * 单个材料升级
     */
    private ItemStack upgradeWithMaterial(ItemStack sword, ItemStack material) {
        String materialId = material.getItem().getRegistryName().toString();
        SwordMaterialData data = UpgradeConfig.getMaterialData(materialId);

        if (data == null) return ItemStack.EMPTY;

        ItemStack result = sword.copy();
        NBTTagCompound tag = result.hasTagCompound() ? result.getTagCompound() : new NBTTagCompound();
        NBTTagCompound upgradeData = tag.hasKey("SwordUpgrades") ?
                tag.getCompoundTag("SwordUpgrades") : new NBTTagCompound();

        // 累加属性
        float currentAttack = upgradeData.getFloat("AttackBonus");
        float currentSpeed = upgradeData.getFloat("SpeedBonus");
        upgradeData.setFloat("AttackBonus", currentAttack + data.attackDamage);
        upgradeData.setFloat("SpeedBonus", currentSpeed + data.attackSpeed);

        // 记录镶嵌
        NBTTagCompound inlays = upgradeData.hasKey("Inlays") ?
                upgradeData.getCompoundTag("Inlays") : new NBTTagCompound();
        int count = inlays.getInteger("Count");

        if (count < MAX_INLAY_COUNT) {
            NBTTagCompound inlay = new NBTTagCompound();
            inlay.setString("Material", materialId);
            inlay.setString("Name", material.getDisplayName());
            inlays.setTag("Inlay_" + count, inlay);
            inlays.setInteger("Count", count + 1);
            upgradeData.setTag("Inlays", inlays);
        } else {
            return ItemStack.EMPTY; // 已满
        }

        // 额外属性
        if (data.extraAttributes != null && !data.extraAttributes.isEmpty()) {
            NBTTagCompound extraAttrs = upgradeData.hasKey("ExtraAttributes") ?
                    upgradeData.getCompoundTag("ExtraAttributes") : new NBTTagCompound();

            for (String key : data.extraAttributes.keySet()) {
                double current = extraAttrs.getDouble(key);
                extraAttrs.setDouble(key, current + data.extraAttributes.get(key));
            }
            upgradeData.setTag("ExtraAttributes", extraAttrs);
        }

        tag.setTag("SwordUpgrades", upgradeData);
        result.setTagCompound(tag);
        return result;
    }

    /**
     * 玩家从输出槽拿取升级结果时
     */
    private ItemStack onUpgradeResultTaken() {
        ItemStack result = items.getStackInSlot(SLOT_OUTPUT).copy();
        
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // 消耗剑和材料
        items.setStackInSlot(SLOT_SWORD, ItemStack.EMPTY);
        for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
            ItemStack material = items.getStackInSlot(i);
            if (!material.isEmpty()) {
                material.shrink(1);
            }
        }

        items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        markDirty();
        syncToClient();

        return result;
    }

    // ==================== 拆除逻辑 ====================
    
    /**
     * 镶嵌信息
     */
    public static class InlayInfo {
        public int index;
        public String materialId;
        public String displayName;
        public int removalCost;

        public InlayInfo(int index, String materialId, String displayName, int removalCost) {
            this.index = index;
            this.materialId = materialId;
            this.displayName = displayName;
            this.removalCost = removalCost;
        }
    }

    /**
     * 获取剑的镶嵌列表
     */
    public List<InlayInfo> getInlays(ItemStack sword) {
        List<InlayInfo> result = new ArrayList<>();
        
        if (!sword.hasTagCompound()) return result;
        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return result;
        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return result;
        
        NBTTagCompound inlays = upgradeData.getCompoundTag("Inlays");
        int count = inlays.getInteger("Count");
        
        for (int i = 0; i < count; i++) {
            String key = "Inlay_" + i;
            if (inlays.hasKey(key)) {
                NBTTagCompound inlay = inlays.getCompoundTag(key);
                String materialId = inlay.getString("Material");
                String name = inlay.getString("Name");
                int cost = calculateRemovalCost(i, count);
                
                result.add(new InlayInfo(i, materialId, name, cost));
            }
        }
        
        return result;
    }

    /**
     * 计算拆除成本
     */
    private int calculateRemovalCost(int index, int totalCount) {
        // 简单算法：基础成本5级 + 索引 * 2
        return 5 + index * 2;
    }

    /**
     * 根据材料ID创建物品
     */
    private ItemStack createGemStack(String materialId) {
        try {
            net.minecraft.item.Item item = net.minecraft.item.Item.getByNameOrId(materialId);
            if (item != null) {
                ItemStack stack = new ItemStack(item, 1);
                // 添加标记表示这是预览
                NBTTagCompound tag = new NBTTagCompound();
                tag.setBoolean("Preview", true);
                stack.setTagCompound(tag);
                return stack;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ItemStack.EMPTY;
    }

    /**
     * 拆除所有镶嵌
     */
    private ItemStack removeAllInlays(ItemStack sword) {
        if (!sword.hasTagCompound()) return sword;
        
        NBTTagCompound tag = sword.getTagCompound();
        if (tag.hasKey("SwordUpgrades")) {
            NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
            
            // 清除属性加成
            upgradeData.setFloat("AttackBonus", 0.0f);
            upgradeData.setFloat("SpeedBonus", 0.0f);
            
            // 清除镶嵌记录
            if (upgradeData.hasKey("Inlays")) {
                upgradeData.removeTag("Inlays");
            }
            
            // 清除额外属性
            if (upgradeData.hasKey("ExtraAttributes")) {
                upgradeData.removeTag("ExtraAttributes");
            }
            
            tag.setTag("SwordUpgrades", upgradeData);
        }
        
        return sword;
    }

    /**
     * 拆除单个宝石（右键点击材料槽预览时）
     */
    public ItemStack removeSingleGem(int slotIndex, EntityPlayer player) {
        if (getCurrentMode() != Mode.REMOVAL) {
            return ItemStack.EMPTY;
        }
        
        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty()) return ItemStack.EMPTY;
        
        int inlayIndex = slotIndex - MATERIAL_SLOT_START;
        List<InlayInfo> inlays = getInlays(sword);
        
        if (inlayIndex < 0 || inlayIndex >= inlays.size()) {
            return ItemStack.EMPTY;
        }
        
        InlayInfo info = inlays.get(inlayIndex);
        
        // 检查经验
        if (player.experienceLevel < info.removalCost) {
            return ItemStack.EMPTY;
        }
        
        // 扣除经验
        player.addExperienceLevel(-info.removalCost);
        
        // 移除镶嵌
        ItemStack updatedSword = removeSingleInlay(sword.copy(), inlayIndex);
        items.setStackInSlot(SLOT_OUTPUT, updatedSword);
        
        // 返回宝石
        ItemStack gem = createGemStack(info.materialId);
        if (gem.hasTagCompound()) {
            gem.getTagCompound().removeTag("Preview"); // 移除预览标记
        }
        
        updatePreview();
        markDirty();
        syncToClient();
        
        return gem;
    }

    /**
     * 拆除单个镶嵌槽位
     */
    private ItemStack removeSingleInlay(ItemStack sword, int inlayIndex) {
        if (!sword.hasTagCompound()) return sword;
        
        NBTTagCompound tag = sword.getTagCompound();
        if (!tag.hasKey("SwordUpgrades")) return sword;
        
        NBTTagCompound upgradeData = tag.getCompoundTag("SwordUpgrades");
        if (!upgradeData.hasKey("Inlays")) return sword;
        
        NBTTagCompound inlays = upgradeData.getCompoundTag("Inlays");
        String key = "Inlay_" + inlayIndex;
        
        if (!inlays.hasKey(key)) return sword;
        
        // 获取要移除的材料数据
        NBTTagCompound inlay = inlays.getCompoundTag(key);
        String materialId = inlay.getString("Material");
        SwordMaterialData data = UpgradeConfig.getMaterialData(materialId);
        
        if (data != null) {
            // 减少属性
            float currentAttack = upgradeData.getFloat("AttackBonus");
            float currentSpeed = upgradeData.getFloat("SpeedBonus");
            upgradeData.setFloat("AttackBonus", Math.max(0, currentAttack - data.attackDamage));
            upgradeData.setFloat("SpeedBonus", Math.max(-4, currentSpeed - data.attackSpeed));
            
            // 减少额外属性
            if (data.extraAttributes != null && upgradeData.hasKey("ExtraAttributes")) {
                NBTTagCompound extraAttrs = upgradeData.getCompoundTag("ExtraAttributes");
                for (String attrKey : data.extraAttributes.keySet()) {
                    double current = extraAttrs.getDouble(attrKey);
                    extraAttrs.setDouble(attrKey, current - data.extraAttributes.get(attrKey));
                }
                upgradeData.setTag("ExtraAttributes", extraAttrs);
            }
        }
        
        // 移除镶嵌记录并重新索引
        int count = inlays.getInteger("Count");
        NBTTagCompound newInlays = new NBTTagCompound();
        int newIndex = 0;
        
        for (int i = 0; i < count; i++) {
            if (i != inlayIndex && inlays.hasKey("Inlay_" + i)) {
                newInlays.setTag("Inlay_" + newIndex, inlays.getCompoundTag("Inlay_" + i));
                newIndex++;
            }
        }
        
        newInlays.setInteger("Count", newIndex);
        upgradeData.setTag("Inlays", newInlays);
        tag.setTag("SwordUpgrades", upgradeData);
        
        return sword;
    }

    /**
     * 拆除所有宝石（箭头按钮）
     */
    public void removeAllGems(EntityPlayer player) {
        if (getCurrentMode() != Mode.REMOVAL) return;
        
        ItemStack sword = items.getStackInSlot(SLOT_OUTPUT);
        if (sword.isEmpty()) return;
        
        List<InlayInfo> inlays = getInlays(sword);
        if (inlays.isEmpty()) return;
        
        // 计算总成本
        int totalCost = 0;
        for (InlayInfo info : inlays) {
            totalCost += info.removalCost;
        }
        
        // 检查经验
        if (player.experienceLevel < totalCost) {
            return;
        }
        
        // 扣除经验
        player.addExperienceLevel(-totalCost);
        
        // 获取所有宝石
        List<ItemStack> gems = new ArrayList<>();
        for (InlayInfo info : inlays) {
            ItemStack gem = createGemStack(info.materialId);
            if (!gem.isEmpty() && gem.hasTagCompound()) {
                gem.getTagCompound().removeTag("Preview");
            }
            if (!gem.isEmpty()) {
                gems.add(gem);
            }
        }
        
        // 返还干净的剑到剑槽
        ItemStack cleanSword = removeAllInlays(sword.copy());
        items.setStackInSlot(SLOT_SWORD, cleanSword);
        
        // 清空输出槽
        items.setStackInSlot(SLOT_OUTPUT, ItemStack.EMPTY);
        
        // 给玩家宝石
        for (ItemStack gem : gems) {
            if (!player.inventory.addItemStackToInventory(gem)) {
                player.dropItem(gem, false);
            }
        }
        
        updatePreview();
        markDirty();
        syncToClient();
    }

    // ==================== 星形升级（箭头按钮）====================
    
    public boolean canPerformStarUpgrade() {
        return getCurrentMode() == Mode.UPGRADE;
    }

    /**
     * 执行星形升级（箭头按钮 - 升级模式）
     */
    public void performStarUpgrade() {
        if (!canPerformStarUpgrade()) return;

        ItemStack sword = items.getStackInSlot(SLOT_SWORD);
        ItemStack upgraded = sword.copy();

        for (int i = MATERIAL_SLOT_START; i <= MATERIAL_SLOT_END; i++) {
            ItemStack material = items.getStackInSlot(i);
            if (!material.isEmpty()) {
                ItemStack temp = upgradeWithMaterial(upgraded, material);
                if (!temp.isEmpty()) {
                    upgraded = temp;
                    material.shrink(1);
                }
            }
        }

        items.setStackInSlot(SLOT_SWORD, upgraded);
        updatePreview();
        markDirty();
        syncToClient();
    }

    public boolean canPerformRemoveAll() {
        return getCurrentMode() == Mode.REMOVAL && !getInlays(items.getStackInSlot(SLOT_OUTPUT)).isEmpty();
    }

    // ==================== Capability ====================
    
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
        return super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) items;
        }
        return super.getCapability(capability, facing);
    }

    public IItemHandler getItemHandler() {
        return items;
    }

    public ItemStack getStackInSlot(int index) {
        return items.getStackInSlot(index);
    }

    public boolean isUsableByPlayer(EntityPlayer player) {
        if (world == null) return false;
        if (world.getTileEntity(pos) != this) return false;
        return player.getDistanceSq(
                (double) pos.getX() + 0.5D,
                (double) pos.getY() + 0.5D,
                (double) pos.getZ() + 0.5D
        ) <= 64.0D;
    }

    // ==================== NBT ====================
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", items.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Items")) {
            items.deserializeNBT(compound.getCompoundTag("Items"));
        }
        if (world != null && !world.isRemote) {
            updatePreview();
        }
    }

    // ==================== Client-Server同步 ====================
    
    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 1, getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound tag = super.getUpdateTag();
        tag.setTag("Items", items.serializeNBT());
        return tag;
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        NBTTagCompound tag = pkt.getNbtCompound();
        readFromNBT(tag);
    }

    private void syncToClient() {
        if (world == null || world.isRemote) return;
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        markDirty();
    }
}