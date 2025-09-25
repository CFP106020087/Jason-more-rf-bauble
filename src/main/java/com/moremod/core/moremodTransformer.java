package com.moremod.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class moremodTransformer implements IClassTransformer {

    private static final boolean ENABLE_TOOLTIP_PATCH = true;
    private static final boolean ENABLE_BAUBLES_EXPANSION = true;

    public static Side side;

    static {
        side = FMLLaunchHandler.side();
        System.out.println("[moremodTransformer] Transformer loaded on side: " + side);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;

        try {
            if ("net.minecraft.enchantment.EnchantmentHelper".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching EnchantmentHelper...");
                return transformEnchantmentHelper(basicClass);
            }

            if (ENABLE_TOOLTIP_PATCH && side == Side.CLIENT && "net.minecraft.item.ItemStack".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching ItemStack (client tooltip)...");
                return transformItemStack(basicClass);
            }

            if (ENABLE_BAUBLES_EXPANSION && "baubles.api.cap.BaublesContainer".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching BaublesContainer for 38 slots...");
                return transformBaublesContainer(basicClass);
            }

            if (ENABLE_BAUBLES_EXPANSION && "keletu.enigmaticlegacy.api.bmtr.BaublesStackHandler".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching EL BaublesStackHandler...");
                return transformELBaublesStackHandler(basicClass);
            }

            // 添加對 BaubleType 的修改
            if (ENABLE_BAUBLES_EXPANSION && "baubles.api.BaubleType".equals(transformedName)) {
                System.out.println("[moremodTransformer] Patching BaubleType for extended TRINKET slots...");
                return transformBaubleType(basicClass);
            }

        } catch (Throwable t) {
            System.err.println("[moremodTransformer] Fatal error transforming " + transformedName);
            t.printStackTrace();
        }
        return basicClass;
    }

    // 修改 BaubleType 的 TRINKET 槽位
    private byte[] transformBaubleType(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("getValidSlots")) {
                if (patchGetValidSlots(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched BaubleType.getValidSlots()");
                }
            }
        }

        if (!modified) return bytes;

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean patchGetValidSlots(MethodNode mn) {
        // 在 getValidSlots 方法開頭添加檢查
        InsnList inject = new InsnList();

        // if (this == TRINKET) return new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this
        inject.add(new FieldInsnNode(Opcodes.GETSTATIC, "baubles/api/BaubleType", "TRINKET", "Lbaubles/api/BaubleType;"));
        LabelNode notTrinket = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ACMPNE, notTrinket));

        // 創建包含 0-20 的數組
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 22)); // 數組大小
        inject.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));

        for (int i = 0; i <= 21; i++) {
            inject.add(new InsnNode(Opcodes.DUP));
            inject.add(new IntInsnNode(Opcodes.BIPUSH, i)); // 索引
            inject.add(new IntInsnNode(Opcodes.BIPUSH, i)); // 值
            inject.add(new InsnNode(Opcodes.IASTORE));
        }

        inject.add(new InsnNode(Opcodes.ARETURN));
        inject.add(notTrinket);

        mn.instructions.insert(inject);
        return true;
    }

    private byte[] transformBaublesContainer(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("<init>")) {
                if (patchBaublesConstructor(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched BaublesContainer constructor");
                }
            }

            if (mn.name.equals("setSize")) {
                if (patchBaublesSetSize(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched BaublesContainer.setSize()");
                }
            }

            if (mn.name.equals("isItemValidForSlot")) {
                if (patchIsItemValidForSlot(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched isItemValidForSlot()");
                }
            }

            if (mn.name.equals("setStackInSlot")) {
                if (patchSetStackInSlot(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched setStackInSlot()");
                }
            }

            if (mn.name.equals("isChanged")) {
                if (patchIsChanged(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched isChanged()");
                }
            }

            if (mn.name.equals("setChanged")) {
                if (patchSetChanged(mn)) {
                    modified = true;
                    System.out.println("[moremodTransformer]   + patched setChanged()");
                }
            }
        }

        if (!modified) return bytes;

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean patchBaublesConstructor(MethodNode mn) {
        boolean patched = false;
        AbstractInsnNode[] arr = mn.instructions.toArray();

        for (AbstractInsnNode n : arr) {
            if (n.getOpcode() == Opcodes.BIPUSH || n.getOpcode() == Opcodes.SIPUSH) {
                IntInsnNode intInsn = (IntInsnNode) n;
                if (intInsn.operand == 7 || intInsn.operand == 14) {
                    System.out.println("[moremodTransformer]     Changing " + intInsn.operand + " -> 38 in constructor");
                    intInsn.operand = 39;
                    patched = true;
                }
            }
        }
        return patched;
    }

    private boolean patchBaublesSetSize(MethodNode mn) {
        InsnList inject = new InsnList();

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 38));
        LabelNode skipUpdate = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPGE, skipUpdate));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 38));
        inject.add(new VarInsnNode(Opcodes.ISTORE, 1));
        inject.add(skipUpdate);

        mn.instructions.insert(inject);
        return true;
    }

    private boolean patchIsChanged(MethodNode mn) {
        InsnList inject = new InsnList();

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        LabelNode checkUpper = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPGE, checkUpper));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        inject.add(new InsnNode(Opcodes.IRETURN));

        inject.add(checkUpper);
        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new FieldInsnNode(Opcodes.GETFIELD, "baubles/api/cap/BaublesContainer", "changed", "[Z"));
        inject.add(new InsnNode(Opcodes.ARRAYLENGTH));
        LabelNode continueNormal = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPLT, continueNormal));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        inject.add(new InsnNode(Opcodes.IRETURN));
        inject.add(continueNormal);

        mn.instructions.insert(inject);
        return true;
    }

    private boolean patchSetChanged(MethodNode mn) {
        InsnList inject = new InsnList();

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new InsnNode(Opcodes.ICONST_0));
        LabelNode checkUpper = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPGE, checkUpper));
        inject.add(new InsnNode(Opcodes.RETURN));

        inject.add(checkUpper);
        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new FieldInsnNode(Opcodes.GETFIELD, "baubles/api/cap/BaublesContainer", "changed", "[Z"));
        inject.add(new InsnNode(Opcodes.ARRAYLENGTH));
        LabelNode continueNormal = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPLT, continueNormal));
        inject.add(new InsnNode(Opcodes.RETURN));
        inject.add(continueNormal);

        mn.instructions.insert(inject);
        return true;
    }

    private boolean patchIsItemValidForSlot(MethodNode mn) {
        InsnList inject = new InsnList();

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 14));
        LabelNode continueNormal = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPLT, continueNormal));

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 21));
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPGT, continueNormal));

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 2));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 3));
        inject.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "com/moremod/accessorybox/AccessoryBoxHelper",
                "isValidForExtraSlot",
                "(ILnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EntityLivingBase;)Z",
                false
        ));
        inject.add(new InsnNode(Opcodes.IRETURN));

        inject.add(continueNormal);

        mn.instructions.insert(inject);
        return true;
    }

    private boolean patchSetStackInSlot(MethodNode mn) {
        InsnList inject = new InsnList();

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 14));
        LabelNode continueNormal = new LabelNode();
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPLT, continueNormal));

        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new IntInsnNode(Opcodes.BIPUSH, 21));
        inject.add(new JumpInsnNode(Opcodes.IF_ICMPGT, continueNormal));

        inject.add(new VarInsnNode(Opcodes.ALOAD, 0));
        inject.add(new VarInsnNode(Opcodes.ILOAD, 1));
        inject.add(new VarInsnNode(Opcodes.ALOAD, 2));
        inject.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraftforge/items/ItemStackHandler",
                "setStackInSlot",
                "(ILnet/minecraft/item/ItemStack;)V",
                false
        ));
        inject.add(new InsnNode(Opcodes.RETURN));

        inject.add(continueNormal);

        mn.instructions.insert(inject);
        return true;
    }

    private byte[] transformELBaublesStackHandler(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if (mn.name.equals("deserializeNBT")) {
                AbstractInsnNode[] arr = mn.instructions.toArray();
                for (AbstractInsnNode n : arr) {
                    if (n.getOpcode() == Opcodes.BIPUSH) {
                        IntInsnNode intInsn = (IntInsnNode) n;
                        if (intInsn.operand == 14) {
                            intInsn.operand = 38;
                            modified = true;
                            System.out.println("[moremodTransformer]   + patched EL deserializeNBT (14 -> 38)");
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

    // EnchantmentHelper 相關方法保持不變
    private byte[] transformEnchantmentHelper(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if ((mn.name.equals("getEnchantmentLevel") || mn.name.equals("func_77506_a"))
                    && mn.desc.equals("(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I")) {
                if (patchGetEnchantmentLevel_AstralStyle(mn)) {
                    modified = true;
                }
            }

            if ((mn.name.equals("getEnchantments") || mn.name.equals("func_82781_a"))
                    && mn.desc.equals("(Lnet/minecraft/item/ItemStack;)Ljava/util/Map;")) {
                if (patchGetEnchantments(mn)) {
                    modified = true;
                }
            }

            if (mn.name.equals("applyEnchantmentModifier") || mn.name.equals("func_77518_a")) {
                if (mn.desc.equals("(Lnet/minecraft/enchantment/EnchantmentHelper$IModifier;Lnet/minecraft/item/ItemStack;)V")) {
                    if (patchApplyEnchantmentModifier_Single(mn)) {
                        modified = true;
                    }
                }
            }
        }

        if (!modified) return bytes;

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean patchGetEnchantmentLevel_AstralStyle(MethodNode mn) {
        boolean patched = false;
        AbstractInsnNode[] arr = mn.instructions.toArray();
        for (AbstractInsnNode n : arr) {
            if (n.getOpcode() == Opcodes.IRETURN) {
                InsnList p = new InsnList();
                p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                p.add(new VarInsnNode(Opcodes.ALOAD, 1));
                p.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/moremod/enchantment/EnchantmentBoostHelper",
                        "getNewEnchantmentLevel",
                        "(ILnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I",
                        false
                ));
                mn.instructions.insertBefore(n, p);
                patched = true;
            }
        }
        return patched;
    }

    private boolean patchGetEnchantments(MethodNode mn) {
        boolean patched = false;
        AbstractInsnNode[] arr = mn.instructions.toArray();
        for (AbstractInsnNode n : arr) {
            if (n.getOpcode() == Opcodes.ARETURN) {
                InsnList p = new InsnList();
                p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                p.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "com/moremod/enchantment/EnchantmentBoostHelper",
                        "applyNewEnchantmentLevels",
                        "(Ljava/util/Map;Lnet/minecraft/item/ItemStack;)Ljava/util/Map;",
                        false
                ));
                mn.instructions.insertBefore(n, p);
                patched = true;
            }
        }
        return patched;
    }

    private boolean patchApplyEnchantmentModifier_Single(MethodNode mn) {
        boolean patched = false;
        AbstractInsnNode[] arr = mn.instructions.toArray();

        for (int i = 0; i < arr.length; i++) {
            AbstractInsnNode n = arr[i];
            if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) n;
                if ((call.name.equals("getEnchantmentTagList") || call.name.equals("func_77986_q"))
                        && call.desc.equals("()Lnet/minecraft/nbt/NBTTagList;")) {

                    InsnList p = new InsnList();
                    p.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    p.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/enchantment/EnchantmentBoostHelper",
                            "modifyEnchantmentTagsForCombat",
                            "(Lnet/minecraft/nbt/NBTTagList;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagList;",
                            false
                    ));
                    mn.instructions.insert(call, p);
                    patched = true;
                    break;
                }
            }
        }
        return patched;
    }

    private byte[] transformItemStack(byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, ClassReader.EXPAND_FRAMES);

        boolean modified = false;

        for (MethodNode mn : cn.methods) {
            if ((mn.name.equals("getTooltip") || mn.name.equals("func_82840_a"))) {
                if (patchTooltip_EnchTags(mn)) {
                    modified = true;
                }
            }
        }

        if (!modified) return bytes;

        ClassWriter cw = new SafeClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    private boolean patchTooltip_EnchTags(MethodNode mn) {
        boolean patched = false;
        AbstractInsnNode[] arr = mn.instructions.toArray();
        for (AbstractInsnNode n : arr) {
            if (n.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                MethodInsnNode call = (MethodInsnNode) n;
                if ((call.name.equals("getEnchantmentTagList") || call.name.equals("func_77986_q"))
                        && call.desc.equals("()Lnet/minecraft/nbt/NBTTagList;")) {
                    InsnList p = new InsnList();
                    p.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    p.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "com/moremod/enchantment/EnchantmentBoostHelper",
                            "modifyEnchantmentTagsForTooltip",
                            "(Lnet/minecraft/nbt/NBTTagList;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/nbt/NBTTagList;",
                            false
                    ));
                    mn.instructions.insert(call, p);
                    patched = true;
                    break;
                }
            }
        }
        return patched;
    }

    static class SafeClassWriter extends ClassWriter {
        public SafeClassWriter(int flags) { super(flags); }
        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            try {
                ClassLoader cl = this.getClass().getClassLoader();
                Class<?> c1 = Class.forName(type1.replace('/', '.'), false, cl);
                Class<?> c2 = Class.forName(type2.replace('/', '.'), false, cl);
                if (c1.isAssignableFrom(c2)) return type1;
                if (c2.isAssignableFrom(c1)) return type2;
                while (!c1.isAssignableFrom(c2)) {
                    c1 = c1.getSuperclass();
                    if (c1 == null) return "java/lang/Object";
                }
                return c1.getName().replace('.', '/');
            } catch (Throwable t) {
                return "java/lang/Object";
            }
        }
    }
}