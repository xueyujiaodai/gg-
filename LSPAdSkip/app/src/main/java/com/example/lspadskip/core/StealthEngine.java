package com.example.lspadskip.core;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * 深度隐身 / 防检测引擎（进阶版）。
 *
 * 用于验证自有应用能否检测到自己被本模块 hook。开启隐身模式后打点六路：
 *  1. 栈帧清洗  ：Thread.getStackTrace() / Throwable.getStackTrace() 过滤 Xposed 与模块帧。
 *  2. 包管理器隐藏：getInstalledPackages / getInstalledApplications / getPackageInfo /
 *                 getApplicationInfo 中移除模块与 Xposed/LSPosed 安装器包。
 *  3. 文件系统隐藏：File.exists / isFile / listFiles / list 等隐藏模块 APK 与安装器路径。
 *  4. /proc/self/maps 过滤：BufferedReader.readLine 清除 lspd / riru / zygisk / xposed 行。
 *  5. 日志净化   ：android.util.Log.println 替换含 Xposed/模块字样的 tag。
 *  6. 字符串混淆 ：Obf 保证 DEX 中无连续 "de.robv.android.xposed" 字面量（随模块编译即生效）。
 *
 * 局限：zygisk/riru 注入、LSPosed 管理器进程、模块 APK 安装记录等框架级指纹无法在模块内清除，
 * 详见 README「局限性与自检清单」。
 */
public final class StealthEngine {

    private StealthEngine() {
    }

    private static volatile boolean setupDone = false;

    /** 栈/日志过滤的类名前缀。 */
    private static final String[] STACK_PREFIX = {Obf.XP, Obf.MOD_PKG};

    /** 需从包管理器 / 文件系统隐藏的包名。 */
    private static final Set<String> HIDE_PKGS = new HashSet<>(Arrays.asList(
            Obf.MOD_PKG,
            "org.lsposed.manager",
            "de.robv.android.xposed.installer",
            "com.android.shell",
            "com.tsng.hidemyapplist"
    ));

    /** maps 行过滤关键词。 */
    private static final String[] MAPS_MARK = {"lspd", "riru", "zygisk", "xposed", Obf.MOD_PKG};

    /** 安装深度隐身打点（每进程仅执行一次）。 */
    public static void setup(ClassLoader cl) {
        if (setupDone) {
            return;
        }
        setupDone = true;
        installStackClean();
        installPackageManagerHide(cl);
        installFileHide();
        installMapsHide();
        installLogSanitize();
    }

    // ---------------- 1. 栈帧清洗 ----------------
    private static void installStackClean() {
        try {
            XposedHelpers.findAndHookMethod(Thread.class, "getStackTrace",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(cleanStack((StackTraceElement[]) param.getResult()));
                        }
                    });
        } catch (Throwable ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(Throwable.class, "getStackTrace",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            param.setResult(cleanStack((StackTraceElement[]) param.getResult()));
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static StackTraceElement[] cleanStack(StackTraceElement[] in) {
        if (in == null || in.length == 0) {
            return in;
        }
        List<StackTraceElement> out = new ArrayList<>(in.length);
        for (StackTraceElement e : in) {
            if (e != null && !hitPrefix(e.getClassName(), STACK_PREFIX)) {
                out.add(e);
            }
        }
        return out.toArray(new StackTraceElement[0]);
    }

    private static boolean hitPrefix(String s, String[] prefixes) {
        if (s == null) {
            return false;
        }
        for (String p : prefixes) {
            if (s.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    // ---------------- 2. 包管理器隐藏 ----------------
    private static void installPackageManagerHide(ClassLoader cl) {
        try {
            Class<?> pm = XposedHelpers.findClass("android.app.ApplicationPackageManager", cl);

            XposedHelpers.findAndHookMethod(pm, "getInstalledPackages", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            List<?> list = (List<?>) param.getResult();
                            if (list == null) {
                                return;
                            }
                            for (int i = list.size() - 1; i >= 0; i--) {
                                Object o = list.get(i);
                                if (o instanceof PackageInfo
                                        && HIDE_PKGS.contains(((PackageInfo) o).packageName)) {
                                    list.remove(i);
                                }
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(pm, "getInstalledApplications", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            List<?> list = (List<?>) param.getResult();
                            if (list == null) {
                                return;
                            }
                            for (int i = list.size() - 1; i >= 0; i--) {
                                Object o = list.get(i);
                                if (o instanceof ApplicationInfo
                                        && HIDE_PKGS.contains(((ApplicationInfo) o).packageName)) {
                                    list.remove(i);
                                }
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(pm, "getPackageInfo", String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String pkg = (String) param.args[0];
                            if (pkg != null && HIDE_PKGS.contains(pkg)) {
                                param.setResult(null);
                            }
                        }
                    });

            XposedHelpers.findAndHookMethod(pm, "getApplicationInfo", String.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String pkg = (String) param.args[0];
                            if (pkg != null && HIDE_PKGS.contains(pkg)) {
                                param.setResult(null);
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    // ---------------- 3. 文件系统隐藏 ----------------
    private static void installFileHide() {
        Class<?> f = File.class;
        XC_MethodHook hideIfMatches = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                File file = (File) param.thisObject;
                if (file != null && isHiddenPath(file.getAbsolutePath())) {
                    java.lang.reflect.Method m = (param.method instanceof java.lang.reflect.Method)
                            ? (java.lang.reflect.Method) param.method : null;
                    param.setResult(m != null && m.getReturnType() == long.class ? 0L : false);
                }
            }
        };
        try {
            XposedHelpers.findAndHookMethod(f, "exists", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "isFile", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "isDirectory", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "canRead", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "canWrite", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "canExecute", hideIfMatches);
            XposedHelpers.findAndHookMethod(f, "length", hideIfMatches);
        } catch (Throwable ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(f, "listFiles", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    File[] fs = (File[]) param.getResult();
                    if (fs == null) {
                        return;
                    }
                    List<File> keep = new ArrayList<>();
                    for (File x : fs) {
                        if (!isHiddenPath(x.getAbsolutePath())) {
                            keep.add(x);
                        }
                    }
                    param.setResult(keep.toArray(new File[0]));
                }
            });
        } catch (Throwable ignored) {
        }
        try {
            XposedHelpers.findAndHookMethod(f, "list", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String[] ss = (String[]) param.getResult();
                    if (ss == null) {
                        return;
                    }
                    List<String> keep = new ArrayList<>();
                    for (String s : ss) {
                        if (!isHiddenPath(s)) {
                            keep.add(s);
                        }
                    }
                    param.setResult(keep.toArray(new String[0]));
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean isHiddenPath(String abs) {
        if (abs == null) {
            return false;
        }
        for (String p : HIDE_PKGS) {
            if (abs.contains(p)) {
                return true;
            }
        }
        return false;
    }

    // ---------------- 4. /proc/self/maps 过滤 ----------------
    private static void installMapsHide() {
        try {
            XposedHelpers.findAndHookMethod(BufferedReader.class, "readLine", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String line = (String) param.getResult();
                    if (line != null && mapsMarked(line)) {
                        // 用空行替换命中行，避免中断流
                        param.setResult("");
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static boolean mapsMarked(String line) {
        for (String m : MAPS_MARK) {
            if (line.contains(m)) {
                return true;
            }
        }
        return false;
    }

    // ---------------- 5. 日志标签净化 ----------------
    private static void installLogSanitize() {
        try {
            XposedHelpers.findAndHookMethod(Log.class, "println",
                    int.class, String.class, String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String tag = (String) param.args[1];
                            if (tag != null && revealsXposed(tag)) {
                                param.args[1] = "System.out";
                            }
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    private static boolean revealsXposed(String tag) {
        return hitPrefix(tag, STACK_PREFIX) || tag.contains("Xposed") || tag.contains(Obf.TAG);
    }
}
