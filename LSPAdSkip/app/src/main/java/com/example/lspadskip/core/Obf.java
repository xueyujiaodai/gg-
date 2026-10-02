package com.example.lspadskip.core;

/**
 * 运行时字符串混淆。
 *
 * 目的：很多应用会扫描模块 DEX 的字符串常量，凡出现连续的
 * "de.robv.android.xposed"、"xposed"、"lspd" 或模块包名字样即判定被 hook。
 * 这里把这些关键字符串改为按字符构造，避免在 DEX 中形成连续字面量。
 */
public final class Obf {

    private Obf() {
    }

    /** "de.robv.android.xposed" —— 逐字符构造，避免连续字面量。 */
    public static final String XP = new String(new char[]{
            'd', 'e', '.', 'r', 'o', 'b', 'v', '.', 'a', 'n', 'd', 'r', 'o', 'i', 'd',
            '.', 'x', 'p', 'o', 's', 'e', 'd'});

    /** 模块自身包名（com.example.lspadskip，逐字符构造）。 */
    public static final String MOD_PKG = new String(new char[]{
            'c', 'o', 'm', '.', 'e', 'x', 'a', 'm', 'p', 'l', 'e', '.', 'l', 's', 'p',
            'a', 'd', 's', 'k', 'i', 'p'});

    /** 日志标签（用于日志净化识别）。 */
    public static final String TAG = new String(new char[]{
            'L', 'S', 'P', 'A', 'd', 'S', 'k', 'i', 'p'});

    public static String s(String a, String b) {
        return a + b;
    }
}
