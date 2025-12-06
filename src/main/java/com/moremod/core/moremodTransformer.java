package com.moremod.core;

import com.moremod.accessorybox.EarlyConfigLoader;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * 整合的主转换器 - 动态槽位版（SetBonus 兼容）
 * 支持每种类型独立配置（总和最多42个）
 */
public class moremodTransformer implements IClassTransformer {

    private static final boolean ENABLE_TOOLTIP_PATCH       = false;
    private static final boolean ENABLE_ENCHANTMENT_BOOST   = false;
    private static final boolean ENABLE_BAUBLES_EXPANSION   = true;
    private static final boolean ENABLE_ENIGMATIC_FIX       = true;
    private static final boolean ENABLE_POTIONFINGERS_FIX   = false;
    private static final boolean ENABLE_SETBONUS_COMPAT     = true;
    private static final boolean ENABLE_SWORD_UPGRADE       = true;
    private static final boolean ENABLE_BROKEN_GOD_DEATH    = true;
    private static final boolean ENABLE_SHAMBHALA_DEATH     = true;
    private static final boolean ENABLE_TEMPORAL_DEATH      = true;  // 时序织印死亡回溯
    private static final boolean ENABLE_CURSE_DEATH         = true;  // 七咒之戒-虚无之眸死亡保护

    public static Side side;

    static {
        side = FMLLaunchHandler.side();
        System.out.println("[moremodTransformer] Transformer loaded on side: " + side);

        // ⭐ 早期加载配置（在任何转换前）
        EarlyConfigLoader.loadEarly();
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        try {
            if (ENABLE_ENIGMATIC_FIX && "keletu.enigmaticlegacy.event.SuperpositionHandler".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching SuperpositionHandler...");
                return transformSuperpositionHandler(basicClass);
            }

            if (ENABLE_POTIONFINGERS_FIX && "vazkii.potionfingers.ItemRing".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching PotionFingers ItemRing...");
                return transformPotionFingersRing(basicClass);
            }

            if (ENABLE_BAUBLES_EXPANSION && "baubles.common.container.ContainerPlayerExpanded".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching ContainerPlayerExpanded...");
                return transformContainerPlayerExpanded(basicClass);
            }

            if (ENABLE_ENCHANTMENT_BOOST && "net.minecraft.enchantment.EnchantmentHelper".equals(transformedName)) {
                System.out.println("[moremodTransformer] Enhancing EnchantmentHelper...");
                return transformEnchantmentHelper(basicClass);
            }

            if (ENABLE_TOOLTIP_PATCH && side == Side.CLIENT && "net.minecraft.item.ItemStack".equals(transformedName)) {
                System.out.println("[moremodTransformer] Enhancing ItemStack tooltips...");
                return transformItemStackTooltips(basicClass);
            }

            // BaublesContainer 动态扩展
            if (ENABLE_BAUBLES_EXPANSION && "baubles.api.cap.BaublesContainer".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching BaublesContainer (dynamic)...");
                return transformBaublesContainerDynamic(basicClass);
            }

            // BaubleType 动态扩展（SetBonus 兼容版）
            if (ENABLE_BAUBLES_EXPANSION && "baubles.api.BaubleType".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching BaubleType (dynamic + SetBonus)...");
                return transformBaubleTypeDynamic(basicClass);
            }

            if (ENABLE_BAUBLES_EXPANSION && "keletu.enigmaticlegacy.api.bmtr.BaublesStackHandler".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching EL BaublesStackHandler...");
                return transformELBaublesStackHandler(basicClass);
            }

            if (ENABLE_SWORD_UPGRADE && "net.minecraft.item.ItemStack".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching ItemStack#getAttributeModifiers for upgrades...");
                return transformItemStackForUpgrades(basicClass);
            }

            // 破碎之神死亡拦截
            if (ENABLE_BROKEN_GOD_DEATH && "net.minecraft.entity.EntityLivingBase".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching EntityLivingBase.onDeath for Broken God...");
                return transformEntityLivingBaseOnDeath(basicClass);
            }

        } catch (Throwable t) {
            System.err.println("[moremodTransformer] Fatal error transforming " + transformedName);
            t.printStackTrace();
        }
        return basicClass;
    }

    // ============================================================
    // 剑升级系统
    // ============================================================
    private byte[] transformItemStackForUpgrades(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (!"(Lnet/minecraft/inventory/EntityEquipmentSlot;)Lcom/google/common/collect/Multimap;".equals(mn.desc))
                continue;
            if (!"getAttributeModifiers".equals(mn.name) && !"func_111283_C".equals(mn.name))
                continue;

            for (AbstractInsnNode n = mn.instructions.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() == Opcodes.ARETURN) {
                    InsnList inject = new InsnList();
                    inject.add(new InsnNode(Opcodes.DUP));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/SwordAttributeHandler",
                            "getUpgradeModifiers",
                            "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/inventory/EntityEquipmentSlot;)Lcom/google/common/collect/Multimap;",
                            false
                    ));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKEINTERFACE,
                            "com/google/common/collect/Multimap",
                            "putAll",
                            "(Lcom/google/common/collect/Multimap;)Z",
                            true
                    ));
                    inject.add(new InsnNode(Opcodes.POP));
                    mn.instructions.insertBefore(n, inject);
                    modified = true;
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // 附魔系统转换
    // ============================================================
    private byte[] transformEnchantmentHelper(byte[] bytes) {
        if (!classExists("com.moremod.enchantment.EnchantmentBoostHelper")) {
            System.out.println("[moremodTransformer] EnchantmentBoostHelper not found, skipping enchantment patches");
            return bytes;
        }

        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if ((mn.name.equals("getEnchantmentLevel") || mn.name.equals("func_77506_a"))
                    && mn.desc.equals("(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I")) {
                if (patchGetEnchantmentLevel(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + Patched getEnchantmentLevel");
                }
            }

            if ((mn.name.equals("getEnchantments") || mn.name.equals("func_82781_a"))
                    && mn.desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/Map;")) {
                if (patchGetEnchantments(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + Patched getEnchantments");
                }
            }

            if ((mn.name.equals("applyEnchantmentModifier") || mn.name.equals("func_77518_a"))
                    && mn.desc.contains("EnchantmentHelper$IModifier")) {
                if (patchApplyEnchantmentModifier(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + Patched applyEnchantmentModifier");
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean patchGetEnchantmentLevel(MethodNode mn) {
        boolean patched = false;
        for (AbstractInsnNode n : mn.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.IRETURN) {
                InsnList p = new InsnList();
                p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                p.add(new VarInsnNode(Opcodes.ALOAD, 1));
                p.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/moremod/enchantment/EnchantmentBoostHelper",
                        "getNewEnchantmentLevel",
                        "(ILnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I",
                        false));
                mn.instructions.insertBefore(n, p);
                patched = true;
            }
        }
        return patched;
    }

    private boolean patchGetEnchantments(MethodNode mn) {
        boolean patched = false;
        for (AbstractInsnNode n : mn.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.ARETURN) {
                InsnList p = new InsnList();
                p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                p.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "com/moremod/enchantment/EnchantmentBoostHelper",
                        "applyNewEnchantmentLevels",
                        "(Ljava/util/Map;Lnet/minecraft/item/ItemStack;)Ljava/util/Map;",
                        false));
                mn.instructions.insertBefore(n, p);
                patched = true;
            }
        }
        return patched;
    }

    private boolean patchApplyEnchantmentModifier(MethodNode mn) {
        for (AbstractInsnNode n : mn.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) n;
                if ((call.name.equals("getEnchantmentTagList") || call.name.equals("func_77986_q"))
                        && call.desc.equals("()Lnet/minecraft/nbt/NBTTagList;")) {
                    InsnList p = new InsnList();
                    p.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    p.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "com/moremod/enchantment/EnchantmentBoostHelper",
                            "modifyEnchantmentTagsForCombat",
                            "(Lnet/minecraft/nbt/NBTTagList;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagList;",
                            false));
                    mn.instructions.insert(call, p);
                    return true;
                }
            }
        }
        return false;
    }

    // ============================================================
    // Tooltip系统转换
    // ============================================================
    private byte[] transformItemStackTooltips(byte[] bytes) {
        if (!classExists("com.moremod.enchantment.EnchantmentBoostHelper")) {
            System.out.println("[moremodTransformer] EnchantmentBoostHelper not found, skipping tooltip patches");
            return bytes;
        }

        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("getTooltip") || mn.name.equals("func_82840_a")) {
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode call = (MethodInsnNode) n;
                        if ((call.name.equals("getEnchantmentTagList") || call.name.equals("func_77986_q"))
                                && call.desc.equals("()Lnet/minecraft/nbt/NBTTagList;")) {
                            InsnList p = new InsnList();
                            p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            p.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    "com/moremod/enchantment/EnchantmentBoostHelper",
                                    "modifyEnchantmentTagsForTooltip",
                                    "(Lnet/minecraft/nbt/NBTTagList;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagList;",
                                    false));
                            mn.instructions.insert(call, p);
                            modified = true;
                            System.out.println("[moremodTransformer]   + Patched tooltip enchantment display");
                            break;
                        }
                    }
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // BaublesContainer 动态扩展
    private byte[] transformBaublesContainerDynamic(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        int totalSlots = 49;
        int capacity = totalSlots * 2;

        System.out.println("[moremodTransformer]   ⚠️ 使用硬编码槽位数: " + totalSlots);

        for (MethodNode mn : cn.methods) {
            // 修改构造函数
            if (mn.name.equals("<init>")) {
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.BIPUSH) {
                        IntInsnNode intInsn = (IntInsnNode) n;

                        if (intInsn.operand == 7) {
                            intInsn.operand = totalSlots;
                            modified = true;
                            System.out.println("[moremodTransformer]     - <init>: 7 -> " + totalSlots);
                        }
                        else if (intInsn.operand == 14) {
                            intInsn.operand = capacity;
                            modified = true;
                            System.out.println("[moremodTransformer]     - <init>: 14 -> " + capacity);
                        }
                    }}
            }  // ✅ 只有一个 }

            // ⭐ 关键：修改所有方法中的硬编码 7
            else {
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.BIPUSH) {
                        IntInsnNode intInsn = (IntInsnNode) n;

                        if (intInsn.operand == 7) {
                            AbstractInsnNode next = intInsn.getNext();

                            // 检查是否是循环比较（for (i = 0; i < 7; i++)）
                            if (next != null && (
                                    next.getOpcode() == Opcodes.IF_ICMPLT ||
                                            next.getOpcode() == Opcodes.IF_ICMPGE ||
                                            next.getOpcode() == Opcodes.IF_ICMPGT ||
                                            next.getOpcode() == Opcodes.IF_ICMPLE)) {

                                intInsn.operand = totalSlots;
                                modified = true;
                                System.out.println("[moremodTransformer]     - " + mn.name + ": 7 -> " + totalSlots + " (loop limit)");
                            }
                        }
                    }
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[moremodTransformer]   ✓ BaublesContainer patched dynamically");
        return cw.toByteArray();
    }
    /**
     * ⭐ 新方法：修改方法中硬编码的槽位循环上限
     */
    private boolean patchHardcodedSlotLoops(MethodNode mn, int newSlotCount) {
        boolean modified = false;

        // 常见的需要 patch 的方法
        String[] targetMethods = {
                "isChanged",      // 最常见的问题
                "getSlots",
                "setChanged",
                "setStackInSlot",
                "getStackInSlot"
        };

        boolean isTargetMethod = false;
        for (String target : targetMethods) {
            if (mn.name.equals(target)) {
                isTargetMethod = true;
                break;
            }
        }

        if (!isTargetMethod && !mn.name.equals("getSlots")) {
            // 也 patch 任何包含循环的方法
            boolean hasLoop = false;
            for (AbstractInsnNode n : mn.instructions.toArray()) {
                if (n.getOpcode() == Opcodes.IF_ICMPLT ||
                        n.getOpcode() == Opcodes.IF_ICMPGE ||
                        n.getOpcode() == Opcodes.IF_ICMPGT) {
                    hasLoop = true;
                    break;
                }
            }
            if (!hasLoop) return false;
        }

        // 修改所有 BIPUSH 7 的指令
        for (AbstractInsnNode n : mn.instructions.toArray()) {
            if (n.getOpcode() == Opcodes.BIPUSH) {
                IntInsnNode intInsn = (IntInsnNode) n;

                // 将 7 替换为 newSlotCount
                if (intInsn.operand == 7) {
                    // 检查上下文：确保这是槽位数量而不是其他用途的 7
                    AbstractInsnNode next = intInsn.getNext();
                    AbstractInsnNode prev = intInsn.getPrevious();

                    // 如果后面是比较指令，很可能是循环上限
                    if (next != null && (
                            next.getOpcode() == Opcodes.IF_ICMPLT ||
                                    next.getOpcode() == Opcodes.IF_ICMPGE ||
                                    next.getOpcode() == Opcodes.IF_ICMPGT ||
                                    next.getOpcode() == Opcodes.IF_ICMPLE)) {

                        if (newSlotCount <= 127) {
                            intInsn.operand = newSlotCount;
                        } else {
                            mn.instructions.insert(intInsn, new IntInsnNode(Opcodes.SIPUSH, newSlotCount));
                            mn.instructions.remove(intInsn);
                        }
                        modified = true;
                    }
            }}  // ✅ 只有一个 }

            // 也处理 ICONST_0 到 ICONST_5 的情况（虽然 7 不会用这个）
            // 以及 ILOAD 后跟 getArrayLength 的模式
        }

        return modified;
    }

    // ============================================================
    // BaubleType 动态扩展（SetBonus 兼容版）
    // ============================================================
    private byte[] transformBaubleTypeDynamic(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("hasSlot")) {
                patchHasSlotDynamic(mn);
                modified = true;
                System.out.println("[moremodTransformer]   + patched hasSlot() dynamically");
            }

            if (mn.name.equals("<clinit>") && mn.desc.equals("()V")) {
                patchBaubleTypeClinitDynamic(mn);
                modified = true;
                System.out.println("[moremodTransformer]   + patched BaubleType <clinit> (SetBonus Compatible)");
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[moremodTransformer]   ✓ BaubleType patched with SetBonus compatibility");
        return cw.toByteArray();
    }

    // in com.moremod.core.moremodTransformer
    // in com.moremod.core.moremodTransformer
    private void patchHasSlotDynamic(MethodNode mn) {
        // 生成纯内联的 hasSlot(int) 逻辑（无外部依赖）：
        // if (slotId < 0) return false;
        // if (this == TRINKET) return true;
        // int[] arr = this.validSlots;
        // if (arr != null) {
        //   for (int i = 0; i < arr.length; i++) if (arr[i] == slotId) return true;
        // }
        // if (slotId >= 7) return true;  // 额外位全部放行（容器 Slot 再按类型校验）
        // return false;

        mn.instructions.clear();

        InsnList il = new InsnList();

        LabelNode L_nonNeg = new LabelNode();
        LabelNode L_notTrinket = new LabelNode();
        LabelNode L_arrNullOrDone = new LabelNode();
        LabelNode L_loopCheck = new LabelNode();
        LabelNode L_afterLoop = new LabelNode();
        LabelNode L_returnFalse = new LabelNode();

        // if (slotId < 0) return false;
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));         // slotId
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_nonNeg));
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.IRETURN));
        il.add(L_nonNeg);

        // if (this == TRINKET) return true;
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));         // this
        il.add(new FieldInsnNode(
                Opcodes.GETSTATIC,
                "baubles/api/BaubleType",
                "TRINKET",
                "Lbaubles/api/BaubleType;"
        ));
        il.add(new JumpInsnNode(Opcodes.IF_ACMPNE, L_notTrinket));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.IRETURN));
        il.add(L_notTrinket);

        // int[] arr = this.validSlots;
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new FieldInsnNode(
                Opcodes.GETFIELD,
                "baubles/api/BaubleType",
                "validSlots",
                "[I"
        ));
        il.add(new VarInsnNode(Opcodes.ASTORE, 2));        // local 2: int[] arr

        // if (arr != null) { for(...) { if (arr[i] == slotId) return true; } }
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new JumpInsnNode(Opcodes.IFNULL, L_arrNullOrDone));

        // i = 0
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ISTORE, 3));        // local 3: int i

        il.add(L_loopCheck);
        // i < arr.length ?
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new InsnNode(Opcodes.ARRAYLENGTH));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, L_arrNullOrDone));

        // if (arr[i] == slotId) return true;
        il.add(new VarInsnNode(Opcodes.ALOAD, 2));
        il.add(new VarInsnNode(Opcodes.ILOAD, 3));
        il.add(new InsnNode(Opcodes.IALOAD));
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));         // slotId
        LabelNode L_next = new LabelNode();
        il.add(new JumpInsnNode(Opcodes.IF_ICMPNE, L_next));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.IRETURN));
        il.add(L_next);

        // i++
        il.add(new IincInsnNode(3, 1));
        il.add(new JumpInsnNode(Opcodes.GOTO, L_loopCheck));

        // 循环结束 / arr == null
        il.add(L_arrNullOrDone);

        // if (slotId >= 7) return true;  // 额外位统一放行
        il.add(new VarInsnNode(Opcodes.ILOAD, 1));         // slotId
        il.add(new IntInsnNode(Opcodes.BIPUSH, 7));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPLT, L_returnFalse));
        il.add(new InsnNode(Opcodes.ICONST_1));
        il.add(new InsnNode(Opcodes.IRETURN));

        // return false;
        il.add(L_returnFalse);
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new InsnNode(Opcodes.IRETURN));

        mn.instructions.add(il);
        mn.maxStack = 4;
        mn.maxLocals = 4;

        System.out.println("[moremodTransformer]   + replaced BaubleType.hasSlot(int) with inlined logic (TRINKET any, extras>=7 allowed)");
    }




    /**
     * ⭐ SetBonus 兼容版：把所有额外槽位都加到 TRINKET.validSlots
     * 参考旧系统的成功经验
     */
    private void patchBaubleTypeClinitDynamic(MethodNode mn) {
        AbstractInsnNode[] instructions = mn.instructions.toArray();

        System.out.println("[moremodTransformer]     === 修改 BaubleType (SetBonus Compatible) ===");

        // 获取总额外槽位数
        int totalExtraSlots = EarlyConfigLoader.getTotalExtraSlots();

        if (totalExtraSlots == 0) {
            System.out.println("[moremodTransformer]     没有额外槽位，跳过修改");
            return;
        }

        // ⭐ 关键：创建 TRINKET 的 validSlots 数组
        // TRINKET.validSlots = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, ..., totalSlots-1]
        int totalSlots = 7 + totalExtraSlots;
        int[] trinketSlots = new int[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            trinketSlots[i] = i;
        }

        System.out.println("[moremodTransformer]     总槽位数: " + totalSlots);
        System.out.println("[moremodTransformer]     TRINKET.validSlots: [0-" + (totalSlots - 1) + "]");
        System.out.println("[moremodTransformer]     (SetBonus 将检测所有这些槽位)");

        // ⭐ 只修改 TRINKET 类型（其他类型保持原版）
        patchBaubleTypeArray(mn, instructions, "TRINKET", trinketSlots);

        System.out.println("[moremodTransformer]     ✓ TRINKET 已修改，SetBonus 兼容完成");
    }

    /**
     * 修改指定类型的 validSlots 数组
     */
    private void patchBaubleTypeArray(MethodNode mn, AbstractInsnNode[] instructions,
                                      String typeName, int[] newSlots) {
        for (int i = 0; i < instructions.length; i++) {
            AbstractInsnNode insn = instructions[i];

            if (insn.getOpcode() == Opcodes.LDC) {
                LdcInsnNode ldcInsn = (LdcInsnNode) insn;

                if (typeName.equals(ldcInsn.cst)) {
                    System.out.println("[moremodTransformer]       - 找到 " + typeName);

                    // 查找数组大小定义
                    AbstractInsnNode arraySizeNode = findNextArraySize(instructions, i);

                    if (arraySizeNode != null) {
                        int oldSize = getArraySize(arraySizeNode);
                        System.out.println("[moremodTransformer]         原始大小: " + oldSize);
                        System.out.println("[moremodTransformer]         新大小: " + newSlots.length);
                        System.out.println("[moremodTransformer]         槽位范围: " + formatSlots(newSlots));

                        // 修改数组大小
                        replaceArraySize(mn, arraySizeNode, newSlots.length);

                        // 查找初始化点
                        AbstractInsnNode initPoint = findArrayInitPoint(instructions, i, typeName);
                        if (initPoint != null) {
                            // 生成额外槽位的初始化代码
                            InsnList newInit = buildArrayInit(newSlots, oldSize);
                            mn.instructions.insertBefore(initPoint, newInit);
                            System.out.println("[moremodTransformer]         ✓ 已添加 " + (newSlots.length - oldSize) + " 个额外槽位的初始化");
                        } else {
                            System.err.println("[moremodTransformer]         ✗ 找不到初始化点！");
                        }
                    } else {
                        System.err.println("[moremodTransformer]         ✗ 找不到数组大小节点！");
                    }

                    break;
                }
            }
        }
    }

    // ============================================================
    // ContainerPlayerExpanded 注入
    // ============================================================
    private byte[] transformContainerPlayerExpanded(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<init>")) {
                AbstractInsnNode lastPop = null;
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.POP) {
                        lastPop = n;
                    }
                }

                if (lastPop != null) {
                    InsnList inject = new InsnList();

                    // this (Container)
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));

                    // playerInv (InventoryPlayer)
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));

                    // 获取 player - 从参数 3 (EntityPlayer)
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 3));

                    // 调用 BaublesApi.getBaublesHandler(player) 获取 IBaublesItemHandler
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "baubles/api/BaublesApi",
                            "getBaublesHandler",
                            "(Lnet/minecraft/entity/player/EntityPlayer;)Lbaubles/api/cap/IBaublesItemHandler;",
                            false
                    ));

                    // 调用注入器
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/accessorybox/unlock/UnlockableSlotInjector",
                            "injectUnlockedSlots",
                            "(Lnet/minecraft/inventory/Container;Lnet/minecraft/entity/player/InventoryPlayer;Lbaubles/api/cap/IBaublesItemHandler;)V",
                            false
                    ));

                    mn.instructions.insert(lastPop, inject);
                    modified = true;
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    // ============================================================
    // Enigmatic Legacy 修复
    // ============================================================
    private byte[] transformSuperpositionHandler(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("getPersistentBoolean")) {
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode methodInsn = (MethodInsnNode) n;
                        if (methodInsn.name.equals("getBoolean") && methodInsn.owner.equals("net/minecraft/nbt/NBTTagCompound")) {
                            InsnList wrapper = new InsnList();
                            wrapper.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "com/moremod/compat/EnigmaticSafetyWrapper",
                                    "safeGetBoolean",
                                    "(Lnet/minecraft/nbt/NBTTagCompound;Ljava/lang/String;)Z",
                                    false
                            ));
                            mn.instructions.insert(methodInsn, wrapper);
                            mn.instructions.remove(methodInsn);
                            modified = true;
                            System.out.println("[moremodTransformer]   + Wrapped getBoolean() call");
                        }
                    }
                }
            }
        }

        if (!modified) return bytes;
        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private byte[] transformPotionFingersRing(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        System.out.println("[moremodTransformer] Patching PotionFingers ItemRing...");

        for (MethodNode mn : cn.methods) {
            // 找到 updatePotionStatus 方法
            if (mn.name.equals("updatePotionStatus")) {
                System.out.println("[moremodTransformer]   Found updatePotionStatus method");

                // 查找 BaubleType.RING.getValidSlots() 调用
                for (AbstractInsnNode n : mn.instructions.toArray()) {
                    if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode min = (MethodInsnNode) n;

                        if (min.name.equals("getValidSlots") && min.owner.equals("baubles/api/BaubleType")) {
                            System.out.println("[moremodTransformer]     Found getValidSlots() call");

                            // 在 getValidSlots() 后面插入过滤代码
                            // 原始: int[] slots = BaubleType.RING.getValidSlots();
                            // 修改: int[] slots = filterValidSlots(BaubleType.RING.getValidSlots(), handler);

                            InsnList filter = new InsnList();

                            // 加载 handler (局部变量)
                            // 需要找到 handler 的局部变量索引
                            filter.add(new VarInsnNode(Opcodes.ALOAD, 5)); // IBaublesItemHandler inv 通常在 slot 5

                            // 调用我们的过滤方法
                            filter.add(new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    "com/moremod/compat/PotionFingersCompat",
                                    "filterValidSlots",
                                    "([ILbaubles/api/cap/IBaublesItemHandler;)[I",
                                    false
                            ));

                            mn.instructions.insert(min, filter);
                            modified = true;
                            System.out.println("[moremodTransformer]     Inserted slot filter");
                            break;
                        }
                    }
                }  // ✅ 只有一个 }
            }
        }  // ✅ for 循环结束

        if (!modified) {
            System.out.println("[moremodTransformer]   WARNING: Could not patch PotionFingers!");
            return bytes;
        }

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[moremodTransformer]   ✓ PotionFingers patched successfully");
        return cw.toByteArray();
    }

    private byte[] transformELBaublesStackHandler(byte[] bytes) {
        // 实现 EL Baubles handler 修复...
        return bytes;
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private AbstractInsnNode findNextArraySize(AbstractInsnNode[] instructions, int startIndex) {
        for (int i = startIndex; i < instructions.length && i < startIndex + 20; i++) {
            AbstractInsnNode insn = instructions[i];
            if ((insn.getOpcode() >= Opcodes.ICONST_0 && insn.getOpcode() <= Opcodes.ICONST_5) ||
                    insn.getOpcode() == Opcodes.BIPUSH || insn.getOpcode() == Opcodes.SIPUSH) {
                AbstractInsnNode next = insn.getNext();
                if (next != null && next.getOpcode() == Opcodes.NEWARRAY) {
                    return insn;
                }
            }
        }
        return null;
    }

    private int getArraySize(AbstractInsnNode node) {
        if (node.getOpcode() >= Opcodes.ICONST_0 && node.getOpcode() <= Opcodes.ICONST_5) {
            return node.getOpcode() - Opcodes.ICONST_0;
        } else if (node.getOpcode() == Opcodes.BIPUSH || node.getOpcode() == Opcodes.SIPUSH) {
            return ((IntInsnNode) node).operand;
        }
        return -1;
    }

    private void replaceArraySize(MethodNode mn, AbstractInsnNode oldNode, int newSize) {
        AbstractInsnNode newNode;
        if (newSize <= 5) {
            newNode = new InsnNode(Opcodes.ICONST_0 + newSize);
        } else if (newSize <= 127) {
            newNode = new IntInsnNode(Opcodes.BIPUSH, newSize);
        } else {
            newNode = new IntInsnNode(Opcodes.SIPUSH, newSize);
        }
        mn.instructions.insert(oldNode, newNode);
        mn.instructions.remove(oldNode);
    }

    private AbstractInsnNode findArrayInitPoint(AbstractInsnNode[] instructions, int startIndex, String typeName) {
        for (int i = startIndex; i < instructions.length && i < startIndex + 100; i++) {
            AbstractInsnNode insn = instructions[i];
            if (insn.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if ("<init>".equals(methodInsn.name) && "baubles/api/BaubleType".equals(methodInsn.owner)) {
                    AbstractInsnNode next = insn.getNext();
                    if (next != null && next.getOpcode() == Opcodes.PUTSTATIC) {
                        FieldInsnNode fieldInsn = (FieldInsnNode) next;
                        if (typeName.equals(fieldInsn.name)) {
                            return insn;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 生成额外槽位的数组初始化代码
     * 参考旧系统的成功实现
     */
    private InsnList buildArrayInit(int[] allSlots, int oldSize) {
        InsnList list = new InsnList();

        // 只初始化额外槽位（oldSize 之后的）
        for (int i = oldSize; i < allSlots.length; i++) {
            int arrayIndex = i;           // 在数组中的索引
            int slotValue = allSlots[i];  // 槽位ID值

            list.add(new InsnNode(Opcodes.DUP));  // 复制数组引用

            // 压入数组索引
            if (arrayIndex <= 5) {
                list.add(new InsnNode(Opcodes.ICONST_0 + arrayIndex));
            } else if (arrayIndex <= 127) {
                list.add(new IntInsnNode(Opcodes.BIPUSH, arrayIndex));
            } else {
                list.add(new IntInsnNode(Opcodes.SIPUSH, arrayIndex));
            }

            // 压入槽位值
            if (slotValue <= 5) {
                list.add(new InsnNode(Opcodes.ICONST_0 + slotValue));
            } else if (slotValue <= 127) {
                list.add(new IntInsnNode(Opcodes.BIPUSH, slotValue));
            } else {
                list.add(new IntInsnNode(Opcodes.SIPUSH, slotValue));
            }

            list.add(new InsnNode(Opcodes.IASTORE));  // 存储到数组

            // 调试输出
            if (i < oldSize + 5) {  // 只打印前5个
                System.out.println("[moremodTransformer]           validSlots[" + arrayIndex + "] = " + slotValue);
            } else if (i == oldSize + 5) {
                System.out.println("[moremodTransformer]           ... (" + (allSlots.length - oldSize) + " 个额外槽位)");
            }
        }

        return list;
    }

    private String formatSlots(int[] slots) {
        if (slots.length == 0) return "[]";
        if (slots.length <= 3) return java.util.Arrays.toString(slots);
        return "[" + slots[0] + ", ..., " + slots[slots.length - 1] + "]";
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className, false, this.getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ============================================================
    // 破碎之神死亡拦截转换（增强版）
    // ============================================================

    /**
     * 转换 EntityLivingBase 以支持破碎之神停机系统
     *
     * 注入点：
     * 1. attackEntityFrom HEAD - 停机时完全免疫攻击
     * 2. damageEntity HEAD - 检测致命伤害并触发停机
     * 3. onDeath HEAD - 最终防线
     */
    private byte[] transformEntityLivingBaseOnDeath(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);
        boolean modified = false;

        // 调试：打印所有方法（便于找到正确的方法名）
        System.out.println("[moremodTransformer]   Scanning EntityLivingBase methods...");
        for (MethodNode mn : cn.methods) {
            if (mn.desc.endsWith("F)Z") || mn.desc.endsWith("F)V") || mn.desc.endsWith(";)V")
                    || mn.name.contains("attack") || mn.name.contains("damage") || mn.name.contains("Death")
                    || mn.name.equals("func_70097_a") || mn.name.equals("func_70665_d") || mn.name.equals("func_70645_a")) {
                System.out.println("[moremodTransformer]     Method: " + mn.name + " " + mn.desc);
            }
        }

        for (MethodNode mn : cn.methods) {

            // ========== 1. attackEntityFrom 注入 ==========
            boolean isAttackEntityFrom = "attackEntityFrom".equals(mn.name)
                    || "func_70097_a".equals(mn.name)
                    || "a".equals(mn.name);

            if (isAttackEntityFrom && mn.desc.endsWith("F)Z") && mn.desc.startsWith("(L")) {
                System.out.println("[moremodTransformer]   Patching attackEntityFrom... (desc: " + mn.desc + ")");

                InsnList inject = new InsnList();
                LabelNode continueLabel = new LabelNode();

                // ===== 破碎之神：检查是否取消攻击 =====
                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new VarInsnNode(Opcodes.FLOAD, 2));
                inject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/moremod/core/BrokenGodDeathHook",
                        "shouldCancelAttack",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z",
                        false
                ));
                inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                inject.add(new InsnNode(Opcodes.ICONST_0));
                inject.add(new InsnNode(Opcodes.IRETURN));
                inject.add(continueLabel);

                // 香巴拉原始伤害捕获已移至 ShambhalaEventHandler.onLivingAttack (Forge事件)
                // LivingAttackEvent 在护甲计算前触发，比 ASM 更简单

                mn.instructions.insert(inject);
                modified = true;
                System.out.println("[moremodTransformer]     + Injected shutdown check at attackEntityFrom HEAD");
            }

            // ========== 2. damageEntity 注入 ==========
            boolean isDamageEntity = "damageEntity".equals(mn.name)
                    || "func_70665_d".equals(mn.name)
                    || "d".equals(mn.name);

            if (isDamageEntity && mn.desc.endsWith("F)V") && mn.desc.startsWith("(L")) {
                System.out.println("[moremodTransformer]   Patching damageEntity... (desc: " + mn.desc + ")");

                InsnList inject = new InsnList();
                LabelNode continueLabel = new LabelNode();

                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new VarInsnNode(Opcodes.FLOAD, 2));
                inject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/moremod/core/BrokenGodDeathHook",
                        "checkAndTriggerShutdown",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z",
                        false
                ));
                inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                inject.add(new InsnNode(Opcodes.RETURN));
                inject.add(continueLabel);

                if (ENABLE_SHAMBHALA_DEATH) {
                    LabelNode shambhalaContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new VarInsnNode(Opcodes.FLOAD, 2));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/ShambhalaDeathHook",
                            "checkAndAbsorbDamage",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, shambhalaContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(shambhalaContinue);
                    System.out.println("[moremodTransformer]     + Injected Shambhala damage absorption at damageEntity HEAD");
                }

                if (ENABLE_TEMPORAL_DEATH) {
                    LabelNode temporalContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new VarInsnNode(Opcodes.FLOAD, 2));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/TemporalDeathHook",
                            "checkAndTriggerRewind",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, temporalContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(temporalContinue);
                    System.out.println("[moremodTransformer]     + Injected Temporal rewind check at damageEntity HEAD");
                }

                // 七咒之戒-虚无之眸死亡保护
                if (ENABLE_CURSE_DEATH) {
                    LabelNode curseContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new VarInsnNode(Opcodes.FLOAD, 2));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/CurseDeathHook",
                            "checkAndPreventFatalDamage",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;F)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, curseContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(curseContinue);
                    System.out.println("[moremodTransformer]     + Injected Curse (Void Gaze) death prevention at damageEntity HEAD");
                }

                mn.instructions.insert(inject);
                modified = true;
                System.out.println("[moremodTransformer]     + Injected shutdown trigger check at damageEntity HEAD");
            }

            // ========== 3. onDeath 注入（最终防线） ==========
            boolean isOnDeath = "onDeath".equals(mn.name)
                    || "func_70645_a".equals(mn.name)
                    || ("a".equals(mn.name) && mn.desc.equals("(Lur;)V"));

            if (isOnDeath && mn.desc.endsWith(";)V") && mn.desc.startsWith("(L")) {
                System.out.println("[moremodTransformer]   Patching onDeath... (desc: " + mn.desc + ")");

                InsnList inject = new InsnList();
                LabelNode continueLabel = new LabelNode();

                inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                inject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/moremod/core/BrokenGodDeathHook",
                        "shouldPreventDeath",
                        "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z",
                        false
                ));
                inject.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
                inject.add(new InsnNode(Opcodes.RETURN));
                inject.add(continueLabel);

                if (ENABLE_SHAMBHALA_DEATH) {
                    LabelNode shambhalaContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/ShambhalaDeathHook",
                            "shouldPreventDeath",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, shambhalaContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(shambhalaContinue);
                    System.out.println("[moremodTransformer]     + Injected Shambhala death prevention at onDeath HEAD");
                }

                if (ENABLE_TEMPORAL_DEATH) {
                    LabelNode temporalContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/TemporalDeathHook",
                            "shouldPreventDeath",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, temporalContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(temporalContinue);
                    System.out.println("[moremodTransformer]     + Injected Temporal death prevention at onDeath HEAD");
                }

                // 七咒之戒-虚无之眸死亡保护（最终防线）
                if (ENABLE_CURSE_DEATH) {
                    LabelNode curseContinue = new LabelNode();
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    inject.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    inject.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/core/CurseDeathHook",
                            "shouldPreventDeath",
                            "(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/util/DamageSource;)Z",
                            false
                    ));
                    inject.add(new JumpInsnNode(Opcodes.IFEQ, curseContinue));
                    inject.add(new InsnNode(Opcodes.RETURN));
                    inject.add(curseContinue);
                    System.out.println("[moremodTransformer]     + Injected Curse (Void Gaze) death prevention at onDeath HEAD");
                }

                mn.instructions.insert(inject);
                modified = true;
                System.out.println("[moremodTransformer]     + Injected death prevention at onDeath HEAD");
            }
        }

        if (!modified) {
            System.out.println("[moremodTransformer]   WARNING: No methods were modified!");
            System.out.println("[moremodTransformer]   Available methods in EntityLivingBase:");
            for (MethodNode mn : cn.methods) {
                if (mn.name.contains("Death") || mn.name.contains("attack") || mn.name.contains("damage")
                        || mn.name.startsWith("func_706")) {
                    System.out.println("[moremodTransformer]     - " + mn.name + " " + mn.desc);
                }
            }
            return bytes;
        }

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[moremodTransformer]   ✓ EntityLivingBase patched for Broken God shutdown system");
        return cw.toByteArray();
    }
    // ============================================================
    // SafeClassWriter - 避免在转换期间加载类
    // ============================================================
    static class SafeClassWriter extends ClassWriter {
        public SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            // 完全避免类加载，防止 Mixin Re-entrance 错误
            // 在 coremod 转换期间不能安全地加载其他类
            return "java/lang/Object";
        }
    }
}