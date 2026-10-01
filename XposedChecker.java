package com.example.lspadskip;

import com.example.lspadskip.core.Obf;

/**
 * 判断当前进程是否运行在 LSPosed / Xposed 环境中。
 * 通过尝试加载 XposedBridge 判定；仅在未注入时返回 false。
 * 类名经 Obf 逐字符构造，避免 DEX 中出现连续 "de.robv.android.xposed" 字面量。
 */
public final class XposedChecker {

    private XposedChecker() {
    }

    public static boolean isActive() {
        try {
            Class.forName(Obf.XP + ".XposedBridge", false,
                    XposedChecker.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
