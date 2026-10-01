package com.example.lspadskip.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 广告 SDK 自动识别。
 *
 * 通过检查目标应用类加载器中是否存在各广告 SDK 的签名类，判断命中了哪些 SDK。
 * 识别结果用于：告知你该为哪些广告 SDK 编写/启用 hook 规则，减少逆向定位工作量。
 *
 * 注意：SDK 内部类名随版本会变化，这里仅收录较稳定的入口类。规则的具体方法名
 * 仍需以目标应用实际加载的 SDK 版本为准（可先用 Jadx 反查确认）。
 */
public final class AdSdkDetector {

    private AdSdkDetector() {
    }

    /** SDK 名称 -> 签名类（任一存在即判定命中）。 */
    private static final Map<String, String[]> PROFILES = new LinkedHashMap<>();

    static {
        PROFILES.put("穿山甲/Pangle", new String[]{
                "com.bytedance.sdk.openadsdk.TTAdSdk",
                "com.bytedance.sdk.openadsdk.AdSlot"});
        PROFILES.put("优量汇/GDT", new String[]{
                "com.qq.e.comm.managers.GDTAdSdk",
                "com.qq.e.ads.ADActivity"});
        PROFILES.put("AdMob", new String[]{
                "com.google.android.gms.ads.MobileAds"});
        PROFILES.put("Mintegral", new String[]{
                "com.mbridge.msdk.MBridgeConstans",
                "com.mbridge.msdk.out.MBAdSDK"});
        PROFILES.put("ironSource", new String[]{
                "com.ironsource.mediationsdk.IronSource"});
        PROFILES.put("AppLovin", new String[]{
                "com.applovin.sdk.AppLovinSdk"});
        PROFILES.put("Unity Ads", new String[]{
                "com.unity3d.ads.UnityAds"});
        PROFILES.put("百度/百青藤", new String[]{
                "com.baidu.mobads.SdkManager"});
        PROFILES.put("Vungle", new String[]{
                "com.vungle.warren.Vungle"});
        PROFILES.put("MoPub", new String[]{
                "com.mopub.mobileads.MoPub"});
        PROFILES.put("Inmobi", new String[]{
                "com.inmobi.ads.InMobiAdRequest"});
        PROFILES.put("Smaato", new String[]{
                "com.smaato.sdk.core.SmaatoSdk"});
    }

    /** 返回在目标类加载器中命中的 SDK 名称列表。 */
    public static List<String> detect(ClassLoader cl) {
        List<String> found = new ArrayList<>();
        if (cl == null) {
            return found;
        }
        for (Map.Entry<String, String[]> e : PROFILES.entrySet()) {
            try {
                for (String cls : e.getValue()) {
                    try {
                        Class.forName(cls, false, cl);
                        found.add(e.getKey());
                        break;
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return found;
    }
}
