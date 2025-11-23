// EquipmentTimeConfig.java - English Only Version (No Encoding Issues)
package com.moremod.config;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = "moremod", name = "moremod/equipment_time_restriction")
@Mod.EventBusSubscriber(modid = "moremod")
public class EquipmentTimeConfig {

    private static volatile boolean configInitialized = false;
    private static final Object INIT_LOCK = new Object();

    @Config.Comment({
            "========================================",
            "Mechanical Core Equipment Time Restriction",
            "Controls the time limit for first-time equipment",
            "========================================"
    })
    @Config.Name("Time Restriction Settings")
    public static TimeRestrictionSettings restriction = new TimeRestrictionSettings();

    @Config.Comment("Message configuration for players")
    @Config.Name("Message Settings")
    public static MessageSettings messages = new MessageSettings();

    public static class TimeRestrictionSettings {

        @Config.Comment({
                "Enable time restriction for mechanical core equipment",
                "true = Players must equip within time limit",
                "false = No restriction (all players can equip freely)"
        })
        @Config.Name("Enable Restriction")
        @Config.RequiresMcRestart
        public boolean enabled = true;

        @Config.Comment({
                "Time limit in seconds after first join",
                "Default: 600 seconds (10 minutes)",
                "Range: 60 to 86400 seconds"
        })
        @Config.Name("Time Limit (seconds)")
        @Config.RangeInt(min = 60, max = 86400)
        public int timeLimit = 600;

        @Config.Comment({
                "Show countdown warnings to players",
                "true = Display warnings at specific time points",
                "false = No warnings"
        })
        @Config.Name("Show Warnings")
        public boolean showWarnings = true;

        @Config.Comment({
                "Time points (in seconds) to display warnings",
                "Default: [300, 180, 60, 30, 10]",
                "Means: 5 min, 3 min, 1 min, 30 sec, 10 sec remaining"
        })
        @Config.Name("Warning Points")
        public int[] warningPoints = new int[]{300, 180, 60, 30, 10};

        @Config.Comment({
                "Allow admins to reset player restrictions",
                "true = Enable /moremod resetequiptime command",
                "false = Disable reset functionality"
        })
        @Config.Name("Allow Admin Reset")
        public boolean allowAdminReset = true;

        @Config.Comment({
                "Send welcome message on first join",
                "true = Send welcome message",
                "false = No message"
        })
        @Config.Name("Send Welcome Message")
        public boolean sendWelcome = true;
    }

    public static class MessageSettings {

        @Config.Comment({
                "Welcome message (supports color codes: &a &e &c &7)",
                "Use {time} as placeholder for time limit",
                "Use \\n for new lines (double backslash)"
        })
        @Config.Name("Welcome Message")
        public String welcome = "&aWelcome to the server!\\n&eNote: You must equip the Mechanical Core within &c{time}\\n&7or you will be permanently banned from using it!";

        @Config.Comment({
                "Ban message when time limit exceeded",
                "Supports color codes and \\n for new lines"
        })
        @Config.Name("Ban Message")
        public String banned = "&cYou missed the chance to equip Mechanical Core!\\n&7You did not equip it within the time limit.\\n&eContact admin for help.";
    }

    /**
     * Initialize config in preInit (first line)
     */
    public static void initialize() {
        synchronized (INIT_LOCK) {
            if (configInitialized) {
                return;
            }

            try {
                System.out.println("[EquipmentTimeConfig] Loading configuration...");

                // Sync from file to memory
                ConfigManager.sync("moremod", Config.Type.INSTANCE);

                configInitialized = true;

                System.out.println("[EquipmentTimeConfig] Configuration loaded successfully!");
                printConfig();

            } catch (Exception e) {
                System.err.println("[EquipmentTimeConfig] Failed to load configuration!");
                System.err.println("[EquipmentTimeConfig] Error: " + e.getMessage());
                e.printStackTrace();

                configInitialized = true;
                System.out.println("[EquipmentTimeConfig] Using default values");
            }
        }
    }

    /**
     * Auto-save when config changed via GUI
     */
    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!"moremod".equals(event.getModID())) {
            return;
        }

        try {
            System.out.println("[EquipmentTimeConfig] Config changed via GUI");
            ConfigManager.sync("moremod", Config.Type.INSTANCE);
            System.out.println("[EquipmentTimeConfig] Config saved");
            printConfig();

        } catch (Exception e) {
            System.err.println("[EquipmentTimeConfig] Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Print current configuration
     */
    private static void printConfig() {
        System.out.println("========================================");
        System.out.println("Current Configuration:");
        System.out.println("  Enabled: " + restriction.enabled);
        System.out.println("  Time Limit: " + restriction.timeLimit + " seconds");
        System.out.println("  Show Warnings: " + restriction.showWarnings);
        System.out.println("  Allow Admin Reset: " + restriction.allowAdminReset);
        System.out.println("  Send Welcome: " + restriction.sendWelcome);
        System.out.println("========================================");
    }

    /**
     * Safe access methods (auto-initialize)
     */
    public static boolean isEnabled() {
        ensureInitialized();
        return restriction.enabled;
    }

    public static int getTimeLimit() {
        ensureInitialized();
        return restriction.timeLimit;
    }

    public static boolean shouldShowWarnings() {
        ensureInitialized();
        return restriction.showWarnings;
    }

    public static boolean allowAdminReset() {
        ensureInitialized();
        return restriction.allowAdminReset;
    }

    public static boolean shouldSendWelcome() {
        ensureInitialized();
        return restriction.sendWelcome;
    }

    public static int[] getWarningPoints() {
        ensureInitialized();
        return restriction.warningPoints;
    }

    public static String getWelcomeMessage() {
        ensureInitialized();
        return messages.welcome;
    }

    public static String getBannedMessage() {
        ensureInitialized();
        return messages.banned;
    }

    /**
     * Ensure config is initialized before access
     */
    private static void ensureInitialized() {
        if (!configInitialized) {
            System.err.println("[EquipmentTimeConfig] Config accessed before initialization!");
            initialize();
        }
    }

    /**
     * Reload config (for command)
     */
    public static void reload() {
        synchronized (INIT_LOCK) {
            try {
                System.out.println("[EquipmentTimeConfig] Reloading config...");
                ConfigManager.sync("moremod", Config.Type.INSTANCE);
                System.out.println("[EquipmentTimeConfig] Reload successful");
                printConfig();

            } catch (Exception e) {
                System.err.println("[EquipmentTimeConfig] Reload failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Reset to default values (for command)
     */
    public static void resetToDefaults() {
        synchronized (INIT_LOCK) {
            try {
                System.out.println("[EquipmentTimeConfig] Resetting to defaults...");

                restriction.enabled = true;
                restriction.timeLimit = 600;
                restriction.showWarnings = true;
                restriction.warningPoints = new int[]{300, 180, 60, 30, 10};
                restriction.allowAdminReset = true;
                restriction.sendWelcome = true;

                messages.welcome = "&aWelcome to the server!\\n&eNote: You must equip the Mechanical Core within &c{time}\\n&7or you will be permanently banned from using it!";
                messages.banned = "&cYou missed the chance to equip Mechanical Core!\\n&7You did not equip it within the time limit.\\n&eContact admin for help.";

                ConfigManager.sync("moremod", Config.Type.INSTANCE);

                System.out.println("[EquipmentTimeConfig] Reset complete");
                printConfig();

            } catch (Exception e) {
                System.err.println("[EquipmentTimeConfig] Reset failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}