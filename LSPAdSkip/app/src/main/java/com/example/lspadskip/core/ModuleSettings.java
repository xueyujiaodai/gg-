package com.example.lspadskip.core;

import org.json.JSONObject;

/**
 * 全局模块设置（由悬浮窗实时调节，hook 引擎跨进程读取）。
 */
public class ModuleSettings {

    public int accelFactor = 1;        // 加速倍数 1 ~ 20，1 表示不加速
    public boolean enableSkip = true;  // 总开关：跳过广告
    public boolean enableBlock = true; // 拦截广告展示
    public boolean enableAccel = true; // 加速倒计时 / 播放进度
    public boolean enableAutoClose = true; // 自动关闭广告页
    public boolean autoDetectSdk = true;   // 自动识别目标应用命中的广告 SDK

    /** 隐身/防检测测试模式：静默日志 + 清洗 Xposed 栈帧（用于验证自有应用能否检测到被 hook）。 */
    public boolean stealthEnabled = false;

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("accelFactor", accelFactor);
            o.put("enableSkip", enableSkip);
            o.put("enableBlock", enableBlock);
            o.put("enableAccel", enableAccel);
            o.put("enableAutoClose", enableAutoClose);
            o.put("autoDetectSdk", autoDetectSdk);
            o.put("stealthEnabled", stealthEnabled);
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static ModuleSettings fromJson(JSONObject o) {
        ModuleSettings s = new ModuleSettings();
        s.accelFactor = Math.max(1, o.optInt("accelFactor", 1));
        s.enableSkip = o.optBoolean("enableSkip", true);
        s.enableBlock = o.optBoolean("enableBlock", true);
        s.enableAccel = o.optBoolean("enableAccel", true);
        s.enableAutoClose = o.optBoolean("enableAutoClose", true);
        s.autoDetectSdk = o.optBoolean("autoDetectSdk", true);
        s.stealthEnabled = o.optBoolean("stealthEnabled", false);
        return s;
    }
}
