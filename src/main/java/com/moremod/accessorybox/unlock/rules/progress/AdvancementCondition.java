package com.moremod.accessorybox.unlock.rules.progress;

import com.moremod.accessorybox.unlock.rules.UnlockCondition;
import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;

/**
 * 进度条件
 */
public class AdvancementCondition implements UnlockCondition {
    private final String advancementId;

    public AdvancementCondition(String advancementId) {
        this.advancementId = advancementId;
    }

    @Override
    public boolean check(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP)) {
            return false;
        }

        EntityPlayerMP playerMP = (EntityPlayerMP) player;
        Advancement advancement = playerMP.getServerWorld()
                .getAdvancementManager()
                .getAdvancement(new ResourceLocation(advancementId));

        if (advancement == null) {
            return false;
        }

        return playerMP.getAdvancements().getProgress(advancement).isDone();
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public String getType() {
        return "advancement";
    }

    @Override
    public String getDescription() {
        return "进度: " + advancementId;
    }
}
