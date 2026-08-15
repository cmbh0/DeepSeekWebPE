package com.mycompany.application;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayLauncherActivity extends Activity {

    private static final int OVERLAY_REQUEST_CODE = 4501;
    private boolean permissionPageVisible;
    private boolean settingsOpened;
    private boolean overlayStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isOverlayAllowed()) {
            startOverlay();
        } else {
            showPermissionPage();
            openOverlaySettings();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (permissionPageVisible && isOverlayAllowed()) {
            startOverlay();
        } else if (permissionPageVisible) {
            settingsOpened = false;
        }
    }

    private boolean isOverlayAllowed() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void openOverlaySettings() {
        if (settingsOpened || Build.VERSION.SDK_INT < 23) {
            return;
        }
        settingsOpened = true;
        try {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST_CODE);
        } catch (Exception ignored) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }

    private void showPermissionPage() {
        permissionPageVisible = true;
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER);
        page.setPadding(dp(40), dp(32), dp(40), dp(32));
        page.setBackgroundColor(Color.rgb(14, 20, 29));

        TextView title = new TextView(this);
        title.setText("DeepSeekPE Web UI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        TextView message = new TextView(this);
        message.setText("需要开启“在其他应用上层显示”权限，才能创建可移动、可缩放的 Web 窗口。\n授权后将自动打开窗口。\n\n请在系统设置中允许本应用显示在其他应用上层。" );
        message.setTextColor(Color.rgb(177, 193, 211));
        message.setTextSize(15);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, dp(16), 0, dp(22));

        Button settingsButton = new Button(this);
        settingsButton.setText("打开悬浮窗权限设置");
        settingsButton.setTextColor(Color.WHITE);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        });

        page.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(message, new LinearLayout.LayoutParams(
                dp(560), LinearLayout.LayoutParams.WRAP_CONTENT));
        page.addView(settingsButton, new LinearLayout.LayoutParams(
                dp(250), dp(52)));
        setContentView(page);
    }

    private void startOverlay() {
        if (overlayStarted) {
            return;
        }
        overlayStarted = true;
        permissionPageVisible = false;
        Intent serviceIntent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}