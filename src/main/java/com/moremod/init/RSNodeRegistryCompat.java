package com.moremod.init;

import com.moremod.moremod;
import com.moremod.node.NetworkNodeCreativeWirelessTransmitter;
import com.moremod.tile.TileCreativeWirelessTransmitter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.lang.reflect.*;
import java.util.Arrays;

public final class RSNodeRegistryCompat {

    private RSNodeRegistryCompat() {}

    public static void registerAll() {
        // 1) TileEntity —— 显式注册
        GameRegistry.registerTileEntity(
                TileCreativeWirelessTransmitter.class,
                new ResourceLocation(moremod.MODID, "creative_wireless_transmitter")
        );

        // 2) 兼容不同 RS 版本的 API + Registry + Factory 签名
        try {
            // 2.1 获取 API 类（api 或 apiimpl）
            Class<?> apiClazz = tryLoadClass("com.raoulvdberge.refinedstorage.api.API");
            if (apiClazz == null) {
                apiClazz = tryLoadClass("com.raoulvdberge.refinedstorage.apiimpl.API");
            }
            if (apiClazz == null) {
                throw new IllegalStateException("Refined Storage API class not found (api.API or apiimpl.API)");
            }

            // 2.2 调用静态 API.instance()
            Method instanceM = apiClazz.getMethod("instance");
            Object apiInstance = instanceM.invoke(null);

            // 2.3 获取 NetworkNodeRegistry
            Method getRegistryM = apiInstance.getClass().getMethod("getNetworkNodeRegistry");
            Object registry = getRegistryM.invoke(apiInstance);
            Class<?> registryClass = registry.getClass();

            // 2.4 找到 add(...) 方法（两参）：(String/ResourceLocation, Factory)
            Method addMethod = null;
            for (Method m : registryClass.getMethods()) {
                if (!m.getName().equals("add")) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2) {
                    addMethod = m;
                    break;
                }
            }
            if (addMethod == null) {
                throw new NoSuchMethodException("NetworkNodeRegistry.add(...) not found");
            }

            // 2.5 解析参数类型
            Class<?> idParamType = addMethod.getParameterTypes()[0];
            Class<?> factoryIface = addMethod.getParameterTypes()[1];

            // 2.6 组装第一个参数：ID
            Object idArg;
            String idStr = NetworkNodeCreativeWirelessTransmitter.ID; // "moremod:creative_wireless_transmitter"
            if (idParamType == String.class) {
                idArg = idStr;
            } else if (idParamType.getName().equals("net.minecraft.util.ResourceLocation")) {
                // new ResourceLocation("moremod:creative_wireless_transmitter")
                Constructor<?> rlCtor = idParamType.getConstructor(String.class);
                idArg = rlCtor.newInstance(idStr);
            } else {
                throw new IllegalStateException("Unsupported id parameter type for add(...): " + idParamType.getName());
            }

            // 2.7 创建动态 Factory 代理（兼容 create/apply 的各种签名）
            Object factoryProxy = Proxy.newProxyInstance(
                    factoryIface.getClassLoader(),
                    new Class<?>[]{factoryIface},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        Class<?>[] p = method.getParameterTypes();

                        // 情况 1：create(World, BlockPos)
                        if (name.equals("create") && p.length == 2 &&
                                World.class.isAssignableFrom(p[0]) &&
                                BlockPos.class.isAssignableFrom(p[1])) {
                            World w = (World) args[0];
                            BlockPos pos = (BlockPos) args[1];
                            return new NetworkNodeCreativeWirelessTransmitter(w, pos);
                        }

                        // 情况 2：create(NBTTagCompound, World)
                        if (name.equals("create") && p.length == 2 &&
                                NBTTagCompound.class.isAssignableFrom(p[0]) &&
                                World.class.isAssignableFrom(p[1])) {
                            NBTTagCompound tag = (NBTTagCompound) args[0];
                            World w = (World) args[1];
                            BlockPos pos = readPos(tag);
                            NetworkNodeCreativeWirelessTransmitter node =
                                    new NetworkNodeCreativeWirelessTransmitter(w, pos);
                            if (tag != null) node.read(tag);
                            return node;
                        }

                        // 情况 3：apply(NBTTagCompound, World)（BiFunction 风格）
                        if (name.equals("apply") && p.length == 2 &&
                                NBTTagCompound.class.isAssignableFrom(p[0]) &&
                                World.class.isAssignableFrom(p[1])) {
                            NBTTagCompound tag = (NBTTagCompound) args[0];
                            World w = (World) args[1];
                            BlockPos pos = readPos(tag);
                            NetworkNodeCreativeWirelessTransmitter node =
                                    new NetworkNodeCreativeWirelessTransmitter(w, pos);
                            if (tag != null) node.read(tag);
                            return node;
                        }

                        // 情况 4：create(World, BlockPos, NBTTagCompound)
                        if (name.equals("create") && p.length == 3 &&
                                World.class.isAssignableFrom(p[0]) &&
                                BlockPos.class.isAssignableFrom(p[1]) &&
                                NBTTagCompound.class.isAssignableFrom(p[2])) {
                            World w = (World) args[0];
                            BlockPos pos = (BlockPos) args[1];
                            NBTTagCompound tag = (NBTTagCompound) args[2];
                            NetworkNodeCreativeWirelessTransmitter node =
                                    new NetworkNodeCreativeWirelessTransmitter(w, pos);
                            if (tag != null) node.read(tag);
                            return node;
                        }

                        // 情况 5：create(NBTTagCompound, World, BlockPos)（有些分支）
                        if (name.equals("create") && p.length == 3 &&
                                NBTTagCompound.class.isAssignableFrom(p[0]) &&
                                World.class.isAssignableFrom(p[1]) &&
                                BlockPos.class.isAssignableFrom(p[2])) {
                            NBTTagCompound tag = (NBTTagCompound) args[0];
                            World w = (World) args[1];
                            BlockPos pos = (BlockPos) args[2];
                            NetworkNodeCreativeWirelessTransmitter node =
                                    new NetworkNodeCreativeWirelessTransmitter(w, pos);
                            if (tag != null) node.read(tag);
                            return node;
                        }

                        // 兜底：处理 Object 本身的方法
                        if (method.getDeclaringClass() == Object.class) {
                            switch (name) {
                                case "toString":
                                    return "RSFactoryProxy@" + Integer.toHexString(System.identityHashCode(proxy));
                                case "hashCode":
                                    return System.identityHashCode(proxy);
                                case "equals":
                                    return proxy == args[0];
                            }
                        }
                        throw new UnsupportedOperationException(
                                "Unsupported factory method: " + method + " with args " + Arrays.toString(args));
                    }
            );

            // 2.8 调用 add(id, factory)
            addMethod.invoke(registry, idArg, factoryProxy);

            System.out.println("[moremod][RSCompat] Registered node factory for "
                    + NetworkNodeCreativeWirelessTransmitter.ID + " via " + sig(addMethod));

        } catch (Throwable t) {
            throw new RuntimeException("[moremod][RSCompat] Failed to register RS node factory", t);
        }
    }

    /* ---------------- helpers ---------------- */

    private static Class<?> tryLoadClass(String fqcn) {
        try {
            return Class.forName(fqcn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static BlockPos readPos(NBTTagCompound tag) {
        if (tag == null) return BlockPos.ORIGIN;
        if (tag.hasKey("x") && tag.hasKey("y") && tag.hasKey("z")) {
            return new BlockPos(tag.getInteger("x"), tag.getInteger("y"), tag.getInteger("z"));
        }
        // 某些环境写 posX/posY/posZ
        int x = tag.hasKey("posX") ? tag.getInteger("posX") : 0;
        int y = tag.hasKey("posY") ? tag.getInteger("posY") : 0;
        int z = tag.hasKey("posZ") ? tag.getInteger("posZ") : 0;
        return new BlockPos(x, y, z);
    }

    private static String sig(Method m) {
        return m.getName() + Arrays.toString(m.getParameterTypes());
    }
}
