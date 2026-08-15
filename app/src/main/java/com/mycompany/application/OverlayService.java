package com.mycompany.application;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OverlayService extends Service {

    private static final String TARGET_URL = "http://127.0.0.1:3080";
    private static final int NOTIFICATION_ID = 3080;
    private static final String NOTIFICATION_CHANNEL_ID = "deepseekpe_overlay";
    private static final int IPAD_VIEWPORT_WIDTH = 1366;
    private static final float WINDOW_WIDTH_RATIO = 0.89f;
    private static final float WINDOW_HEIGHT_RATIO = 0.37f;
    private static final float WINDOW_TOP_RATIO = 0.195f;
    private static final float WINDOW_MIN_WIDTH_RATIO = 0.56f;
    private static final float WINDOW_MIN_HEIGHT_RATIO = 0.22f;
    private static final int TITLE_BAR_UI = 28;
    private static final int TITLE_BUTTON_UI = 29;
    private static final int HANDLE_SIZE_UI = 21;
    private static final float USER_SCALE_MIN = 0.75f;
    private static final float USER_SCALE_MAX = 1.50f;
    private static final float USER_SCALE_STEP = 0.10f;
    private static final String IPAD_USER_AGENT =
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.0 Mobile/15E148 Safari/604.1";

    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;
    private FrameLayout windowRoot;
    private FrameLayout contentRoot;
    private WebView webView;
    private TextView titleView;
    private ImageButton refreshButton;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton zoomOutButton;
    private ImageButton zoomInButton;
    private ImageButton minimizeButton;
    private ImageButton maximizeButton;
    private ImageButton closeButton;
    private View resizeHandle;
    private boolean errorVisible;
    private boolean pageLoading;
    private float userScale = 1.0f;
    private int lastViewportScale = -1;
    private int lastViewportWidth = -1;
    private int lastViewportHeight = -1;
    private boolean viewportSyncPosted;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (pageLoading && !errorVisible) {
                showErrorPage();
            }
        }
    };
    private final Runnable viewportSync = new Runnable() {
        @Override
        public void run() {
            viewportSyncPosted = false;
            syncWebViewport();
        }
    };
    private boolean maximized;
    private boolean minimized;
    private boolean windowAdded;
    private int savedX;
    private int savedY;
    private int savedWidth;
    private int savedHeight;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        startOverlayNotification();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createWindow();
    }

    private void startOverlayNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Web UI 悬浮窗口",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("保持 DeepSeekPE Web UI 悬浮窗口运行");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        Intent launchIntent = new Intent(this, OverlayLauncherActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, launchIntent, pendingFlags);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setContentTitle("DeepSeekPE Web UI")
                .setContentText("悬浮窗口正在运行")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setShowWhen(false);
        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void createWindow() {
        windowRoot = new FrameLayout(this);
        windowRoot.setBackgroundColor(Color.rgb(17, 27, 40));

        buildTitleBar();

        contentRoot = new FrameLayout(this);
        contentRoot.setBackgroundColor(Color.rgb(14, 20, 29));
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        contentParams.topMargin = ui(TITLE_BAR_UI);
        windowRoot.addView(contentRoot, contentParams);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(14, 20, 29));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setInitialScale(1);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, MotionEvent event) {
                if (event.getPointerCount() > 1) {
                    return true;
                }
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        webView.scrollTo(0, webView.getScrollY());
                    }
                });
                return false;
            }
        });
        webView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (right - left != oldRight - oldLeft) {
                    scheduleViewportSync();
                }
            }
        });
        contentRoot.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        configureWebView();

        resizeHandle = createResizeHandle();
        FrameLayout.LayoutParams handleParams = new FrameLayout.LayoutParams(
                ui(HANDLE_SIZE_UI), ui(HANDLE_SIZE_UI), Gravity.RIGHT | Gravity.BOTTOM);
        windowRoot.addView(resizeHandle, handleParams);

        int width = getDefaultWindowWidth();
        int height = getDefaultWindowHeight();
        windowParams = createWindowParams(width, height);
        positionDefaultWindow();

        try {
            windowManager.addView(windowRoot, windowParams);
            windowAdded = true;
            scheduleViewportSync();
        } catch (Exception error) {
            stopSelf();
            return;
        }
        webView.loadUrl(TARGET_URL);
    }

    private void buildTitleBar() {
        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(ui(7), 0, ui(2), 0);
        titleBar.setBackground(createTitleBarBackground());
        titleBar.setContentDescription("拖动窗口标题栏");

        ImageButton appMark = new ImageButton(this);
        appMark.setImageDrawable(new WindowIconDrawable(
                WindowIconDrawable.APP_MARK, Color.rgb(170, 218, 255)));
        appMark.setBackgroundColor(Color.TRANSPARENT);
        appMark.setPadding(ui(4), ui(4), ui(4), ui(4));
        LinearLayout.LayoutParams appMarkParams =
                new LinearLayout.LayoutParams(ui(22), ui(27));

        titleView = new TextView(this);
        titleView.setText("DeepSeek Harness");
        titleView.setTextColor(Color.rgb(232, 241, 250));
        titleView.setTextSize(10);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        titleParams.leftMargin = ui(5);
        titleView.setLayoutParams(titleParams);

        refreshButton = createTitleButton(WindowIconDrawable.REFRESH, "刷新");
        backButton = createTitleButton(WindowIconDrawable.BACK, "上一步");
        forwardButton = createTitleButton(WindowIconDrawable.FORWARD, "下一步");
        zoomOutButton = createTitleButton(WindowIconDrawable.ZOOM_OUT, "缩小网页显示比例");
        zoomInButton = createTitleButton(WindowIconDrawable.ZOOM_IN, "放大网页显示比例");
        minimizeButton = createTitleButton(WindowIconDrawable.MINIMIZE, "最小化");
        maximizeButton = createTitleButton(WindowIconDrawable.MAXIMIZE, "全屏或还原");
        closeButton = createTitleButton(WindowIconDrawable.CLOSE, "关闭");

        titleBar.addView(closeButton);
        titleBar.addView(minimizeButton);
        titleBar.addView(appMark, appMarkParams);
        titleBar.addView(titleView, titleParams);
        titleBar.addView(backButton);
        titleBar.addView(refreshButton);
        titleBar.addView(zoomOutButton);
        titleBar.addView(zoomInButton);
        titleBar.addView(forwardButton);
        titleBar.addView(maximizeButton);
        updateScaleTitle();
        updateScaleButtonState();

        FrameLayout.LayoutParams titleParamsRoot = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, ui(TITLE_BAR_UI));
        windowRoot.addView(titleBar, titleParamsRoot);

        View.OnTouchListener titleDragListener = new WindowDragListener();
        titleBar.setOnTouchListener(titleDragListener);
        appMark.setOnTouchListener(titleDragListener);
        titleView.setOnTouchListener(titleDragListener);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (errorVisible) {
                    errorVisible = false;
                    webView.loadUrl(TARGET_URL);
                } else {
                    webView.reload();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeUserScale(-USER_SCALE_STEP);
            }
        });
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeUserScale(USER_SCALE_STEP);
            }
        });
        minimizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                minimized = !minimized;
                if (minimized) {
                    savedWidth = windowParams.width;
                    savedHeight = windowParams.height;
                }
                contentRoot.setVisibility(minimized ? View.GONE : View.VISIBLE);
                resizeHandle.setVisibility(minimized ? View.GONE : View.VISIBLE);
                updateWindowSize(savedWidth,
                        minimized ? ui(TITLE_BAR_UI) : savedHeight);
                if (!minimized) {
                    scheduleViewportSync();
                }
            }
        });
        maximizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (minimized) {
                    minimized = false;
                    contentRoot.setVisibility(View.VISIBLE);
                    resizeHandle.setVisibility(View.VISIBLE);
                }
                toggleMaximize();
                scheduleViewportSync();
            }
        });
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });
    }

    private ImageButton createTitleButton(int iconType, String description) {
        ImageButton button = new ImageButton(this);
        button.setContentDescription(description);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(0, 0, 0, 0);
        button.setImageDrawable(new WindowIconDrawable(
                iconType, Color.rgb(218, 235, 249)));
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ui(TITLE_BUTTON_UI), ui(TITLE_BAR_UI)));
        return button;
    }

    private View createResizeHandle() {
        ImageButton handle = new ImageButton(this);
        handle.setImageDrawable(new WindowIconDrawable(
                WindowIconDrawable.RESIZE, Color.rgb(147, 207, 243)));
        handle.setBackgroundColor(Color.TRANSPARENT);
        handle.setPadding(0, 0, 0, 0);
        handle.setContentDescription("拖动调整窗口大小");
        handle.setOnTouchListener(new ResizeListener());
        return handle;
    }

    private WindowManager.LayoutParams createWindowParams(int width, int height) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width,
                height,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = ui(8);
        params.y = ui(12);
        return params;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(IPAD_USER_AGENT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (isTargetUrl(url)) {
                    pageLoading = true;
                    errorVisible = false;
                    lastViewportWidth = -1;
                    lastViewportHeight = -1;
                    lastViewportScale = -1;
                    mainHandler.removeCallbacks(connectionTimeout);
                    mainHandler.postDelayed(connectionTimeout, 9000L);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isTargetUrl(url) && !errorVisible) {
                    pageLoading = false;
                    mainHandler.removeCallbacks(connectionTimeout);
                    scheduleViewportSync();
                    updateButtons();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (!errorVisible && isTargetUrl(failingUrl)) {
                    showErrorPage();
                }
            }

            @Override
            public void onReceivedError(
                    WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= 23
                        && request.isForMainFrame()
                        && !errorVisible
                        && isTargetUrl(request.getUrl().toString())) {
                    showErrorPage();
                }
            }
        });
    }

    private void showErrorPage() {
        pageLoading = false;
        mainHandler.removeCallbacks(connectionTimeout);
        errorVisible = true;
        webView.stopLoading();
        webView.loadUrl("file:///android_asset/error.html");
        updateButtons();
    }

    private void scheduleViewportSync() {
        if (viewportSyncPosted) {
            return;
        }
        viewportSyncPosted = true;
        mainHandler.postDelayed(viewportSync, 16L);
    }

    private void syncWebViewport() {
        if (webView == null || minimized || errorVisible) {
            return;
        }
        int contentWidth = webView.getWidth();
        int contentHeight = webView.getHeight();
        if (contentWidth <= 0 && windowParams != null) {
            contentWidth = windowParams.width;
        }
        if (contentHeight <= 0 && windowParams != null) {
            contentHeight = Math.max(1, windowParams.height - ui(TITLE_BAR_UI));
        }
        if (contentWidth <= 0 || contentHeight <= 0
                || (contentWidth == lastViewportWidth
                && contentHeight == lastViewportHeight)) {
            return;
        }
        lastViewportWidth = contentWidth;
        lastViewportHeight = contentHeight;
        float density = Math.max(1.0f, getResources().getDisplayMetrics().density);
        float contentCssWidth = contentWidth / density;
        int layoutWidth = Math.max(640,
                Math.round(IPAD_VIEWPORT_WIDTH / userScale));
        float scale = contentCssWidth / layoutWidth;
        scale = Math.max(0.05f, Math.min(scale, 2.0f));
        int scalePercent = Math.max(5, Math.round(scale * 100.0f));
        if (scalePercent != lastViewportScale) {
            lastViewportScale = scalePercent;
            webView.setInitialScale(scalePercent);
        }
        String scaleValue = String.format(java.util.Locale.US, "%.4f", scale);
        String viewportContent = "width=" + layoutWidth
                + ", initial-scale=" + scaleValue
                + ", minimum-scale=" + scaleValue
                + ", maximum-scale=" + scaleValue
                + ", user-scalable=no";
        String script = "(function(){"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}"
                + "window.__overlayViewportContent='" + viewportContent + "';"
                + "m.content=window.__overlayViewportContent;"
                + "document.documentElement.style.minWidth='0';"
                + "document.documentElement.style.maxWidth='" + layoutWidth + "px';"
                + "document.documentElement.style.overflowX='hidden';"
                + "document.documentElement.style.touchAction='pan-y';"
                + "document.body.style.overflowX='hidden';"
                + "document.body.style.touchAction='pan-y';"
                + "if(!window.__overlayViewportObserver){"
                + "window.__overlayViewportObserver=new MutationObserver(function(){"
                + "if(m.content!==window.__overlayViewportContent){m.content=window.__overlayViewportContent;}"
                + "});"
                + "window.__overlayViewportObserver.observe(m,{attributes:true,attributeFilter:['content']});"
                + "document.addEventListener('touchmove',function(e){"
                + "if(e.touches&&e.touches.length>1){e.preventDefault();}"
                + "},{passive:false});"
                + "document.addEventListener('gesturestart',function(e){e.preventDefault();});"
                + "document.addEventListener('gesturechange',function(e){e.preventDefault();});"
                + "document.addEventListener('wheel',function(e){if(e.ctrlKey){e.preventDefault();}},{passive:false});"
                + "}"
                + "window.dispatchEvent(new Event('resize'));"
                + "})()";
        webView.scrollTo(0, webView.getScrollY());
        if (Build.VERSION.SDK_INT >= 19) {
            webView.evaluateJavascript(script, null);
        } else {
            webView.loadUrl("javascript:" + script);
        }
    }

    private boolean isTargetUrl(String url) {
        return url != null && (url.equals(TARGET_URL)
                || url.startsWith(TARGET_URL + "/")
                || url.startsWith(TARGET_URL + "?")
                || url.startsWith(TARGET_URL + "#"));
    }

    private void changeUserScale(float delta) {
        userScale = Math.max(USER_SCALE_MIN,
                Math.min(USER_SCALE_MAX, userScale + delta));
        updateScaleTitle();
        lastViewportWidth = -1;
        lastViewportHeight = -1;
        lastViewportScale = -1;
        updateScaleButtonState();
        scheduleViewportSync();
    }

    private void updateScaleTitle() {
        if (titleView == null) {
            return;
        }
        int percent = Math.round(userScale * 100.0f);
        titleView.setText("DeepSeek · " + percent + "%");
        zoomOutButton.setContentDescription(
                "缩小网页显示比例，当前 " + percent + "%");
        zoomInButton.setContentDescription(
                "放大网页显示比例，当前 " + percent + "%");
    }

    private void updateScaleButtonState() {
        if (zoomOutButton == null || zoomInButton == null) {
            return;
        }
        boolean canZoomOut = userScale > USER_SCALE_MIN + 0.001f;
        boolean canZoomIn = userScale < USER_SCALE_MAX - 0.001f;
        zoomOutButton.setEnabled(canZoomOut);
        zoomInButton.setEnabled(canZoomIn);
        zoomOutButton.setAlpha(canZoomOut ? 1.0f : 0.35f);
        zoomInButton.setAlpha(canZoomIn ? 1.0f : 0.35f);
    }

    private void updateButtons() {
        if (backButton == null || forwardButton == null) {
            return;
        }
        backButton.setEnabled(!errorVisible && webView.canGoBack());
        forwardButton.setEnabled(!errorVisible && webView.canGoForward());
        backButton.setAlpha(backButton.isEnabled() ? 1.0f : 0.35f);
        forwardButton.setAlpha(forwardButton.isEnabled() ? 1.0f : 0.35f);
        updateScaleButtonState();
    }

    private void toggleMaximize() {
        if (!maximized) {
            savedX = windowParams.x;
            savedY = windowParams.y;
            savedWidth = windowParams.width;
            savedHeight = windowParams.height;
            windowParams.x = 0;
            windowParams.y = 0;
            windowParams.width = getDisplayWidth();
            windowParams.height = getDisplayHeight();
            maximized = true;
            resizeHandle.setVisibility(View.GONE);
        } else {
            windowParams.x = savedX;
            windowParams.y = savedY;
            windowParams.width = savedWidth;
            windowParams.height = savedHeight;
            maximized = false;
            resizeHandle.setVisibility(minimized ? View.GONE : View.VISIBLE);
        }
        applyWindowParams();
    }

    private void updateWindowSize(int width, int height) {
        windowParams.width = width;
        windowParams.height = height;
        if (!minimized && !maximized) {
            savedWidth = width;
            savedHeight = height;
        }
        applyWindowParams();
    }

    private void applyWindowParams() {
        if (windowAdded) {
            try {
                windowManager.updateViewLayout(windowRoot, windowParams);
                scheduleViewportSync();
            } catch (Exception ignored) {
            }
        }
    }

    private int getDefaultWindowWidth() {
        int minWidth = Math.round(getDisplayWidth() * WINDOW_MIN_WIDTH_RATIO);
        return clamp(
                Math.round(getDisplayWidth() * WINDOW_WIDTH_RATIO),
                minWidth,
                getDisplayWidth() - ui(12));
    }

    private int getDefaultWindowHeight() {
        int minHeight = Math.round(getDisplayHeight() * WINDOW_MIN_HEIGHT_RATIO);
        return clamp(
                Math.round(getDisplayHeight() * WINDOW_HEIGHT_RATIO),
                minHeight,
                getDisplayHeight() - ui(24));
    }

    private void positionDefaultWindow() {
        windowParams.width = getDefaultWindowWidth();
        windowParams.height = getDefaultWindowHeight();
        windowParams.x = Math.max(ui(6),
                (getDisplayWidth() - windowParams.width) / 2);
        windowParams.y = clamp(
                Math.round(getDisplayHeight() * WINDOW_TOP_RATIO),
                ui(10),
                Math.max(ui(10), getDisplayHeight() - windowParams.height - ui(10)));
        savedX = windowParams.x;
        savedY = windowParams.y;
        savedWidth = windowParams.width;
        savedHeight = windowParams.height;
    }

    private int getDisplayWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private int getDisplayHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private GradientDrawable createTitleBarBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(74, 145, 216), Color.rgb(53, 111, 184)});
        float radius = ui(4);
        drawable.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
        return drawable;
    }

    private int ui(int value) {
        float density = getResources().getDisplayMetrics().density;
        float compactScale = Math.max(1.0f, Math.min(density, 1.35f));
        return Math.round(value * compactScale);
    }

    private final class WindowDragListener implements View.OnTouchListener {
        private float downX;
        private float downY;
        private int startX;
        private int startY;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startX = windowParams.x;
                    startY = windowParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!maximized) {
                        int maxX = Math.max(0, getDisplayWidth() - windowParams.width);
                        int maxY = Math.max(0, getDisplayHeight() - windowParams.height);
                        windowParams.x = clamp(
                                startX + Math.round(event.getRawX() - downX), 0, maxX);
                        windowParams.y = clamp(
                                startY + Math.round(event.getRawY() - downY), 0, maxY);
                        applyWindowParams();
                    }
                    return true;
                default:
                    return true;
            }
        }
    }

    private final class ResizeListener implements View.OnTouchListener {
        private float downX;
        private float downY;
        private int startWidth;
        private int startHeight;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    startWidth = windowParams.width;
                    startHeight = windowParams.height;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!maximized && !minimized) {
                        int minWidth = Math.round(
                                getDisplayWidth() * WINDOW_MIN_WIDTH_RATIO);
                        int minHeight = Math.round(
                                getDisplayHeight() * WINDOW_MIN_HEIGHT_RATIO);
                        int width = Math.max(minWidth,
                                startWidth + Math.round(event.getRawX() - downX));
                        int height = Math.max(minHeight,
                                startHeight + Math.round(event.getRawY() - downY));
                        width = Math.min(width, Math.max(minWidth,
                                getDisplayWidth() - windowParams.x));
                        height = Math.min(height, Math.max(minHeight,
                                getDisplayHeight() - windowParams.y));
                        updateWindowSize(width, height);
                    }
                    return true;
                default:
                    return true;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowParams != null && !maximized && !minimized) {
            positionDefaultWindow();
            applyWindowParams();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mainHandler.removeCallbacks(connectionTimeout);
        mainHandler.removeCallbacks(viewportSync);
        viewportSyncPosted = false;
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        if (windowAdded && windowManager != null && windowRoot != null) {
            try {
                windowManager.removeView(windowRoot);
            } catch (Exception ignored) {
            }
        }
        windowAdded = false;
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(true);
        } else {
            stopForeground(false);
        }
        super.onDestroy();
    }
}