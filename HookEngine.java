package com.example.lspadskip.core;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 规则引擎：负责在目标应用进程中加载规则与全局设置，查找目标类与方法并注入 hook。
 *
 * 设置开关（来自悬浮窗，经 RulesProvider 跨进程下发）：
 *  - enableSkip      ：总开关，关闭则整个模块对该包不生效
 *  - enableBlock     ：决定 block / override_return 类规则是否生效
 *  - enableAccel     ：决定 accel_param 类规则、以及 invoke_after 延迟的加速是否生效
 *  - enableAutoClose ：决定 invoke_after（自动关闭广告页）类规则是否生效
 *  - accelFactor     ：加速倍数，作用于 accel_param 参数缩放与 invoke_after 延迟缩短
 *  - stealthEnabled  ：隐身模式，静默日志 + 清洗 Xposed 栈帧（验证自有应用能否检测到被 hook）
 */
public class HookEngine {

    /** 混淆后的日志标签，避免 DEX 中出现模块连续字样。 */
    private static final String TAG = Obf.TAG;
    /** 隐身模式下是否关闭所有日志输出（消除 logcat 中 Xposed / 模块字样痕迹）。 */
    private static volatile boolean NO_LOG = false;

    private HookEngine() {
    }

    /** 统一日志出口；隐身模式下静默。 */
    private static void log(String msg) {
        if (NO_LOG) {
            return;
        }
        XposedBridge.log(msg);
    }

    public static void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        Context ctx = (Context) lpparam.context;
        if (ctx == null) {
            return;
        }
        ModuleSettings settings = readSettings(ctx);
        NO_LOG = settings.stealthEnabled;

        // 隐身模式：安装深度隐身打点（栈/包管理器/文件/maps/日志）
        if (settings.stealthEnabled) {
            StealthEngine.setup(lpparam.classLoader);
            log(TAG + " stealth mode ON for " + lpparam.packageName);
        }

        // 总开关：关闭则本包不处理
        if (!settings.enableSkip) {
            log(TAG + " skip disabled for " + lpparam.packageName);
            return;
        }

        // 主动广告引擎：自动点击"跳过/关闭"按钮
        if (settings.enableAutoClose) {
            AdViewHunter.setup(lpparam.classLoader);
        }

        // 广告 SDK 自动识别：告知命中了哪些 SDK，便于编写规则
        if (settings.autoDetectSdk) {
            List<String> sdks = AdSdkDetector.detect(lpparam.classLoader);
            if (!sdks.isEmpty()) {
                log(TAG + " detected ad SDK: " + sdks);
            }
        }

        List<HookRule> rules = loadRulesForPackage(lpparam);
        if (rules.isEmpty()) {
            return;
        }
        log(TAG + " apply " + rules.size() + " rule(s) to " + lpparam.packageName
                + " (factor x" + settings.accelFactor + ")");

        for (final HookRule rule : rules) {
            if (!modeEnabled(rule.mode, settings)) {
                continue;
            }
            try {
                Class<?> clazz = XposedHelpers.findClass(rule.className, lpparam.classLoader);
                XposedBridge.hookAllMethods(clazz, rule.methodName, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        applyRule(rule, settings, param);
                    }
                });
                log(TAG + " hooked: " + rule.className + "#" + rule.methodName
                        + " [" + rule.mode + "]");
            } catch (Throwable t) {
                log(TAG + " hook failed: " + rule.className + "#" + rule.methodName
                        + " -> " + t);
            }
        }
    }

    /** 根据全局设置判断某 mode 是否允许生效。 */
    private static boolean modeEnabled(String mode, ModuleSettings s) {
        switch (mode) {
            case "block":
            case "override_return":
                return s.enableBlock;
            case "invoke_after":
                return s.enableAutoClose;
            case "accel_param":
                return s.enableAccel;
            default:
                return true;
        }
    }

    private static ModuleSettings readSettings(Context ctx) {
        ModuleSettings s = new ModuleSettings();
        try {
            Cursor c = ctx.getContentResolver().query(
                    RulesProvider.SETTINGS_URI, null, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        String json = c.getString(c.getColumnIndexOrThrow("json"));
                        s = ModuleSettings.fromJson(new JSONObject(json));
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Throwable t) {
            log(TAG + " read settings failed: " + t);
        }
        return s;
    }

    private static List<HookRule> loadRulesForPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        List<HookRule> result = new ArrayList<>();
        try {
            Context ctx = (Context) lpparam.context;
            if (ctx == null) {
                return result;
            }
            Cursor cursor = ctx.getContentResolver().query(
                    RulesProvider.RULES_URI, null, null, null, null);
            if (cursor == null) {
                return result;
            }
            try {
                if (cursor.moveToFirst()) {
                    String json = cursor.getString(cursor.getColumnIndexOrThrow("json"));
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        HookRule r = HookRule.fromJson(o);
                        if (lpparam.packageName.equals(r.packageName)) {
                            result.add(r);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        } catch (Throwable t) {
            log(TAG + " read rules failed: " + t);
        }
        return result;
    }

    /** 按规则 mode + 全局设置执行对应动作。 */
    private static void applyRule(final HookRule rule, ModuleSettings settings,
                                  XC_MethodHook.MethodHookParam param) {
        switch (rule.mode) {
            case "block":
                param.setResult(null);
                log(TAG + " [block] " + rule.className + "#" + rule.methodName);
                break;

            case "override_return":
                param.setResult(rule.returnValue);
                log(TAG + " [override] " + rule.className + "#" + rule.methodName
                        + " -> " + rule.returnValue);
                break;

            case "invoke_after": {
                final Object thisObj = param.thisObject;
                if (thisObj == null || rule.targetMethod == null || rule.targetMethod.isEmpty()) {
                    return;
                }
                final Object[] args = param.args;
                long delay = rule.delayMs;
                if (settings.enableAccel && settings.accelFactor > 1) {
                    delay = delay / settings.accelFactor;
                }
                final long realDelay = delay;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            XposedHelpers.callMethod(thisObj, rule.targetMethod, args);
                            log(TAG + " [invoke_after] called " + rule.targetMethod
                                    + " after " + realDelay + "ms");
                        } catch (Throwable t) {
                            log(TAG + " [invoke_after] error: " + t);
                        }
                    }
                }, realDelay);
                break;
            }

            case "accel_param":
                if (settings.enableAccel && settings.accelFactor > 1
                        && param.args != null && param.args.length > 0
                        && param.args[0] instanceof Number) {
                    Number n = (Number) param.args[0];
                    double v = n.doubleValue() / settings.accelFactor;
                    param.args[0] = scaledBack(n, v);
                    log(TAG + " [accel] " + rule.className + "#" + rule.methodName
                            + " -> x" + settings.accelFactor + " ("
                            + n + " -> " + param.args[0] + ")");
                }
                break;

            default:
                break;
        }
    }

    /** 按原始参数类型还原缩放后的数值。 */
    private static Object scaledBack(Number original, double v) {
        if (original instanceof Integer) {
            return (int) Math.round(v);
        }
        if (original instanceof Long) {
            return Math.round(v);
        }
        if (original instanceof Float) {
            return (float) v;
        }
        return v; // double
    }
}
