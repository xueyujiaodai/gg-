package com.example.lspadskip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lspadskip.core.HookRule;
import com.example.lspadskip.core.RulesStore;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView tvStatus;
    private TextView tvScopeHint;
    private ListView listRules;
    private Button btnFloat;
    private ArrayAdapter<String> adapter;
    private final List<HookRule> rules = new ArrayList<>();
    private boolean floatRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvScopeHint = findViewById(R.id.tv_scope_hint);
        listRules = findViewById(R.id.list_rules);
        btnFloat = findViewById(R.id.btn_float);
        Button btnLab = findViewById(R.id.btn_lab);
        Button btnAdd = findViewById(R.id.btn_add);
        Button btnExample = findViewById(R.id.btn_example);
        Button btnClear = findViewById(R.id.btn_clear);

        boolean active = isXposedActive();
        tvStatus.setText(active
                ? "状态：LSPosed 模块已激活 ✅\n请在 LSPosed 管理器中勾选目标应用作用域后重启该应用。"
                : "状态：未在 LSPosed 中激活 ⚠\n请先安装 APK，再到 LSPosed 管理器启用本模块并勾选作用域。");
        tvScopeHint.setText("提示：hook 的是目标应用的广告 SDK 类与方法，需按其真实签名编写规则。"
                + "\n通用模式：block=拦截；override_return=覆盖返回值；"
                + "invoke_after=延迟后调用目标方法；accel_param=数值参数按倍数缩小。");

        rules.clear();
        rules.addAll(RulesStore.loadRules(this));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setText(rules.get(position).toString());
                return tv;
            }
        };
        listRules.setAdapter(adapter);
        refreshList();

        btnFloat.setOnClickListener(v -> toggleFloat());
        btnLab.setOnClickListener(v ->
                startActivity(new Intent(this, DetectionLabActivity.class)));
        btnAdd.setOnClickListener(v -> showAddDialog());
        btnExample.setOnClickListener(v -> {
            loadExamples();
            Toast.makeText(this, "已载入示例规则（需按目标应用实际 SDK 调整类名/方法名）", Toast.LENGTH_LONG).show();
        });
        Button btnBili = findViewById(R.id.btn_bili);
        btnBili.setOnClickListener(v -> {
            loadBilibiliMiniGameExamples();
            Toast.makeText(this, "已载入 B 站小游戏广告示例（类名/方法名为模板，需按实际 B 站版本核对）", Toast.LENGTH_LONG).show();
        });
        btnClear.setOnClickListener(v -> {
            rules.clear();
            RulesStore.clearRules(this);
            refreshList();
        });

        listRules.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(position);
            return true;
        });
    }

    /** 开启/关闭悬浮窗（含悬浮窗权限申请）。API 23 以下悬浮窗权限安装时默认授予。 */
    private void toggleFloat() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            }
            Toast.makeText(this, "请在系统设置中允许本应用的悬浮窗权限后再开启", Toast.LENGTH_LONG).show();
            return;
        }
        if (floatRunning) {
            stopService(new Intent(this, FloatWindowService.class));
            floatRunning = false;
            btnFloat.setText("开启悬浮窗控制");
            Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show();
        } else {
            Intent i = new Intent(this, FloatWindowService.class);
            if (Build.VERSION.SDK_INT >= 26) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            floatRunning = true;
            btnFloat.setText("关闭悬浮窗控制");
            Toast.makeText(this, "悬浮窗已开启，可拖动悬浮球展开面板调节", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从悬浮窗授权页返回后更新按钮文案
        if (btnFloat != null) {
            boolean granted = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
            btnFloat.setText(granted ? "开启悬浮窗控制" : "开启悬浮窗控制（需授权）");
        }
    }

    /** 载入几条示例规则，方便理解格式（真实使用需替换为目标应用 SDK 的类与方法）。 */
    private void loadExamples() {
        HookRule r1 = new HookRule("com.example.targetapp",
                "com.example.targetapp.ad.AdManager", "shouldShowAd", "override_return");
        r1.returnValue = false;

        HookRule r2 = new HookRule("com.example.targetapp",
                "com.example.targetapp.ad.VideoAdPlayer", "onAdFinished", "block");

        HookRule r3 = new HookRule("com.example.targetapp",
                "com.example.targetapp.ad.VideoAdPlayer", "onCountdownTick", "invoke_after");
        r3.targetMethod = "onAdFinished";
        r3.delayMs = 500;

        // 演示 accel_param：把倒计时/进度数值参数按悬浮窗倍数缩小
        HookRule r4 = new HookRule("com.example.targetapp",
                "com.example.targetapp.ad.VideoAdPlayer", "setRemainingMillis", "accel_param");

        rules.clear();
        rules.add(r1);
        rules.add(r2);
        rules.add(r3);
        rules.add(r4);
        RulesStore.saveRules(this, rules);
        refreshList();
    }

    /**
     * B 站小游戏激励广告加速示例（目标包名 tv.danmaku.bili）。
     *
     * 说明：以下类名/方法名为"激励视频广告（看完领奖励）"流程的通用模板，
     * 需按实际 B 站版本中广告 SDK 的真实类与方法核对后使用（可用 Jadx 反查，
     * 或开启"自动识别广告 SDK"看日志命中的是穿山甲/优量汇等哪家）。
     * 常见流程：广告加载 -> 倒计时 -> 可关闭/领取 -> 奖励回调 onReward/onRewardVerify。
     */
    private void loadBilibiliMiniGameExamples() {
        final String pkg = "tv.danmaku.bili";

        // 1) 把"是否已看完广告 / 奖励是否可发放"判定直接改为 true
        HookRule r1 = new HookRule(pkg,
                "tv.danmaku.bili.xxx.ad.RewardAdHelper", "isRewardReady", "override_return");
        r1.returnValue = true;

        // 2) 命中"奖励发放回调"时直接阻断（已视为发放，不二次执行）
        HookRule r2 = new HookRule(pkg,
                "tv.danmaku.bili.xxx.ad.RewardAdHelper", "onRewardVerify", "block");

        // 3) 广告倒计时回调：把剩余时长数值参数按悬浮窗倍数缩小（加速倒计时）
        HookRule r3 = new HookRule(pkg,
                "tv.danmaku.bili.xxx.ad.RewardAdView", "setRemainingSeconds", "accel_param");

        // 4) 广告可关闭后延迟自动调用"领取/关闭"方法
        HookRule r4 = new HookRule(pkg,
                "tv.danmaku.bili.xxx.ad.RewardAdView", "onCanClose", "invoke_after");
        r4.targetMethod = "claimReward";
        r4.delayMs = 200;

        rules.clear();
        rules.add(r1);
        rules.add(r2);
        rules.add(r3);
        rules.add(r4);
        RulesStore.saveRules(this, rules);
        refreshList();
    }

    private void refreshList() {
        if (adapter == null) {
            return;
        }
        List<String> texts = new ArrayList<>();
        for (HookRule r : rules) {
            texts.add(r.toString());
        }
        adapter.clear();
        adapter.addAll(texts);
        adapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);

        EditText etPkg = field("目标应用包名，如 com.example.app");
        EditText etCls = field("目标类全限定名，如 com.xxx.ad.AdManager");
        EditText etMtd = field("目标方法名，如 shouldShowAd");
        String[] modes = {"block", "override_return", "invoke_after", "accel_param"};
        final String[] chosen = {"block"};

        layout.addView(etPkg, lp(pad));
        layout.addView(etCls, lp(pad));
        layout.addView(etMtd, lp(pad));

        new AlertDialog.Builder(this)
                .setTitle("添加规则")
                .setView(layout)
                .setSingleChoiceItems(modes, 0, (d, w) -> chosen[0] = modes[w])
                .setPositiveButton("确定", (d, w) -> {
                    String pkg = etPkg.getText().toString().trim();
                    String cls = etCls.getText().toString().trim();
                    String mtd = etMtd.getText().toString().trim();
                    if (pkg.isEmpty() || cls.isEmpty() || mtd.isEmpty()) {
                        Toast.makeText(this, "包名/类名/方法名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    HookRule r = new HookRule(pkg, cls, mtd, chosen[0]);
                    rules.add(r);
                    RulesStore.saveRules(this, rules);
                    refreshList();
                    Toast.makeText(this, "已添加，重启目标应用后生效", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(int position) {
        if (position < 0 || position >= rules.size()) {
            return;
        }
        HookRule target = rules.get(position);
        new AlertDialog.Builder(this)
                .setTitle("删除规则")
                .setMessage("确定删除该规则？\n" + target)
                .setPositiveButton("删除", (d, w) -> {
                    rules.remove(position);
                    RulesStore.saveRules(this, rules);
                    refreshList();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean isXposedActive() {
        try {
            return XposedChecker.isActive();
        } catch (Throwable t) {
            return false;
        }
    }

    private EditText field(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        return et;
    }

    private LinearLayout.LayoutParams lp(int pad) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(pad, 0, pad, 0);
        return lp;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
