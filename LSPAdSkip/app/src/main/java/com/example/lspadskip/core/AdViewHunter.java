package com.example.lspadskip.core;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 主动广告引擎 v2：自动在目标 Activity 视图树里找到"跳过 / 关闭 / 倒计时"按钮并点击。
 *
 * 升级点：
 *  - 多维打分：综合 text / contentDescription / 控件 id / 是否可点击 / 尺寸，选出最像
 *    关闭按钮的控件（避免误点广告主体），而非只按文本一次匹配。
 *  - 循环扫描：在 onWindowFocusChanged 与 onResume 时启动，按固定间隔重试直到点击成功
 *    或 Activity 结束，因此"倒计时结束后才出现"的跳过按钮也能被点到。
 *  - 匹配词覆盖文本与资源 id 两类（skip/close/dismiss/countdown/timer 等）。
 * 仅当悬浮窗"自动关闭广告页"开关开启时启用。
 */
public final class AdViewHunter {

    private AdViewHunter() {
    }

    /** 文本/contentDescription 命中关键词。 */
    private static final String[] TEXT_KEYWORDS = {
            "跳过", "关闭", "知道了", "以后再说", "不再提醒", "关闭广告", "跳过广告",
            "x", "×", "✕"
    };

    /** 控件 id 命中关键词（小写匹配）。 */
    private static final String[] ID_KEYWORDS = {
            "skip", "close", "dismiss", "countdown", "timer", "close_ad",
            "iv_close", "btn_skip", "jump"
    };

    private static final int MAX_SCAN_ATTEMPTS = 15;
    private static final long SCAN_INTERVAL_MS = 350L;
    private static final int MAX_DEPTH = 14;

    private static volatile boolean setupDone = false;

    public static void setup(final ClassLoader cl) {
        if (setupDone) {
            return;
        }
        setupDone = true;
        try {
            Class<?> activity = XposedHelpers.findClass("android.app.Activity", cl);
            XposedHelpers.findAndHookMethod(activity, "onWindowFocusChanged", boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (Boolean.TRUE.equals(param.args[0])) {
                                scheduleScan(param.thisObject);
                            }
                        }
                    });
            XposedHelpers.findAndHookMethod(activity, "onResume", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    scheduleScan(param.thisObject);
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** 启动一次扫描循环：间隔重试，点击到目标或 Activity 结束即停止。 */
    private static void scheduleScan(final Object activityObj) {
        final Activity act = (Activity) activityObj;
        final Handler h = new Handler(Looper.getMainLooper());
        final int[] attempts = {0};
        Runnable scan = new Runnable() {
            @Override
            public void run() {
                if (act.isFinishing() || attempts[0] >= MAX_SCAN_ATTEMPTS) {
                    return;
                }
                attempts[0]++;
                View decor = null;
                try {
                    decor = act.getWindow().getDecorView();
                } catch (Throwable ignored) {
                }
                if (decor != null) {
                    View best = bestMatch(decor, 0);
                    if (best != null) {
                        try {
                            best.performClick();
                        } catch (Throwable ignored) {
                        }
                        return; // 点中一次即可，下次聚焦/恢复再扫
                    }
                }
                h.postDelayed(this, SCAN_INTERVAL_MS);
            }
        };
        h.postDelayed(scan, 250L);
    }

    /** 深度优先遍历，返回得分最高的候选控件。 */
    private static View bestMatch(View v, int depth) {
        if (v == null || depth > MAX_DEPTH) {
            return null;
        }
        View best = null;
        int bestScore = 0;
        int s = score(v);
        if (s > 0) {
            best = v;
            bestScore = s;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View c = bestMatch(g.getChildAt(i), depth + 1);
                if (c != null) {
                    int cs = score(c);
                    if (cs > bestScore) {
                        best = c;
                        bestScore = cs;
                    }
                }
            }
        }
        return best;
    }

    /** 给视图打分：越像"跳过/关闭按钮"分越高。 */
    private static int score(View v) {
        int s = 0;
        try {
            if (v.isClickable()) {
                s += 1;
            }
        } catch (Throwable ignored) {
        }
        try {
            String text = "";
            if (v instanceof TextView) {
                CharSequence t = ((TextView) v).getText();
                if (t != null) {
                    text = t.toString();
                }
            }
            String cd = "";
            try {
                CharSequence t = v.getContentDescription();
                if (t != null) {
                    cd = t.toString();
                }
            } catch (Throwable ignored) {
            }
            String rn = "";
            try {
                rn = v.getResources().getResourceName(v.getId());
            } catch (Throwable ignored) {
            }
            String low = (text + cd).toLowerCase();
            for (String k : TEXT_KEYWORDS) {
                if (low.contains(k)) {
                    s += 4;
                }
            }
            for (String k : ID_KEYWORDS) {
                if (rn.toLowerCase().contains(k)) {
                    s += 3;
                }
            }
            // 小尺寸更可能是关闭图标，加分
            try {
                if (v.getWidth() < 200 && v.getHeight() < 200) {
                    s += 1;
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return s;
    }
}
