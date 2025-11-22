package com.moremod.module.impl;

import com.moremod.module.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模块容器默认实现
 *
 * 特性:
 * - 线程安全的模块注册
 * - 依赖解析
 * - 失败安全（单个模块失败不影响其他模块）
 */
public class ModuleContainerImpl implements IModuleContainer {

    private final Map<String, IModule> modules = new ConcurrentHashMap<>();
    private final Map<String, ModuleState> moduleStates = new ConcurrentHashMap<>();
    private final boolean debug;

    public ModuleContainerImpl() {
        this(false);
    }

    public ModuleContainerImpl(boolean debug) {
        this.debug = debug;
    }

    @Override
    public boolean registerModule(@Nonnull IModule module) {
        try {
            String id = module.getModuleId();
            if (modules.containsKey(id)) {
                log("warn", "Module already registered: " + id);
                return false;
            }

            modules.put(id, module);
            moduleStates.put(id, ModuleState.REGISTERED);
            log("info", "Module registered: " + id);
            return true;
        } catch (Throwable t) {
            log("error", "Failed to register module: " + t.getMessage());
            return false;
        }
    }

    @Override
    public boolean unregisterModule(@Nonnull String moduleId) {
        try {
            IModule module = modules.remove(moduleId);
            moduleStates.remove(moduleId);
            if (module != null) {
                log("info", "Module unregistered: " + moduleId);
                return true;
            }
            return false;
        } catch (Throwable t) {
            log("error", "Failed to unregister module " + moduleId + ": " + t.getMessage());
            return false;
        }
    }

    @Nullable
    @Override
    public IModule getModule(@Nonnull String moduleId) {
        return modules.get(moduleId);
    }

    @Override
    public boolean hasModule(@Nonnull String moduleId) {
        return modules.containsKey(moduleId);
    }

    @Nonnull
    @Override
    public Collection<IModule> getAllModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    @Nonnull
    @Override
    public Collection<IModule> getActiveModules() {
        List<IModule> active = new ArrayList<>();
        for (IModule module : modules.values()) {
            if (module.isActive()) {
                active.add(module);
            }
        }
        return active;
    }

    @Override
    public void initializeAll(@Nonnull IModuleContext context) {
        // 按优先级排序
        List<IModule> sorted = sortModulesByPriority();

        for (IModule module : sorted) {
            try {
                String id = module.getModuleId();
                if (moduleStates.get(id) == ModuleState.REGISTERED) {
                    log("debug", "Initializing module: " + id);
                    boolean success = module.init(context);
                    if (success) {
                        moduleStates.put(id, ModuleState.INITIALIZED);
                        log("info", "Module initialized: " + id);
                    } else {
                        log("warn", "Module initialization failed: " + id);
                    }
                }
            } catch (Throwable t) {
                log("error", "Exception during module initialization: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Override
    public void loadAll(@Nonnull IModuleContext context) {
        for (IModule module : modules.values()) {
            try {
                String id = module.getModuleId();
                ModuleState state = moduleStates.get(id);
                if (state == ModuleState.INITIALIZED || state == ModuleState.LOADED) {
                    log("debug", "Loading module: " + id);
                    boolean success = module.load(context);
                    if (success) {
                        moduleStates.put(id, ModuleState.LOADED);
                        log("info", "Module loaded: " + id);
                    } else {
                        log("warn", "Module load failed: " + id);
                    }
                }
            } catch (Throwable t) {
                log("error", "Exception during module load: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Override
    public void unloadAll(@Nonnull IModuleContext context) {
        // 反向卸载（后加载的先卸载）
        List<IModule> sorted = sortModulesByPriority();
        Collections.reverse(sorted);

        for (IModule module : sorted) {
            try {
                log("debug", "Unloading module: " + module.getModuleId());
                module.unload(context);
                moduleStates.put(module.getModuleId(), ModuleState.INITIALIZED);
            } catch (Throwable t) {
                log("error", "Exception during module unload: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Override
    public void attachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        for (IModule module : modules.values()) {
            try {
                String id = module.getModuleId();
                ModuleState state = moduleStates.get(id);
                if (state == ModuleState.LOADED) {
                    log("debug", "Attaching module to host: " + id);
                    boolean success = module.attach(host, context);
                    if (success) {
                        moduleStates.put(id, ModuleState.ATTACHED);
                    }
                }
            } catch (Throwable t) {
                log("error", "Exception during module attach: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Override
    public void detachAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        for (IModule module : modules.values()) {
            try {
                String id = module.getModuleId();
                ModuleState state = moduleStates.get(id);
                if (state == ModuleState.ATTACHED) {
                    log("debug", "Detaching module from host: " + id);
                    module.detach(host, context);
                    moduleStates.put(id, ModuleState.LOADED);
                }
            } catch (Throwable t) {
                log("error", "Exception during module detach: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Override
    public void tickAll(@Nonnull IModuleHost host, @Nonnull IModuleContext context) {
        for (IModule module : modules.values()) {
            try {
                String id = module.getModuleId();
                ModuleState state = moduleStates.get(id);
                if (state == ModuleState.ATTACHED && module.isActive()) {
                    module.onTick(host, context);
                }
            } catch (Throwable t) {
                log("error", "Exception during module tick: " + module.getModuleId() + " - " + t.getMessage());
                t.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public Object sendMessage(@Nonnull String senderId, @Nullable String targetId,
                              @Nonnull Object message, @Nonnull IModuleContext context) {
        try {
            if (targetId != null) {
                // 单播
                IModule target = modules.get(targetId);
                if (target != null && target.isActive()) {
                    return target.handleMessage(senderId, message, context);
                }
            } else {
                // 广播
                for (IModule module : modules.values()) {
                    if (module.isActive() && !module.getModuleId().equals(senderId)) {
                        Object result = module.handleMessage(senderId, message, context);
                        if (result != null) {
                            return result;  // 返回第一个非null响应
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log("error", "Exception during message send: " + t.getMessage());
            t.printStackTrace();
        }
        return null;
    }

    /**
     * 按优先级排序模块
     */
    private List<IModule> sortModulesByPriority() {
        List<IModule> sorted = new ArrayList<>(modules.values());
        sorted.sort((a, b) -> {
            IModuleDescriptor da = a.getDescriptor();
            IModuleDescriptor db = b.getDescriptor();
            if (da != null && db != null) {
                return Integer.compare(da.getPriority(), db.getPriority());
            }
            return 0;
        });
        return sorted;
    }

    private void log(String level, String message) {
        if (debug || !"debug".equals(level)) {
            System.out.println("[ModuleContainer] [" + level.toUpperCase() + "] " + message);
        }
    }

    /**
     * 模块状态枚举
     */
    private enum ModuleState {
        REGISTERED,      // 已注册
        INITIALIZED,     // 已初始化
        LOADED,          // 已加载
        ATTACHED,        // 已附加到宿主
    }
}
