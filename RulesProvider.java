package com.example.lspadskip.core;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 跨进程 Provider。
 *
 * 作用：LSPosed 的 hook 代码运行在【目标应用】的进程里，
 * 而规则与设置保存在【本模块】应用。二者 UID 不同，
 * 因此通过一个 exported 的 ContentProvider 把数据以 JSON 返回给 hook 引擎。
 *
 * 查询方式（在目标进程内）：
 *  - content://com.example.lspadskip.rules/rules     -> 全部启用规则的 JSON 数组（列 "json"）
 *  - content://com.example.lspadskip.rules/settings  -> 全局设置 JSON（列 "json"）
 */
public class RulesProvider extends ContentProvider {

    public static final String AUTHORITY = "com.example.lspadskip.rules";
    public static final Uri RULES_URI = Uri.parse("content://" + AUTHORITY + "/rules");
    public static final Uri SETTINGS_URI = Uri.parse("content://" + AUTHORITY + "/settings");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (uri != null && "/settings".equals(uri.getPath())) {
            ModuleSettings s = SettingsStore.load(getContext());
            return singleRowCursor(s.toJson().toString());
        }

        // 默认返回规则
        JSONArray arr = new JSONArray();
        List<HookRule> rules = RulesStore.loadRules(getContext());
        for (HookRule r : rules) {
            if (r.enabled) {
                arr.put(r.toJson());
            }
        }
        return singleRowCursor(arr.toString());
    }

    private Cursor singleRowCursor(String json) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"json"});
        cursor.addRow(new Object[]{json});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "text/plain";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}
