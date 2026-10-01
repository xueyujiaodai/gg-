package com.example.lspadskip;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

/**
 * 检测实验室：说明隐身模式当前覆盖哪些检测向量、以及模块无法隐藏的框架级指纹，
 * 供你对照验证自有应用的 hook 检测是否健壮。
 */
public class DetectionLabActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);

        TextView tvCoverage = findViewById(R.id.tv_coverage);
        TextView tvRemaining = findViewById(R.id.tv_remaining);
        TextView tvUsage = findViewById(R.id.tv_usage);

        tvCoverage.setText(
                "开启隐身模式后，模块会针对以下常见检测向量打点（重启目标应用生效）：\n\n"
                + "1. 静默日志\n   关闭所有 XposedBridge 输出，logcat 无 Xposed/模块字样。\n\n"
                + "2. 栈帧清洗\n   hook Thread.getStackTrace()/Throwable.getStackTrace()，\n"
                + "    过滤 de.robv.android.xposed 与本模块栈帧。\n\n"
                + "3. 包管理器隐藏\n   getInstalledPackages/getInstalledApplications/getPackageInfo\n"
                + "    /getApplicationInfo 中移除本模块与 LSPosed 安装器。\n\n"
                + "4. 文件系统隐藏\n   File.exists/isFile/listFiles/list 隐藏模块 APK 与安装器路径。\n\n"
                + "5. /proc/self/maps 过滤\n   BufferedReader.readLine 清除 lspd/riru/zygisk/xposed 行。\n\n"
                + "6. 日志标签净化\n   Log.println 替换含 Xposed/模块字样的 tag。\n\n"
                + "7. DEX 字符串混淆\n   代码内无连续 de.robv.android.xposed 字面量（随编译生效）。");

        tvRemaining.setText(
                "以下指纹在模块进程之外，模块无法清除，你的应用仍可据此检出：\n\n"
                + "· LSPosed/Xposed 安装器包名与签名（即使被包管理隐藏，\n"
                + "  仍可能从应用详情页、备份、系统设置中发现）\n\n"
                + "· zygisk/riru 注入痕迹、so 库（本模块 maps 过滤只覆盖\n"
                + "  BufferedReader 读路径，直接 File 读取或 syscall 不受影响）\n\n"
                + "· Java 层 hook 的类加载器差异、方法计数变化、\n"
                + "  hook 方法的非原生实现痕迹\n\n"
                + "· 行为检测：规则一旦生效，广告行为会改变，可被观察发现\n\n"
                + "· /data/adb 下 lspd/riru/zygisk 相关文件本身仍存在");

        tvUsage.setText(
                "对照验证方法：\n"
                + "1. 在你的应用里实现检测：检查 Xposed 类、扫 logcat、抓调用栈、\n"
                + "   查已安装应用、读 /proc/self/maps。\n"
                + "2. 不开隐身 → 你的应用应当能检出；开隐身 → 若仍检出，说明\n"
                + "   用的是上面'框架级指纹'的检测（更健壮）；若不再检出，说明依赖\n"
                + "   的是模块已覆盖的向量，可针对性加固。\n"
                + "3. 不要以此误判为'绝对不可检测'——它只是模块可控制范围内的隐身。");
    }
}
