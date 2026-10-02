package com.example.lspadskip;

import com.example.lspadskip.core.HookEngine;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed 模块入口。
 * 在 LSPosed / EdXposed 中启用本模块并勾选目标应用后，
 * 目标应用启动时会回调此类的 handleLoadPackage，进而由 HookEngine 应用广告规则。
 */
public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 交给规则引擎统一处理
        HookEngine.handleLoadPackage(lpparam);
    }
}
