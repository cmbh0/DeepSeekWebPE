package com.mycompany.application;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
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

public class MainActivity extends Activity {

    private static final String TARGET_URL = "http://127.0.0.1:3080";
    private static final int IPAD_VIEWPORT_WIDTH = 1366;
    private static final int IPAD_INITIAL_SCALE = 100;
    private static final int MENU_SIZE_DP = 46;
    private static final int MENU_MARGIN_DP = 16;
    private static final int PANEL_WIDTH_DP = 154;
    private static final int PANEL_HEIGHT_DP = 52;
    private static final int ACTION_BUTTON_SIZE_DP = 44;
    private static final int PANEL_GAP_DP = 7;
    private static final String IPAD_USER_AGENT =
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) "
                    + "AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    + "Version/17.0 Mobile/15E148 Safari/604.1";

    private FrameLayout root;
    private WebView webView;
    private ImageButton menuButton;
    private LinearLayout actionPanel;
    private ImageButton retryButton;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private boolean errorVisible;
    private boolean pageLoading;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable connectionTimeout = new Runnable() {
        @Override
        public void run() {
            if (pageLoading && !errorVisible) {
                showErrorPage();
            }
        }
    };
    private int touchSlop;
    private int menuLeft;
    private int menuTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestFullscreen();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(14, 20, 29));
        root.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setInitialScale(IPAD_INITIAL_SCALE);
        root.addView(webView);
        setContentView(root);

        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        configureWebView();
        createFloatingMenu();
        webView.loadUrl(TARGET_URL);
    }

    private void requestFullscreen() {
        Window window = getWindow();
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
        int legacyFlags = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        window.getDecorView().setSystemUiVisibility(legacyFlags);
        window.getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                            mainHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    requestFullscreen();
                                }
                            }, 250L);
                        }
                    }
                });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            requestFullscreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestFullscreen();
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUserAgentString(IPAD_USER_AGENT);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setBackgroundColor(Color.rgb(14, 20, 29));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (isTargetUrl(url)) {
                    pageLoading = true;
                    errorVisible = false;
                    injectTabletViewport(view);
                    mainHandler.removeCallbacks(connectionTimeout);
                    mainHandler.postDelayed(connectionTimeout, 9000L);
                    updateNavigationState();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isTargetUrl(url) && !errorVisible) {
                    pageLoading = false;
                    mainHandler.removeCallbacks(connectionTimeout);
                    injectTabletViewport(view);
                    updateNavigationState();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(
                    WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (!errorVisible && isLegacyMainFrameError(view, failingUrl)) {
                    showErrorPage();
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                updateNavigationState();
            }

            @Override
            public void onReceivedError(
                    WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && request.isForMainFrame()
                        && !errorVisible
                        && isTargetUrl(request.getUrl().toString())) {
                    showErrorPage();
                }
            }

            @Override
            public void onReceivedHttpError(
                    WebView view, WebResourceRequest request,
                    android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && request.isForMainFrame()
                        && !errorVisible
                        && isTargetUrl(request.getUrl().toString())
                        && errorResponse.getStatusCode() >= 400) {
                    showErrorPage();
                }
            }
        });
    }

    private boolean isTargetUrl(String url) {
        return url != null && (url.equals(TARGET_URL)
                || url.startsWith(TARGET_URL + "/")
                || url.startsWith(TARGET_URL + "?")
                || url.startsWith(TARGET_URL + "#"));
    }

    private boolean isLegacyMainFrameError(WebView view, String failingUrl) {
        return isTargetUrl(failingUrl) && failingUrl.equals(view.getUrl());
    }

    private void injectTabletViewport(WebView view) {
        String script = "(function(){"
                + "var m=document.querySelector('meta[name=viewport]');"
                + "if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}"
                + "m.content='width=" + IPAD_VIEWPORT_WIDTH
                        + ", initial-scale=1.0, maximum-scale=1.0, user-scalable=no';"
                + "})()";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            view.evaluateJavascript(script, null);
        } else {
            view.loadUrl("javascript:" + script);
        }
    }

    private void showErrorPage() {
        if (errorVisible) {
            return;
        }
        pageLoading = false;
        mainHandler.removeCallbacks(connectionTimeout);
        errorVisible = true;
        updateNavigationState();
        webView.stopLoading();
        webView.loadUrl("file:///android_asset/error.html");
    }

    private void retry() {
        errorVisible = false;
        pageLoading = true;
        webView.stopLoading();
        webView.loadUrl(TARGET_URL);
        hideActionPanel();
    }

    private void createFloatingMenu() {
        menuButton = new ImageButton(this);
        menuButton.setContentDescription("打开网页操作菜单");
        menuButton.setImageDrawable(new LiquidGlassDrawable.MenuIcon());
        menuButton.setBackgroundColor(Color.TRANSPARENT);
        menuButton.setPadding(0, 0, 0, 0);
        menuButton.setFocusable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            menuButton.setElevation(dp(6));
        }

        FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(
                dp(MENU_SIZE_DP), dp(MENU_SIZE_DP));
        menuParams.leftMargin = dp(MENU_MARGIN_DP);
        menuParams.topMargin = dp(MENU_MARGIN_DP);
        menuLeft = menuParams.leftMargin;
        menuTop = menuParams.topMargin;
        root.addView(menuButton, menuParams);

        actionPanel = new LinearLayout(this);
        actionPanel.setOrientation(LinearLayout.HORIZONTAL);
        actionPanel.setGravity(android.view.Gravity.CENTER);
        actionPanel.setPadding(dp(4), dp(4), dp(4), dp(4));
        actionPanel.setBackground(new LiquidGlassDrawable.Panel());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            actionPanel.setElevation(dp(6));
        }
        actionPanel.setVisibility(View.GONE);

        retryButton = createIconButton(android.R.drawable.ic_menu_rotate, "重试连接");
        backButton = createIconButton(android.R.drawable.ic_media_previous, "后退");
        forwardButton = createIconButton(android.R.drawable.ic_media_next, "前进");
        actionPanel.addView(retryButton, new LinearLayout.LayoutParams(
                dp(ACTION_BUTTON_SIZE_DP), dp(ACTION_BUTTON_SIZE_DP)));
        actionPanel.addView(backButton, new LinearLayout.LayoutParams(
                dp(ACTION_BUTTON_SIZE_DP), dp(ACTION_BUTTON_SIZE_DP)));
        actionPanel.addView(forwardButton, new LinearLayout.LayoutParams(
                dp(ACTION_BUTTON_SIZE_DP), dp(ACTION_BUTTON_SIZE_DP)));

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                dp(PANEL_WIDTH_DP), dp(PANEL_HEIGHT_DP));
        panelParams.leftMargin = menuLeft;
        panelParams.topMargin = menuTop + dp(MENU_SIZE_DP + PANEL_GAP_DP);
        root.addView(actionPanel, panelParams);

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleActionPanel();
            }
        });
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retry();
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
                hideActionPanel();
            }
        });
        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
                hideActionPanel();
            }
        });

        menuButton.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private int startLeft;
            private int startTop;
            private boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        startLeft = menuLeft;
                        startTop = menuTop;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int nextLeft = startLeft + Math.round(event.getRawX() - downX);
                        int nextTop = startTop + Math.round(event.getRawY() - downY);
                        if (!dragging && (Math.abs(nextLeft - startLeft) > touchSlop
                                || Math.abs(nextTop - startTop) > touchSlop)) {
                            dragging = true;
                        }
                        if (dragging) {
                            moveMenu(nextLeft, nextTop);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            v.performClick();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return false;
                }
            }
        });

        root.post(new Runnable() {
            @Override
            public void run() {
                updateNavigationState();
            }
        });
    }

    private ImageButton createIconButton(int icon, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setContentDescription(description);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        button.setColorFilter(Color.WHITE);
        return button;
    }

    private void toggleActionPanel() {
        if (actionPanel.getVisibility() == View.VISIBLE) {
            hideActionPanel();
        } else {
            actionPanel.setVisibility(View.VISIBLE);
            positionActionPanel();
            updateNavigationState();
        }
    }

    private void hideActionPanel() {
        actionPanel.setVisibility(View.GONE);
    }

    private void moveMenu(int left, int top) {
        int maxLeft = Math.max(dp(8), root.getWidth() - dp(MENU_SIZE_DP + 8));
        int maxTop = Math.max(dp(8), root.getHeight() - dp(MENU_SIZE_DP + 8));
        menuLeft = Math.max(dp(8), Math.min(left, maxLeft));
        menuTop = Math.max(dp(8), Math.min(top, maxTop));

        FrameLayout.LayoutParams menuParams = (FrameLayout.LayoutParams) menuButton.getLayoutParams();
        menuParams.leftMargin = menuLeft;
        menuParams.topMargin = menuTop;
        menuButton.setLayoutParams(menuParams);
        positionActionPanel();
    }

    private void positionActionPanel() {
        if (actionPanel == null || actionPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        int panelWidth = dp(PANEL_WIDTH_DP);
        int panelHeight = dp(PANEL_HEIGHT_DP);
        int left = menuLeft + dp(MENU_SIZE_DP) - panelWidth;
        int top = menuTop - panelHeight - dp(PANEL_GAP_DP);
        if (top < dp(8)) {
            top = menuTop + dp(MENU_SIZE_DP + PANEL_GAP_DP);
        }
        int maxLeft = Math.max(dp(8), root.getWidth() - panelWidth - dp(8));
        int maxTop = Math.max(dp(8), root.getHeight() - panelHeight - dp(8));
        left = Math.max(dp(8), Math.min(left, maxLeft));
        top = Math.max(dp(8), Math.min(top, maxTop));

        FrameLayout.LayoutParams panelParams = (FrameLayout.LayoutParams) actionPanel.getLayoutParams();
        panelParams.leftMargin = left;
        panelParams.topMargin = top;
        actionPanel.setLayoutParams(panelParams);
    }

    private void updateNavigationState() {
        if (backButton == null || forwardButton == null || webView == null) {
            return;
        }
        boolean canGoBack = !errorVisible && webView.canGoBack();
        boolean canGoForward = !errorVisible && webView.canGoForward();
        backButton.setEnabled(canGoBack);
        forwardButton.setEnabled(canGoForward);
        backButton.setAlpha(canGoBack ? 1.0f : 0.35f);
        forwardButton.setAlpha(canGoForward ? 1.0f : 0.35f);
    }

    private GradientDrawable createRoundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (!errorVisible && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(connectionTimeout);
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}