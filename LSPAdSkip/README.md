# LSPAdSkip Pro — LSPosed 广告规则加速模块（进阶版）

一个基于 **LSPosed (LSP)** 框架的 Android Xposed 模块。它按"规则"的方式 hook 目标应用里的广告逻辑，实现**跳过广告视频 / 加速广告倒计时 / 拦截广告展示**等行为；同时内置**深度隐身引擎**与**检测实验室**，用于验证你的自有应用能否检测到被 hook。

> 🧩 独立包名 `com.example.lspadskip`，与其它应用/模块（如 `Bili调速`）**共存、互不覆盖安装**。
> 默认作用域指向 B 站 `tv.danmaku.bili`。内置「B 站小游戏广告示例」规则模板，用于加速
> 激励广告（看完领奖励），帮助更快领取 B 站小游戏奖励。示例的类名/方法名是流程模板，
> 需按实际 B 站版本的广告 SDK 核对（用 Jadx 反查，或开"自动识别广告 SDK"看日志）。
> 完整功能含：规则引擎、悬浮窗、AdViewHunter 主动关广告、AdSdkDetector 自动识别、
> **深度隐身引擎**与**检测实验室**。

> ⚠️ 声明：本工程为技术学习与个人本地使用用途。hook 第三方应用可能违反其服务条款，请仅用于你有权控制或明确授权的应用，勿用于商业滥用。

---

## 功能特性

- **规则引擎**：每条规则 = 目标包名 + 目标类 + 目标方法 + 行为模式
- **四种行为模式**：
  - `block` — 拦截方法，阻止广告展示 / 判定已完成
  - `override_return` — 用指定值覆盖返回值（如把"是否展示广告"改为 false）
  - `invoke_after` — 方法执行后延迟调用目标方法（自动跳过 / 加速关闭广告页）
  - `accel_param` — 把方法第一个数值参数按倍数缩小（倒计时时长 / 播放进度 / seek 毫秒）
- **主动广告引擎（AdViewHunter v2）**：自动扫描目标 Activity 视图树，按文本 / contentDescription / 控件 id / 可点击 / 尺寸**多维打分**选中跳过/关闭按钮并自动点击；循环重试，能点到"倒计时结束后才出现"的按钮
- **广告 SDK 自动识别（AdSdkDetector）**：内置穿山甲/Pangle、优量汇/GDT、AdMob、Mintegral、ironSource、AppLovin、Unity Ads、百度百青藤、Vungle、MoPub、Inmobi、Smaato 等常见 SDK 签名类，自动判定目标应用命中哪些广告 SDK，便于编写规则
- **悬浮控制窗**：可拖动的悬浮球，展开面板可实时调节**加速倍数（1–20 倍）**和**功能开关**（跳过 / 拦截展示 / 加速 / 自动关闭 / 自动识别 SDK / 隐身）
- **深度隐身引擎（StealthEngine v2）**：见下节，六路防检测打点
- **检测实验室**：列出隐身覆盖项与模块无法隐藏的框架级指纹，供对照验证
- **跨进程数据下发**：hook 运行在目标应用进程，规则与设置存在本模块应用，通过 exported ContentProvider 读取
- **可视化配置 UI**：添加 / 删除规则、载入示例、显示激活状态、悬浮窗开关、检测实验室

---

## 目录结构

```
LSPAdSkip/
├── settings.gradle / build.gradle / gradle.properties
└── app/
    ├── build.gradle                 # 依赖 Xposed API (compileOnly)
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # 模块声明 + Provider
        ├── assets/xposed_init       # 入口类注册
        ├── java/com/example/lspadskip/
        │   ├── HookEntry.java       # LSPosed 入口
        │   ├── MainActivity.java    # 配置 UI
        │   ├── XposedChecker.java
        │   └── core/
        │       ├── HookEngine.java  # 规则引擎（核心）
        │       ├── HookRule.java    # 规则模型
        │       ├── RulesStore.java  # 规则持久化
        │       └── RulesProvider.java # 跨进程读取
        └── res/
```

---

## 构建（得到可直接安装的 APK）

**方式 A：GitHub Actions 自动构建（推荐，无需本机 SDK）**
1. 把本工程推到一个 GitHub 仓库（main 分支，或手动 Run workflow）；
2. 打开仓库 → **Actions** → 等构建完成；
3. 下载 **Artifacts → app-debug**，解压出的 `app-debug.apk` 已用 debug keystore 签名，可直接安装。
> 已内置 `.github/workflows/build.yml`（JDK 17 + Gradle 8.7 + AGP 8.5.2），推送即触发。

**方式 B：本地 Android Studio**
用 Android Studio 打开 `LSPAdSkip` 目录，Gradle 同步后 `Build > Make Project`。
产物：`app/build/outputs/apk/debug/app-debug.apk`

> 需要本机已安装 Android SDK（compileSdk 34）与 JDK 17。

---

## 使用步骤

1. 安装 APK 到已 root 且装有 **LSPosed** 的手机。
2. 打开 **LSPosed 管理器** → 模块 → 启用 `LSPAdSkip`。
3. 在模块详情页勾选**目标应用**（作用域）。
4. 打开 LSPAdSkip App，添加针对目标应用广告 SDK 的规则；点"开启悬浮窗控制"，首次需在系统设置中授予**悬浮窗权限**。
5. **重启目标应用**使其进程重新注入，规则即生效。
6. 悬浮球展开面板：拖动 **加速倍数** 滑杆调节倍数；用 4 个开关控制跳过 / 拦截 / 加速 / 自动关闭。
7. 查看 hook 日志：LSPosed 管理器 → 日志（`LSPAdSkip` 前缀）。

> 设置说明：悬浮窗的倍数与开关即时保存，hook 引擎在**目标应用进程启动时**读取，因此调整后**重启目标应用**生效。

---

## 如何编写广告规则（重点）

广告跳过本质是 **hook 目标应用的广告 SDK 类与方法**。不同 App 的 SDK 不同，需先逆向定位：

> 快捷方式：开启悬浮窗的"自动识别广告 SDK"，重启目标应用后，LSPosed 日志会打印命中的 SDK（如
> `LSPAdSkip detected ad SDK: [穿山甲/Pangle, AdMob]`），你就能直接针对该 SDK 编写规则，省去盲目搜索。

**常见切入点（方法判定思路）**
| 场景 | 定位 | 推荐模式 |
|---|---|---|
| 不展示广告 | 判断"是否展示广告"的方法 | `override_return` → false |
| 判定广告已播完 | 广告播放完成的回调方法 | `block`（阻断并返回 null） |
| 倒计时加速 | 广告倒计时/状态回调 | `invoke_after` 延迟后调关闭方法 |
| 自动跳过 | 跳过按钮点击回调 | `invoke_after` |

**定位方法（需在目标应用上操作）**
1. 用 `jadx` / `Jadx-GUI` 打开目标 APK，搜关键词：`广告`、`ad`、`AdManager`、`shouldShowAd`、`onAdFinished`、`countdown`、`skip`。
2. 用日志 hook（如本引擎 `XposedBridge.log`）打印调用栈，定位实际触发的方法。
3. 按定位到的真实类名 / 方法名填规则。

**在 LSPosed 的"日志"里查看本模块输出**，能确认 hook 是否命中。

---

## B 站小游戏激励广告适配（快速领奖励）

目标：加速 / 跳过 B 站小游戏里的**激励视频广告**（看完才能领奖励），尽快领到游戏奖励。

App 内主界面点「**载入 B 站小游戏广告示例**」会写入 4 条针对 `tv.danmaku.bili` 的规则模板。

**激励广告的通用流程与对应策略**
| 流程节点 | 定位方法（判定思路） | 推荐模式 |
|---|---|---|
| 是否可领奖励 | "奖励是否就绪"的判定方法 | `override_return` → true |
| 奖励发放回调 | `onRewardVerify` / `onReward` 等回调 | `block`（视为已发放） |
| 倒计时加速 | 剩余秒数 / 进度回调 | `accel_param`（按倍数缩小） |
| 自动领取/关闭 | 可关闭后的"领取"方法 | `invoke_after` 延迟自动调用 |

**重要**：B 站不同版本的广告 SDK（穿山甲 / 优量汇 / B 站自研等）类名不同，模板里的 `xxx.ad.RewardAdHelper` 等是**占位类**。要真正生效，请：
1. 开启悬浮窗「自动识别广告 SDK」，重启 B 站看日志命中哪家；
2. 用 Jadx 打开当前 B 站 APK，搜索 `Reward` / `reward` / `onReward` / 倒计时 / 关闭 等定位真实类与方法；
3. 把占位类名替换成真实类名即可。

---

## 规则字段说明

| 字段 | 说明 |
|---|---|
| `packageName` | 目标应用包名 |
| `className` | 目标类全限定名 |
| `methodName` | 目标方法名（hook 所有重载） |
| `mode` | `block` / `override_return` / `invoke_after` / `accel_param` |
| `targetMethod` | `invoke_after` 模式延迟后调用的方法名 |
| `delayMs` | `invoke_after` 延迟毫秒数（会按悬浮窗倍数缩短） |
| `returnValue` | `override_return` 模式的返回值 |
| `enabled` | 是否启用 |

## 悬浮窗设置

| 项 | 说明 |
|---|---|
| 加速倍数 | 1–20，作用于 `accel_param`（参数缩放）与 `invoke_after`（延迟缩短） |
| 跳过广告 | 总开关，关闭则模块对所有目标包不生效 |
| 拦截广告展示 | 控制 `block` / `override_return` 类规则 |
| 加速倒计时 / 进度 | 控制 `accel_param` 类规则与 `invoke_after` 延迟加速 |
| 自动关闭广告页 | 控制 `invoke_after` 类规则 |
| 隐身模式 | 见下方「隐身 / 防 Hook 检测测试模式」 |

## 隐身 / 防 Hook 检测测试模式（StealthEngine v2）

用途：**验证你的自有应用能否检测到自己被本模块 hook**（加固测试），不是用于恶意隐藏。

开启后（悬浮窗开关 → 重启目标应用生效），模块打点六路防检测：

- **静默日志**：关闭所有 `XposedBridge.log` 输出，消除 logcat 中 `LSPAdSkip` / Xposed 字样痕迹。
- **栈帧清洗**：hook `Thread.getStackTrace()` 与 `Throwable.getStackTrace()`，过滤 `de.robv.android.xposed` 及模块自身栈帧。
- **包管理器隐藏**：hook `getInstalledPackages` / `getInstalledApplications` / `getPackageInfo` / `getApplicationInfo`，移除本模块与 Xposed/LSPosed 安装器（`org.lsposed.manager`、`de.robv.android.xposed.installer`、`com.android.shell` 等）的包。
- **文件系统隐藏**：hook `File.exists` / `isFile` / `listFiles` / `list` 等，隐藏模块 APK 与安装器路径。
- **/proc/self/maps 过滤**：hook `BufferedReader.readLine`，将包含 `lspd` / `riru` / `zygisk` / `xposed` 的行替换为空行。
- **日志标签净化 + DEX 字符串混淆**：`Log.println` 替换可疑 tag；`Obf` 使 DEX 中不存在连续 `de.robv.android.xposed` 字面量。

### 主动广告引擎（AdViewHunter）

开启"自动关闭广告页"后生效：hook `Activity.onWindowFocusChanged`，在窗口获得焦点后延迟扫描视图树，找到文本含"跳/关闭/知道了/skip/close"的按钮并自动 `performClick()`。

**局限性（务必知晓）**：Xposed/LSPosed 是 Java 层框架，模块内只能清理模块可控制的部分。
以下指纹**本模块无法清除**，应用仍可能据此检测到被 hook：
- LSPosed / Xposed 安装器的包名与签名（即使被包管理隐藏，仍可能从应用详情页、备份、系统设置中发现）
- zygisk / riru 注入痕迹、so 库（模块 maps 过滤只覆盖 `BufferedReader` 读路径，直接 `File` 读取或 syscall 不受影响）
- Java 层 hook 的类加载器差异、方法计数变化、hook 方法的非原生实现痕迹
- 行为检测：规则一旦生效，广告行为会改变，可被观察发现
- `/data/adb` 下 lspd / riru / zygisk 相关文件本身仍存在

所以隐身模式应作为**测试你的检测是否足够健壮**的对照工具：开启后若你的应用仍能检出，说明检测项属于上述框架级指纹；若不再检出，则说明你的检测依赖日志/栈帧/包管理/文件/maps 等模块已覆盖的向量，可针对性加固。完整的覆盖项与指纹清单见 App 内"检测实验室"。

---

## 常见问题

- **规则没生效？** 确认模块已在 LSPosed 启用、作用域勾选了目标应用、且目标应用已重启；再到日志确认 hook 是否命中（可能类名/方法名不匹配）。
- **hook 报错？** 打开 LSPosed 日志看 `LSPAdSkip hook failed:` 后的异常，通常是类名/方法名错误或方法签名不符。
- **跨进程读不到规则？** 确认 APK 用的是本工程清单里的 `RulesProvider` authority，未被混淆或修改。
