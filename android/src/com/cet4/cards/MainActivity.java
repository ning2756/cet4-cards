package com.cet4.cards;

import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private WebView web;
    private LocalServer server;
    private long lastBack = 0;
    private TextToSpeech tts;
    private volatile String pending = null;

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
        try { s.setMediaPlaybackRequiresUserGesture(false); } catch (Throwable ignored) {}
        web.setWebChromeClient(new android.webkit.WebChromeClient());

        initTts();
        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");

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

    /* 用手机自带的朗读引擎念英文：离线、音质最好、不费流量 */
    private void initTts() {
        try {
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.SUCCESS) {
                        tts = null;
                        return;
                    }
                    try { tts.setLanguage(Locale.US); } catch (Throwable ignored) {}
                    try { tts.setSpeechRate(0.9f); } catch (Throwable ignored) {}
                    String p = pending;
                    pending = null;
                    if (p != null) speakNow(p);
                }
            });
        } catch (Throwable t) {
            tts = null;
        }
    }

    private void speakNow(String text) {
        if (tts == null) return;
        try {
            tts.stop();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cet4");
        } catch (Throwable ignored) {}
    }

    /* 给网页用的小喇叭：window.AndroidTTS.speak("cut back on") */
    public class TtsBridge {
        @JavascriptInterface
        public boolean ready() {
            return tts != null;
        }

        @JavascriptInterface
        public void speak(String text) {
            if (text == null || text.length() == 0) return;
            if (tts == null) {
                pending = text;
                return;
            }
            speakNow(text);
        }

        @JavascriptInterface
        public void setRate(float rate) {
            try {
                if (tts != null && rate > 0.2f && rate < 3f) tts.setSpeechRate(rate);
            } catch (Throwable ignored) {}
        }

        @JavascriptInterface
        public void stop() {
            try { if (tts != null) tts.stop(); } catch (Throwable ignored) {}
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
        try { if (tts != null) tts.stop(); } catch (Throwable ignored) {}
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
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Throwable ignored) {}
            tts = null;
        }
        if (web != null) {
            web.loadUrl("about:blank");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}