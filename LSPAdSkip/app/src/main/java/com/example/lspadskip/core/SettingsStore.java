package com.example.lspadskip.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/**
 * 模块全局设置的持久化（存于本模块应用私有空间，
 * 由 RulesProvider 跨进程暴露给目标应用中的 hook 引擎）。
 */
public final class SettingsStore {

    private static final String PREFS = "settings";
    private static final String KEY = "settings_json";

    private SettingsStore() {
    }

    public static synchronized ModuleSettings load(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY, null);
            if (json == null) {
                return new ModuleSettings();
            }
            return ModuleSettings.fromJson(new JSONObject(json));
        } catch (Exception e) {
            return new ModuleSettings();
        }
    }

    public static synchronized void save(Context ctx, ModuleSettings settings) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, settings.toJson().toString())
                .apply();
    }
}
