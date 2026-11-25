package com.cet4.cards;

import android.content.res.AssetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 极小的本机只读文件服务：把 APK 里打包的 assets 用 http://127.0.0.1:端口 发出来。
 * 这样做的好处是网页有了正常的 http 来源，浏览器才会把背诵进度稳定地存在手机上。
 * 只监听本机回环地址，外部设备连不上，也不联网。
 */
public class LocalServer implements Runnable {

    private final AssetManager assets;
    private final String entry;

    private ServerSocket socket;
    private Thread thread;
    private volatile boolean running;

    public LocalServer(AssetManager assets, String entry) {
        this.assets = assets;
        this.entry = entry;
    }

    public int getPort() {
        return socket == null ? -1 : socket.getLocalPort();
    }

    public void start() throws IOException {
        IOException last = null;
        for (int port = 48620; port <= 48640; port++) {
            try {
                socket = new ServerSocket(port, 8, InetAddress.getByName("127.0.0.1"));
                break;
            } catch (IOException e) {
                last = e;
            }
        }
        if (socket == null) {
            throw last != null ? last : new IOException("no free loopback port");
        }
        running = true;
        thread = new Thread(this, "cet4-assets");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        if (socket != null) {
            try { socket.close(); } catch (IOException ignored) { }
            socket = null;
        }
    }

    @Override
    public void run() {
        while (running) {
            Socket client = null;
            try {
                client = socket.accept();
                client.setSoTimeout(5000);
                handle(client);
            } catch (Throwable ignored) {
                // 单个请求出错不影响后续
            } finally {
                if (client != null) {
                    try { client.close(); } catch (IOException ignored) { }
                }
            }
        }
    }

    private void handle(Socket client) throws IOException {
        String path = readRequestPath(client.getInputStream());
        OutputStream out = client.getOutputStream();
        if (path == null || "/".equals(path)) {
            path = "/" + entry;
        }
        byte[] body = null;
        if (path.indexOf("..") < 0) {
            try {
                body = readAsset(path.startsWith("/") ? path.substring(1) : path);
            } catch (IOException e) {
                body = null;
            }
        }
        if (body == null) {
            write(out, "404 Not Found", "text/plain; charset=utf-8", "not found".getBytes("UTF-8"));
        } else {
            write(out, "200 OK", mimeType(path), body);
        }
        out.flush();
    }

    private byte[] readAsset(String name) throws IOException {
        InputStream is = assets.open(name);
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(64 * 1024);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        } finally {
            try { is.close(); } catch (IOException ignored) { }
        }
    }

    private String readRequestPath(InputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int prev3 = 0, prev2 = 0, prev1 = 0, c;
        while ((c = in.read()) != -1) {
            head.write(c);
            if (head.size() > 8192) return null;
            if (c == '\n' && prev1 == '\r' && prev2 == '\n' && prev3 == '\r') break;
            prev3 = prev2;
            prev2 = prev1;
            prev1 = c;
        }
        String text = head.toString("UTF-8");
        int eol = text.indexOf('\r');
        if (eol < 0) eol = text.indexOf('\n');
        if (eol < 0) return null;
        String[] parts = text.substring(0, eol).split(" ");
        if (parts.length < 2) return null;
        String target = parts[1];
        int q = target.indexOf('?');
        if (q >= 0) target = target.substring(0, q);
        return target;
    }

    private static String mimeType(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html") || p.endsWith(".htm")) return "text/html; charset=utf-8";
        if (p.endsWith(".json")) return "application/json; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }

    private void write(OutputStream out, String status, String mime, byte[] body) throws IOException {
        String header = "HTTP/1.1 " + status + "\r\n"
                + "Content-Type: " + mime + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        out.write(header.getBytes("UTF-8"));
        out.write(body);
    }
}