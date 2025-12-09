package com.moremod.item.armor;

import com.moremod.moremod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 故障盔甲 - Glitch Armor
 * 一套充滿數據異常風格的強力盔甲
 */
public class ItemGlitchArmor extends ItemArmor {

    // 自定義盔甲材質
    public static final ArmorMaterial GLITCH_MATERIAL = EnumHelper.addArmorMaterial(
            "GLITCH",                           // 名稱
            moremod.MODID + ":glitch",          // 貼圖路徑
            40,                                 // 耐久度係數
            new int[]{4, 7, 9, 4},              // 護甲值 (靴子、護腿、胸甲、頭盔)
            20,                                 // 附魔能力
            net.minecraft.init.SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, // 裝備音效
            3.0F                                // 韌性
    );

    private final EntityEquipmentSlot slot;

    public ItemGlitchArmor(EntityEquipmentSlot slot, String name) {
        super(GLITCH_MATERIAL, 0, slot);
        this.slot = slot;
        this.setRegistryName(moremod.MODID, name);
        this.setTranslationKey(name);
        this.setCreativeTab(CreativeTabs.COMBAT);
        this.setMaxStackSize(1);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        // 所有部位統一使用同一張貼圖
        return moremod.MODID + ":textures/models/armor/glitch_armor.png";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add("");
        tooltip.add(TextFormatting.DARK_PURPLE + "§l『故障盔甲』");

        switch (this.slot) {
            case HEAD:
                tooltip.add(TextFormatting.GRAY + "◆ 記憶溢出");
                tooltip.add(TextFormatting.DARK_GRAY + "  擊殺敵人30%機率產生記憶殘留");
                tooltip.add(TextFormatting.DARK_GRAY + "  Shift+右鍵: 存檔讀取 (記錄/傳送回位置)");
                break;
            case CHEST:
                tooltip.add(TextFormatting.GRAY + "◆ 空指針異常 (NULL)");
                tooltip.add(TextFormatting.DARK_GRAY + "  致命傷害20%機率完全消除");
                tooltip.add(TextFormatting.DARK_GRAY + "  觸發後獲得1秒閃爍無敵");
                tooltip.add("");
                tooltip.add(TextFormatting.GRAY + "◆ 段錯誤");
                tooltip.add(TextFormatting.DARK_GRAY + "  攻擊5%機率造成雙倍傳染傷害");
                break;
            case LEGS:
                tooltip.add(TextFormatting.GRAY + "◆ 緩衝區溢出");
                tooltip.add(TextFormatting.DARK_GRAY + "  受傷50%轉為緩存，10秒後返還治療");
                tooltip.add(TextFormatting.DARK_GRAY + "  主動: 釋放緩存傷害為AOE攻擊");
                break;
            case FEET:
                tooltip.add(TextFormatting.GRAY + "◆ 堆疊下溢");
                tooltip.add(TextFormatting.DARK_GRAY + "  無摔落傷害，落地傷害轉移給敵人");
                tooltip.add(TextFormatting.DARK_GRAY + "  踩踏方塊1%機率暫時崩潰");
                break;
            default:
                break;
        }

        tooltip.add("");
        tooltip.add(TextFormatting.LIGHT_PURPLE + "【全套效果】" + TextFormatting.WHITE + " 藍屏死機");
        tooltip.add(TextFormatting.DARK_GRAY + "  消耗50,000 RF啟動時停領域");
    }

    /**
     * 檢查玩家是否穿戴全套故障盔甲
     */
    public static boolean hasFullSet(EntityPlayer player) {
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[]{
                EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET}) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ItemGlitchArmor)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 獲取穿戴的故障盔甲件數
     */
    public static int getArmorPieceCount(EntityPlayer player) {
        int count = 0;
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[]{
                EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET}) {
            ItemStack stack = player.getItemStackFromSlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemGlitchArmor) {
                count++;
            }
        }
        return count;
    }

    /**
     * 檢查是否穿戴特定部位的故障盔甲
     */
    public static boolean hasArmorPiece(EntityPlayer player, EntityEquipmentSlot slot) {
        ItemStack stack = player.getItemStackFromSlot(slot);
        return !stack.isEmpty() && stack.getItem() instanceof ItemGlitchArmor;
    }
}
