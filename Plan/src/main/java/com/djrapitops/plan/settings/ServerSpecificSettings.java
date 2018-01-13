/*
 * Licence is provided in the jar as license.yml also here:
 * https://github.com/Rsl1122/Plan-PlayerAnalytics/blob/master/Plan/src/main/resources/license.yml
 */
package com.djrapitops.plan.settings;

import com.djrapitops.plan.Plan;
import com.djrapitops.plan.PlanBungee;
import com.djrapitops.plan.system.settings.Settings;
import com.djrapitops.plugin.api.config.Config;
import com.djrapitops.plugin.api.utility.log.Log;
import com.djrapitops.plugin.utilities.Verify;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * Bungee Config manager for Server Settings such as:
 * - WebServer Port
 * - ServerName
 * - Theme Base
 *
 * @author Rsl1122
 */
public class ServerSpecificSettings {

    public void addOriginalBukkitSettings(PlanBungee plugin, UUID serverUUID, Map<String, Object> settings) {
        try {
            Config config = plugin.getMainConfig();
            if (!Verify.isEmpty(config.getString("Servers." + serverUUID + ".ServerName"))) {
                return;
            }
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                config.set("Servers." + serverUUID + "." + entry.getKey(), entry.getValue());
            }
            config.save();
        } catch (IOException e) {
            Log.toLog(this.getClass().getName(), e);
        }
    }

    public static void updateSettings(Plan plugin, Map<String, String> settings) {
        Log.debug("Checking new settings..");
        Config config = plugin.getMainConfig();

        boolean changedSomething = false;
        for (Map.Entry<String, String> setting : settings.entrySet()) {
            try {
                String path = setting.getKey();
                if ("sender".equals(path)) {
                    continue;
                }
                String stringValue = setting.getValue();
                Object value = getValue(stringValue);
                String currentValue = config.getString(path);
                if (stringValue.equals(currentValue)) {
                    continue;
                }
                config.set(path, value);
                Log.debug("  " + path + ": " + value);
            } catch (NullPointerException ignored) {
            }
            changedSomething = true;
        }

        if (changedSomething) {
            try {
                config.save();
            } catch (IOException e) {
                Log.toLog("ServerSpecificSettings / ConfigSave", e);
            }
            Log.info("----------------------------------");
            Log.info("The Received Bungee Settings changed the config values, restarting Plan..");
            Log.info("----------------------------------");
            plugin.reloadPlugin(true);
        } else {
            Log.debug("Settings up to date");
        }
    }

    private static Object getValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return value;
    }

    private String getPath(UUID serverUUID, Settings setting) {
        String path = "Servers." + serverUUID;
        switch (setting) {
            case WEBSERVER_PORT:
                path += ".WebServerPort";
                break;
            case SERVER_NAME:
                path += ".ServerName";
                break;
            case THEME_BASE:
                path += ".ThemeBase";
                break;
            default:
                break;
        }
        return path;
    }

    public boolean getBoolean(UUID serverUUID, Settings setting) {
        Config config = PlanBungee.getInstance().getMainConfig();
        String path = getPath(serverUUID, setting);
        return config.getBoolean(path);
    }

    public String getString(UUID serverUUID, Settings setting) {
        Config config = PlanBungee.getInstance().getMainConfig();
        String path = getPath(serverUUID, setting);
        return config.getString(path);
    }

    public Integer getInt(UUID serverUUID, Settings setting) {
        Config config = PlanBungee.getInstance().getMainConfig();
        String path = getPath(serverUUID, setting);
        return config.getInt(path);
    }

    public void set(UUID serverUUID, Settings setting, Object value) throws IOException {
        Config config = PlanBungee.getInstance().getMainConfig();
        String path = getPath(serverUUID, setting);
        config.set(path, value);
        config.save();
    }
}