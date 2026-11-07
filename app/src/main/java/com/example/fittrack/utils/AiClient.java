package com.example.fittrack.utils;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI connection: tries Ollama on your Mac first (free, local), then the free
 * Google Gemini API if an API key was saved.
 */
public class AiClient {

    public interface Callback {
        void onReply(String text);
        void onError(String message);
    }

    // ---- local AI (Ollama) ----
    public static final boolean USE_LOCAL = true;
    // 10.0.2.2 is how the Android emulator reaches your Mac
    private static final String LOCAL_URL = "http://10.0.2.2:11434/api/chat";
    private static final String LOCAL_MODEL = "llama3.2";

    // ---- online AI (Gemini, free key) ----
    private static final String[] GEMINI_MODELS = {"gemini-flash-latest", "gemini-flash-lite-latest"};
    private static final String GEMINI_BASE =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor();

    private static class Resp {
        int code;
        String body;
    }

    /** history items are {role, text}; roles are "user" or "assistant". */
    public static void chat(String apiKey, String system, List<String[]> history, Callback cb) {
        POOL.execute(() -> {
            String result = null;
            String err = null;
            String localErr = null;

            if (USE_LOCAL) {
                try {
                    result = askLocal(system, history);
                } catch (Exception e) {
                    localErr = e.getMessage();
                }
            }

            if (result == null) {
                if (apiKey != null && !apiKey.isEmpty()) {
                    try {
                        result = askGemini(apiKey, system, history);
                    } catch (Exception e) {
                        err = (e.getMessage() != null) ? e.getMessage() : "Something went wrong.";
                    }
                } else {
                    err = ((localErr != null) ? localErr : "No AI is set up.")
                            + " You can also add a free Gemini key with the gear icon.";
                }
            }

            final String r = result;
            final String er = err;
            MAIN.post(() -> {
                if (r != null) cb.onReply(r); else cb.onError(er);
            });
        });
    }

    // ---------- Ollama ----------

    private static String askLocal(String system, List<String[]> history) throws Exception {
        JSONArray msgs = new JSONArray();
        msgs.put(new JSONObject().put("role", "system").put("content", system));
        for (String[] m : history) {
            msgs.put(new JSONObject().put("role", m[0]).put("content", m[1]));
        }
        String body = new JSONObject()
                .put("model", LOCAL_MODEL)
                .put("messages", msgs)
                .put("stream", false)
                .put("options", new JSONObject().put("num_predict", 400))
                .toString();

        Resp r;
        try {
            // first reply can take a while because the model has to load into memory
            r = post(LOCAL_URL, null, body, 3000, 180000);
        } catch (IOException e) {
            throw new IOException("Could not reach Ollama on your Mac. Is it running?");
        }
        if (r.code == 404) {
            throw new IOException("Ollama has no model called " + LOCAL_MODEL
                    + ". Run: ollama pull " + LOCAL_MODEL);
        }
        if (r.code < 200 || r.code >= 300) {
            throw new IOException("Local AI error " + r.code + ".");
        }
        String text = new JSONObject(r.body).getJSONObject("message").optString("content", "").trim();
        if (text.isEmpty()) throw new IOException("The local AI sent an empty reply.");
        return text;
    }

    // ---------- Gemini ----------

    private static String askGemini(String apiKey, String system, List<String[]> history)
            throws Exception {
        JSONArray contents = new JSONArray();
        for (String[] m : history) {
            String role = m[0].equals("assistant") ? "model" : "user";
            contents.put(new JSONObject()
                    .put("role", role)
                    .put("parts", new JSONArray().put(new JSONObject().put("text", m[1]))));
        }
        String body = new JSONObject()
                .put("system_instruction", new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", system))))
                .put("contents", contents)
                .put("generationConfig", new JSONObject().put("maxOutputTokens", 1024))
                .toString();

        Resp last = null;
        for (String model : GEMINI_MODELS) {
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    last = post(GEMINI_BASE + model + ":generateContent", apiKey, body, 15000, 45000);
                } catch (IOException e) {
                    throw new IOException("Could not reach Gemini. Check your internet connection.");
                }
                if (last.code == 503 && attempt < 2) {
                    Thread.sleep(1500L * (attempt + 1));   // busy: wait, then retry
                    continue;
                }
                break;
            }
            if (last.code == 503 || last.code == 429 || last.code == 404) continue;
            break;
        }

        if (last.code < 200 || last.code >= 300) {
            throw new IOException(geminiError(last.code, last.body));
        }
        JSONArray cands = new JSONObject(last.body).optJSONArray("candidates");
        StringBuilder sb = new StringBuilder();
        if (cands != null && cands.length() > 0) {
            JSONObject content = cands.getJSONObject(0).optJSONObject("content");
            JSONArray parts = (content != null) ? content.optJSONArray("parts") : null;
            if (parts != null) {
                for (int i = 0; i < parts.length(); i++) {
                    sb.append(parts.getJSONObject(i).optString("text"));
                }
            }
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) throw new IOException("The AI sent an empty reply. Try asking again.");
        return text;
    }

    private static String geminiError(int code, String resp) {
        String msg = "";
        try {
            msg = new JSONObject(resp).getJSONObject("error").optString("message", "");
        } catch (Exception ignored) { }

        if (msg.toLowerCase().contains("api key")) {
            return "Your API key was rejected. Tap the gear icon to enter it again.";
        }
        if (code == 503) return "The free AI is very busy right now. Try again in a minute.";
        if (code == 429) return "Free limit reached for now. Wait a minute and try again.";
        if (code == 404) return "No AI model was available. Check GEMINI_MODELS in AiClient.java.";
        return "Error " + code + (msg.isEmpty() ? "" : ": " + msg);
    }

    // ---------- shared HTTP ----------

    private static Resp post(String url, String googleKey, String body, int connectMs, int readMs)
            throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(connectMs);
            conn.setReadTimeout(readMs);
            conn.setRequestProperty("content-type", "application/json");
            if (googleKey != null) conn.setRequestProperty("x-goog-api-key", googleKey);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            Resp r = new Resp();
            r.code = conn.getResponseCode();
            InputStream is = (r.code >= 200 && r.code < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            r.body = read(is);
            return r;
        } finally {
            conn.disconnect();
        }
    }

    private static String read(InputStream is) throws IOException {
        if (is == null) return "";
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }
}
