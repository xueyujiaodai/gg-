package com.example.lspadskip;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.lspadskip.core.ModuleSettings;
import com.example.lspadskip.core.SettingsStore;

/**
 * 悬浮控制窗服务。
 *  - 显示一个可拖动的悬浮球；点按悬浮球展开控制面板，面板可收起回悬浮球。
 *  - 面板内含：加速倍数滑杆（1~20 倍）与功能开关（跳过/拦截/加速/自动关闭）。
 *  - 改动实时写入 SettingsStore；hook 引擎（目标应用进程）通过 RulesProvider 读取，
 *    从而在应用重启后按新设置生效。
 */
public class FloatWindowService extends Service {

    private WindowManager wm;
    private View ball;
    private View panel;
    private WindowManager.LayoutParams ballLp;
    private WindowManager.LayoutParams panelLp;
    private ModuleSettings settings;
    private boolean panelShown = false;

    private float downRawX, downRawY;
    private int startX, startY;
    private boolean moved;

    private TextView tvFactorValue;
    private SeekBar seekFactor;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        settings = SettingsStore.load(this);
        if (Build.VERSION.SDK_INT >= 26) {
            createChannel();
            startForeground(1, buildNotification());
        }
        showBall();
    }

    private void showBall() {
        TextView tv = new TextView(this);
        tv.setText("▶");
        tv.setTextSize(18);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.float_ball);
        ball = tv;

        int type = overlayType();
        ballLp = new WindowManager.LayoutParams(
                dp(56), dp(56), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        ballLp.gravity = Gravity.TOP | Gravity.START;
        ballLp.x = dp(20);
        ballLp.y = dp(200);

        ball.setOnTouchListener(this::onBallTouch);
        wm.addView(ball, ballLp);
    }

    private boolean onBallTouch(View v, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                startX = ballLp.x;
                startY = ballLp.y;
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = e.getRawX() - downRawX;
                float dy = e.getRawY() - downRawY;
                if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                    moved = true;
                }
                ballLp.x = startX + (int) dx;
                ballLp.y = startY + (int) dy;
                try {
                    wm.updateViewLayout(ball, ballLp);
                } catch (Exception ignored) {
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved) {
                    togglePanel();
                }
                return true;
        }
        return false;
    }

    private void togglePanel() {
        if (panelShown) {
            hidePanel();
        } else {
            showPanel();
        }
    }

    private void showPanel() {
        if (panel != null) {
            return;
        }
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        panel = inflater.inflate(R.layout.float_panel, null);

        int type = overlayType();
        panelLp = new WindowManager.LayoutParams(
                dp(320), WindowManager.LayoutParams.WRAP_CONTENT, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        panelLp.gravity = Gravity.CENTER;

        tvFactorValue = panel.findViewById(R.id.tv_factor_value);
        seekFactor = panel.findViewById(R.id.seek_factor);
        seekFactor.setMax(20); // progress 0..20 => 倍数 1..21 显示，这里映射 1..20
        seekFactor.setProgress(Math.min(19, Math.max(0, settings.accelFactor - 1)));
        updateFactorText();

        seekFactor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser) {
                    settings.accelFactor = progress + 1;
                    SettingsStore.save(FloatWindowService.this, settings);
                    updateFactorText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });

        bindSwitch(R.id.switch_skip, settings.enableSkip,
                (buttonView, isChecked) -> {
                    settings.enableSkip = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });
        bindSwitch(R.id.switch_block, settings.enableBlock,
                (buttonView, isChecked) -> {
                    settings.enableBlock = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });
        bindSwitch(R.id.switch_accel, settings.enableAccel,
                (buttonView, isChecked) -> {
                    settings.enableAccel = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });
        bindSwitch(R.id.switch_autoclose, settings.enableAutoClose,
                (buttonView, isChecked) -> {
                    settings.enableAutoClose = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });
        bindSwitch(R.id.switch_sdk, settings.autoDetectSdk,
                (buttonView, isChecked) -> {
                    settings.autoDetectSdk = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });
        bindSwitch(R.id.switch_stealth, settings.stealthEnabled,
                (buttonView, isChecked) -> {
                    settings.stealthEnabled = isChecked;
                    SettingsStore.save(FloatWindowService.this, settings);
                });

        Button btnCollapse = panel.findViewById(R.id.btn_collapse);
        btnCollapse.setOnClickListener(v -> hidePanel());

        wm.addView(panel, panelLp);
        panelShown = true;
    }

    private void hidePanel() {
        if (panel != null) {
            try {
                wm.removeView(panel);
            } catch (Exception ignored) {
            }
            panel = null;
            panelShown = false;
        }
    }

    private void bindSwitch(int id, boolean checked,
                            Switch.OnCheckedChangeListener listener) {
        Switch sw = panel.findViewById(id);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
    }

    private void updateFactorText() {
        if (tvFactorValue != null) {
            tvFactorValue.setText("x" + settings.accelFactor);
        }
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                "float_channel", "悬浮窗控制", NotificationManager.IMPORTANCE_LOW);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, "float_channel")
                .setContentTitle("LSPAdSkip 悬浮窗")
                .setContentText("广告加速规则控制运行中")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        hidePanel();
        if (ball != null) {
            try {
                wm.removeView(ball);
            } catch (Exception ignored) {
            }
            ball = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
