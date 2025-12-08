package com.moremod.item.ritual;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 假玩家核心 - 通过三阶祭坛仪式使用玩家头颅创建
 * 可以存储玩家的灵魂信息，用于创建假玩家模拟器
 */
public class ItemFakePlayerCore extends Item {

    public static final String TAG_PLAYER_PROFILE = "PlayerProfile";
    public static final String TAG_PLAYER_NAME = "PlayerName";
    public static final String TAG_PLAYER_UUID = "PlayerUUID";
    public static final String TAG_ACTIVATION_COUNT = "ActivationCount";

    public ItemFakePlayerCore() {
        setRegistryName("moremod", "fake_player_core");
        setUnlocalizedName("moremod.fake_player_core");
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
        setMaxDamage(100); // 100次使用
    }

    /**
     * 从玩家头颅ItemStack中提取GameProfile
     */
    @Nullable
    public static GameProfile getProfileFromSkull(ItemStack skull) {
        if (skull.isEmpty() || skull.getItem() != Items.SKULL) {
            return null;
        }

        // 只有玩家头颅(meta=3)才有profile
        if (skull.getMetadata() != 3) {
            return null;
        }

        if (!skull.hasTagCompound()) {
            return null;
        }

        NBTTagCompound nbt = skull.getTagCompound();
        if (nbt.hasKey("SkullOwner", 10)) {
            return NBTUtil.readGameProfileFromNBT(nbt.getCompoundTag("SkullOwner"));
        } else if (nbt.hasKey("SkullOwner", 8)) {
            // 只有名字的情况
            String name = nbt.getString("SkullOwner");
            if (!name.isEmpty()) {
                return new GameProfile(null, name);
            }
        }

        return null;
    }

    /**
     * 将GameProfile存储到假玩家核心中
     */
    public static void storeProfile(ItemStack core, GameProfile profile) {
        if (core.isEmpty() || !(core.getItem() instanceof ItemFakePlayerCore)) {
            return;
        }

        if (!core.hasTagCompound()) {
            core.setTagCompound(new NBTTagCompound());
        }

        NBTTagCompound nbt = core.getTagCompound();

        if (profile != null) {
            NBTTagCompound profileTag = new NBTTagCompound();
            NBTUtil.writeGameProfile(profileTag, profile);
            nbt.setTag(TAG_PLAYER_PROFILE, profileTag);

            if (profile.getName() != null) {
                nbt.setString(TAG_PLAYER_NAME, profile.getName());
            }
            if (profile.getId() != null) {
                nbt.setString(TAG_PLAYER_UUID, profile.getId().toString());
            }
        }

        nbt.setInteger(TAG_ACTIVATION_COUNT, 0);
    }

    /**
     * 从假玩家核心中读取GameProfile
     */
    @Nullable
    public static GameProfile getStoredProfile(ItemStack core) {
        if (core.isEmpty() || !(core.getItem() instanceof ItemFakePlayerCore)) {
            return null;
        }

        if (!core.hasTagCompound()) {
            return null;
        }

        NBTTagCompound nbt = core.getTagCompound();

        if (nbt.hasKey(TAG_PLAYER_PROFILE, 10)) {
            return NBTUtil.readGameProfileFromNBT(nbt.getCompoundTag(TAG_PLAYER_PROFILE));
        }

        // 尝试从名字和UUID重建
        String name = nbt.getString(TAG_PLAYER_NAME);
        String uuidStr = nbt.getString(TAG_PLAYER_UUID);

        if (!name.isEmpty() || !uuidStr.isEmpty()) {
            UUID uuid = null;
            try {
                if (!uuidStr.isEmpty()) {
                    uuid = UUID.fromString(uuidStr);
                }
            } catch (IllegalArgumentException ignored) {}

            return new GameProfile(uuid, name.isEmpty() ? null : name);
        }

        return null;
    }

    /**
     * 检查是否有存储的玩家信息
     */
    public static boolean hasStoredProfile(ItemStack core) {
        return getStoredProfile(core) != null;
    }

    /**
     * 获取存储的玩家名称
     */
    public static String getStoredPlayerName(ItemStack core) {
        GameProfile profile = getStoredProfile(core);
        if (profile != null && profile.getName() != null) {
            return profile.getName();
        }

        if (core.hasTagCompound()) {
            String name = core.getTagCompound().getString(TAG_PLAYER_NAME);
            if (!name.isEmpty()) return name;
        }

        return "Unknown";
    }

    /**
     * 增加使用次数
     */
    public static void incrementActivation(ItemStack core) {
        if (!core.hasTagCompound()) {
            core.setTagCompound(new NBTTagCompound());
        }
        int count = core.getTagCompound().getInteger(TAG_ACTIVATION_COUNT);
        core.getTagCompound().setInteger(TAG_ACTIVATION_COUNT, count + 1);
    }

    /**
     * 获取使用次数
     */
    public static int getActivationCount(ItemStack core) {
        if (!core.hasTagCompound()) return 0;
        return core.getTagCompound().getInteger(TAG_ACTIVATION_COUNT);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                                       EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // 可以在这里添加放置假玩家控制器方块的逻辑
        return EnumActionResult.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);

        if (!worldIn.isRemote && hasStoredProfile(stack)) {
            // 显示存储的玩家信息
            GameProfile profile = getStoredProfile(stack);
            if (profile != null) {
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GOLD + "假玩家核心绑定: " +
                    TextFormatting.WHITE + (profile.getName() != null ? profile.getName() : "Unknown")
                ));
                playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                    TextFormatting.GRAY + "使用次数: " + getActivationCount(stack) +
                    " / 剩余耐久: " + (stack.getMaxDamage() - stack.getItemDamage())
                ));
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return hasStoredProfile(stack) ? EnumRarity.EPIC : EnumRarity.RARE;
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return hasStoredProfile(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (hasStoredProfile(stack)) {
            String playerName = getStoredPlayerName(stack);
            tooltip.add(TextFormatting.GOLD + "绑定玩家: " + TextFormatting.WHITE + playerName);
            tooltip.add(TextFormatting.GRAY + "使用次数: " + TextFormatting.YELLOW + getActivationCount(stack));
            tooltip.add("");
            tooltip.add(TextFormatting.DARK_PURPLE + "蕴含着 " + playerName + " 的灵魂碎片");
            tooltip.add(TextFormatting.DARK_GRAY + "可用于自动化设备模拟玩家操作");
        } else {
            tooltip.add(TextFormatting.GRAY + "空白的假玩家核心");
            tooltip.add(TextFormatting.DARK_GRAY + "需要通过仪式注入玩家灵魂");
        }

        tooltip.add("");
        tooltip.add(TextFormatting.DARK_GRAY + "耐久: " + (stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (hasStoredProfile(stack)) {
            String playerName = getStoredPlayerName(stack);
            return TextFormatting.GOLD + playerName + "的灵魂核心";
        }
        return super.getItemStackDisplayName(stack);
    }
}
