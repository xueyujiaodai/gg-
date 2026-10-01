package com.example.lspadskip.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则持久化：以 JSON 数组形式存于本模块应用私有 SharedPreferences，
 * 由 RulesProvider 跨进程暴露给目标应用中的 hook 引擎读取。
 */
public final class RulesStore {

    private static final String PREFS = "rules";
    private static final String KEY = "rules_json";

    private RulesStore() {
    }

    public static synchronized List<HookRule> loadRules(Context ctx) {
        List<HookRule> list = new ArrayList<>();
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String json = sp.getString(KEY, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                list.add(HookRule.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static synchronized void saveRules(Context ctx, List<HookRule> rules) {
        JSONArray arr = new JSONArray();
        for (HookRule r : rules) {
            arr.put(r.toJson());
        }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, arr.toString())
                .apply();
    }

    public static synchronized void addRule(Context ctx, HookRule rule) {
        List<HookRule> list = loadRules(ctx);
        list.add(rule);
        saveRules(ctx, list);
    }

    public static synchronized void clearRules(Context ctx) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY)
                .apply();
    }
}
