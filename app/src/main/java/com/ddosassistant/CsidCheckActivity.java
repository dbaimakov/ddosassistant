package com.ddosassistant;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CsidCheckActivity extends ComponentActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText urlInput;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_csid_check);

        urlInput = findViewById(R.id.urlInput);
        output = findViewById(R.id.output);
        Button runBtn = findViewById(R.id.runBtn);

        urlInput.setText("https://iaac-aeic.gc.ca/050/evaluations/exploration");

        runBtn.setOnClickListener(v -> runCheck());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void runCheck() {
        final String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            output.setText("Please enter a URL.");
            return;
        }

        output.setText("Running check...\n");

        executor.execute(() -> {
            String result = probeUrl(url);
            runOnUiThread(() -> output.setText(result));
        });
    }

    private String probeUrl(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();

            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) CSID-Check/1.0");

            int status = conn.getResponseCode();
            String contentType = conn.getContentType();
            Map<String, List<String>> headers = conn.getHeaderFields();

            InputStream is = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String bodySample = readUpTo(is, 8192);

            Classification cls = classify(status, contentType, bodySample, headers);

            StringBuilder sb = new StringBuilder();
            sb.append("URL: ").append(urlString).append("\n");
            sb.append("HTTP: ").append(status).append("\n");
            sb.append("Content-Type: ").append(contentType).append("\n");
            sb.append("Classification: ").append(cls.name()).append("\n\n");
            sb.append("Set-Cookie names:\n");
            sb.append(extractCookieNames(headers)).append("\n");
            sb.append("\nBody sample (first ~8KB):\n");
            sb.append(bodySample);

            return sb.toString();
        } catch (Exception e) {
            return "Error:\n" + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private enum Classification {
        OK_OR_NORMAL,
        POSSIBLE_F5_ASM_BLOCK,
        POSSIBLE_JS_CHALLENGE,
        RATE_LIMITED_OR_THROTTLED,
        UNKNOWN
    }

    private Classification classify(int status, String contentType, String body,
                                    Map<String, List<String>> headers) {

        String b = body == null ? "" : body.toLowerCase();

        boolean looksBlocked =
                b.contains("the requested url was rejected") ||
                        b.contains("support id") ||
                        b.contains("request was blocked") ||
                        b.contains("access denied");

        boolean looksJsChallenge =
                b.contains("enable javascript") ||
                        b.contains("<noscript") ||
                        b.contains("client-side integrity") ||
                        b.contains("csid");

        if (status == 429) {
            return Classification.RATE_LIMITED_OR_THROTTLED;
        }
        if (status >= 500) {
            return Classification.UNKNOWN;
        }
        if (status >= 400 && looksBlocked) {
            return Classification.POSSIBLE_F5_ASM_BLOCK;
        }
        if (looksJsChallenge) {
            return Classification.POSSIBLE_JS_CHALLENGE;
        }
        if (status >= 200 && status < 300) {
            return Classification.OK_OR_NORMAL;
        }

        return Classification.UNKNOWN;
    }

    private String extractCookieNames(Map<String, List<String>> headers) {
        StringBuilder sb = new StringBuilder();
        if (headers == null) {
            return "(none)";
        }

        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies == null || setCookies.isEmpty()) {
            return "(none)";
        }

        for (String c : setCookies) {
            if (c == null) {
                continue;
            }
            int eq = c.indexOf('=');
            if (eq > 0) {
                sb.append("- ").append(c, 0, eq).append("\n");
            } else {
                sb.append("- ").append(c).append("\n");
            }
        }
        return sb.toString();
    }

    private String readUpTo(InputStream is, int maxBytes) throws Exception {
        if (is == null) {
            return "(no body)";
        }
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buf = new byte[1024];
        int total = 0;
        int r;
        while ((r = bis.read(buf)) != -1) {
            int toWrite = Math.min(r, maxBytes - total);
            bos.write(buf, 0, toWrite);
            total += toWrite;
            if (total >= maxBytes) {
                break;
            }
        }

        return bos.toString("UTF-8");
    }
}
