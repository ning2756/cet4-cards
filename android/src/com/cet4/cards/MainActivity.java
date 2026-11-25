package com.cet4.cards;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private WebView web;
    private LocalServer server;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        web.setBackgroundColor(0xFFFFFFFF);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setWebChromeClient(new android.webkit.WebChromeClient());

        setContentView(web, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        String url = null;
        try {
            server = new LocalServer(getAssets(), "index.html");
            server.start();
            url = "http://127.0.0.1:" + server.getPort() + "/index.html";
        } catch (Throwable t) {
            server = null;
        }
        if (url != null) {
            web.loadUrl(url);
        } else {
            // 兜底：万一端口都占用了，就直接读 APK 里的文件
            web.loadUrl("file:///android_asset/index.html");
        }
    }

    @Override
    public void onBackPressed() {
        long now = System.currentTimeMillis();
        if (now - lastBack < 2000L) {
            super.onBackPressed();
            return;
        }
        lastBack = now;
        Toast.makeText(this, "再按一次退出（进度已保存）", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) {
            web.onPause();
            web.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) {
            web.resumeTimers();
            web.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}