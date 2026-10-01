package com.example.lspadskip.core;

import org.json.JSONObject;

/**
 * 一条广告 hook 规则。
 *
 * mode 支持三种行为：
 *  - block         ：拦截目标方法，阻止其继续执行（setResult(null)）。
 *                    适用于“是否展示广告”“广告是否完成”“是否可跳过”这类判定方法。
 *  - override_return：用指定值覆盖目标方法的返回值（如把布尔判定改为 true/false）。
 *  - invoke_after  ：目标方法执行后，延迟 delayMs 毫秒再调用同一对象上的 targetMethod，
 *                    用于“跳过按钮出现后再自动点击 / 加速关闭广告页”等场景。
 */
public class HookRule {

    public String packageName;   // 目标应用包名
    public String className;     // 目标类全限定名
    public String methodName;    // 目标方法名
    public String mode;          // block / override_return / invoke_after
    public String targetMethod = ""; // invoke_after 模式下要调用的方法名
    public long delayMs = 0;     // invoke_after 模式的延迟毫秒数
    public boolean returnValue;  // override_return 模式下返回的值
    public boolean enabled = true;

    public HookRule() {
    }

    public HookRule(String packageName, String className, String methodName, String mode) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.mode = mode;
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("packageName", packageName);
            o.put("className", className);
            o.put("methodName", methodName);
            o.put("mode", mode);
            o.put("targetMethod", targetMethod);
            o.put("delayMs", delayMs);
            o.put("returnValue", returnValue);
            o.put("enabled", enabled);
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static HookRule fromJson(JSONObject o) {
        HookRule r = new HookRule();
        r.packageName = o.optString("packageName");
        r.className = o.optString("className");
        r.methodName = o.optString("methodName");
        r.mode = o.optString("mode", "block");
        r.targetMethod = o.optString("targetMethod");
        r.delayMs = o.optLong("delayMs");
        r.returnValue = o.optBoolean("returnValue");
        r.enabled = o.optBoolean("enabled", true);
        return r;
    }

    @Override
    public String toString() {
        return packageName + " | " + className + "#" + methodName + " [" + mode + "]"
                + (enabled ? "" : " (已停用)");
    }
}
