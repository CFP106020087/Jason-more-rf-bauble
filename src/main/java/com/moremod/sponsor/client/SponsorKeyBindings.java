package com.moremod.sponsor.client;

import com.moremod.sponsor.item.ZhuxianSword;
import com.moremod.sponsor.network.SponsorNetworkHandler;
import com.moremod.sponsor.network.PacketToggleSkill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

/**
 * 赞助者物品快捷键绑定
 *
 * 横渠四句技能快捷键:
 * - Z: 为天地立心
 * - X: 为生民立命
 * - C: 为往圣继绝学
 * - V: 为万世开太平
 *
 * 剑阵快捷键:
 * - B: 切换诛仙剑阵（绝仙形态）
 */
@Mod.EventBusSubscriber(modid = "moremod", value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public class SponsorKeyBindings {

    private static final String CATEGORY = "key.categories.moremod.sponsor";

    // 横渠四句
    public static KeyBinding KEY_TIANXIN;  // 为天地立心
    public static KeyBinding KEY_LIMING;   // 为生民立命
    public static KeyBinding KEY_JUEXUE;   // 为往圣继绝学
    public static KeyBinding KEY_TAIPING;  // 为万世开太平

    // 剑阵
    public static KeyBinding KEY_FORMATION; // 诛仙剑阵

    private static boolean initialized = false;

    /**
     * 注册快捷键
     */
    public static void registerKeyBindings() {
        if (initialized) return;
        initialized = true;

        KEY_TIANXIN = new KeyBinding(
            "key.moremod.zhuxian.tianxin",
            Keyboard.KEY_Z,
            CATEGORY
        );

        KEY_LIMING = new KeyBinding(
            "key.moremod.zhuxian.liming",
            Keyboard.KEY_X,
            CATEGORY
        );

        KEY_JUEXUE = new KeyBinding(
            "key.moremod.zhuxian.juexue",
            Keyboard.KEY_C,
            CATEGORY
        );

        KEY_TAIPING = new KeyBinding(
            "key.moremod.zhuxian.taiping",
            Keyboard.KEY_V,
            CATEGORY
        );

        KEY_FORMATION = new KeyBinding(
            "key.moremod.zhuxian.formation",
            Keyboard.KEY_B,
            CATEGORY
        );

        ClientRegistry.registerKeyBinding(KEY_TIANXIN);
        ClientRegistry.registerKeyBinding(KEY_LIMING);
        ClientRegistry.registerKeyBinding(KEY_JUEXUE);
        ClientRegistry.registerKeyBinding(KEY_TAIPING);
        ClientRegistry.registerKeyBinding(KEY_FORMATION);

        System.out.println("[moremod/sponsor] 快捷键已注册");
    }

    /**
     * 处理快捷键输入
     */
    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;

        if (player == null || mc.currentScreen != null) return;

        ItemStack sword = ZhuxianSword.getZhuxianSword(player);
        if (sword.isEmpty()) return;

        ZhuxianSword item = (ZhuxianSword) sword.getItem();

        // 为天地立心
        if (KEY_TIANXIN != null && KEY_TIANXIN.isPressed()) {
            toggleSkill(player, item, sword, ZhuxianSword.NBT_SKILL_TIANXIN, "为天地立心");
        }

        // 为生民立命
        if (KEY_LIMING != null && KEY_LIMING.isPressed()) {
            toggleSkill(player, item, sword, ZhuxianSword.NBT_SKILL_LIMING, "为生民立命");
        }

        // 为往圣继绝学
        if (KEY_JUEXUE != null && KEY_JUEXUE.isPressed()) {
            toggleSkill(player, item, sword, ZhuxianSword.NBT_SKILL_JUEXUE, "为往圣继绝学");
        }

        // 为万世开太平
        if (KEY_TAIPING != null && KEY_TAIPING.isPressed()) {
            toggleSkill(player, item, sword, ZhuxianSword.NBT_SKILL_TAIPING, "为万世开太平");
        }

        // 诛仙剑阵（仅绝仙形态）
        if (KEY_FORMATION != null && KEY_FORMATION.isPressed()) {
            if (item.getForm(sword) == ZhuxianSword.SwordForm.JUEXIAN) {
                item.setFormationActive(sword, !item.isFormationActive(sword));
                boolean active = item.isFormationActive(sword);
                player.sendStatusMessage(new TextComponentString(
                    (active ? TextFormatting.RED + "⚔ 诛仙剑阵: 开启" : TextFormatting.GRAY + "诛仙剑阵: 关闭") +
                    (active ? TextFormatting.DARK_RED + " (范围999999真伤)" : "")
                ), true);

                // 发送到服务器
                SponsorNetworkHandler.sendToServer(new PacketToggleSkill(ZhuxianSword.NBT_FORMATION_ACTIVE));
            } else {
                player.sendStatusMessage(new TextComponentString(
                    TextFormatting.RED + "需要绝仙形态才能开启剑阵!"
                ), true);
            }
        }
    }

    /**
     * 切换技能
     */
    private static void toggleSkill(EntityPlayer player, ZhuxianSword item, ItemStack sword,
                                    String skillKey, String skillName) {
        item.toggleSkill(sword, skillKey);
        boolean active = item.isSkillActive(sword, skillKey);

        TextFormatting color = active ? TextFormatting.GREEN : TextFormatting.GRAY;
        String status = active ? "开启" : "关闭";

        player.sendStatusMessage(new TextComponentString(
            color + "◆ " + skillName + ": " + status
        ), true);

        // 发送到服务器同步
        SponsorNetworkHandler.sendToServer(new PacketToggleSkill(skillKey));
    }
}
