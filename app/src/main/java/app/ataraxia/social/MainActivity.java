package app.ataraxia.social;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.window.OnBackInvokedDispatcher;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Still. Instagram's own site, in this app, with a local limit. No credential form. */
public class MainActivity extends Activity {
    private static final String BASE = "https://www.instagram.com";
    private static final int PAPER = 0xFFF3EEE4, CARD = 0xFFFCFAF6, INK = 0xFF171512;
    private static final int MUTED = 0xFF6F6A62, SAGE = 0xFF3E5A46, HAIR = 0xFFE2DAD0;
    private SharedPreferences prefs;
    private WebView web;
    private LinearLayout root, panel, bar, nav;
    private View ruleTop, ruleBottom;
    private TextView status;
    private FrameLayout content;
    private View overlay;
    private Typeface display, body, medium;
    private String place = "";
    private String pageUrl = "";
    private LinearLayout roomsBar;
    private final Set<String> openRooms = new HashSet<>();
    private JSONObject people = new JSONObject();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active, browsing;
    private long lastTick;
    private String script;
    private ValueCallback<Uri[]> fileCallback;
    private int generation;
    private final Runnable ticker = new Runnable() {
        public void run() {
            if (!active) return;
            rollDay();
            long now = SystemClock.elapsedRealtime();
            long delta = Math.min(5000, Math.max(0, now - lastTick));
            lastTick = now;
            String url = web == null ? null : web.getUrl();
            if (browsing && Policy.internal(url)) {
                if (Policy.blocked(url)) { rejectRoute(); }
                else if (!Policy.exempt(url)) {
                    prefs.edit().putLong("dailyMs", prefs.getLong("dailyMs", 0) + delta)
                        .putLong("sessionMs", prefs.getLong("sessionMs", 0) + delta).apply();
                    if (limited()) showLimit();
                }
            }
            if (status != null) status.setText(summary());
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        display = face("fonts/fraunces.ttf", Typeface.SERIF);
        body = face("fonts/outfit.ttf", Typeface.SANS_SERIF);
        medium = face("fonts/outfit-medium.ttf", Typeface.SANS_SERIF);
        prefs = getSharedPreferences("quiet-local", MODE_PRIVATE);
        loadRooms();
        if (!prefs.getBoolean("scrollFixed", false))
            prefs.edit().putBoolean("scrollFixed", true).putLong("sessionMs", 0).putLong("cooldown", 0).putLong("dailyMs", 0).apply();
        rollDay();
        try (InputStream in = getAssets().open("filter.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            script = out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) { throw new IllegalStateException("Missing bundled rules", e); }

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAPER);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets cut = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            if (v.getPaddingLeft() != cut.left || v.getPaddingTop() != cut.top
                    || v.getPaddingRight() != cut.right || v.getPaddingBottom() != cut.bottom) {
                v.setPadding(cut.left, cut.top, cut.right, cut.bottom);
            }
            return WindowInsets.CONSUMED;
        });
        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(8), dp(16), dp(8));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_mark);
        mark.setAdjustViewBounds(true);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        bar.addView(mark, new LinearLayout.LayoutParams(dp(36), dp(20)));
        TextView word = text("Still.", 18, INK);
        word.setTypeface(display);
        word.setLetterSpacing(-0.03f);
        word.setPadding(dp(8), 0, dp(8), 0);
        bar.addView(word);
        status = text(summary(), 12, MUTED);
        status.setGravity(Gravity.END);
        status.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        status.setMaxLines(2);
        bar.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        root.addView(bar);
        roomsBar = new LinearLayout(this);
        roomsBar.setOrientation(LinearLayout.HORIZONTAL);
        roomsBar.setGravity(Gravity.CENTER_VERTICAL);
        roomsBar.setBackgroundColor(PAPER);
        roomsBar.setVisibility(View.GONE);
        root.addView(roomsBar, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        ruleTop = rule();
        root.addView(ruleTop, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));
        content = new FrameLayout(this);
        content.setBackgroundColor(PAPER);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        try {
            web = new WebView(this);
        } catch (RuntimeException failed) {
            web = null;
        }
        if (web != null) {
            web.setBackgroundColor(PAPER);
            content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(panel);
        content.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        panel.setTag(scroll);
        ruleBottom = rule();
        root.addView(ruleBottom, new LinearLayout.LayoutParams(-1, Math.max(1, dp(1))));
        nav = new LinearLayout(this);
        nav.setPadding(0, dp(2), 0, dp(2));
        addNav("Home", "home", this::showHome);
        addNav("Inbox", "inbox", () -> navigate(BASE + "/direct/inbox/"));
        addNav("Feed", "feed", this::openFeed);
        addNav("Settings", "settings", this::settings);
        root.addView(nav);
        setContentView(root);
        WindowInsetsController bars = getWindow().getInsetsController();
        if (bars != null) {
            bars.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerBack();
        if (web == null) { showWebMissing(); return; }
        configureWeb();
        if (!prefs.getBoolean("intro", false)) showIntro();
        else showHome();
    }

    private void showWebMissing() {
        place = "error";
        chrome(false);
        panel.removeAllViews();
        panel.addView(headline("The browser is missing."));
        panel.addView(bodyCopy("Still opens Instagram through the system browser. Enable Android System WebView, or Vanadium, then open Still again."));
    }

    private void configureWeb() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false); s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true); s.setMediaPlaybackRequiresUserGesture(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false); s.setSupportMultipleWindows(true);
        s.setGeolocationEnabled(false);
        WebView.setWebContentsDebuggingEnabled(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return !allowNavigation(request.getUrl().toString());
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                generation++;
                if (!Policy.internal(url)) { web.stopLoading(); showHome(); return; }
                if (Policy.blocked(url)) { rejectRoute(); return; }
                if (!Policy.exempt(url) && limited()) { web.stopLoading(); showLimit(); return; }
                notePage(url);
                if (browsing) {
                    web.setVisibility(View.INVISIBLE);
                    final int page = generation;
                    handler.postDelayed(() -> {
                        if (browsing && page == generation && web != null) web.setVisibility(View.VISIBLE);
                    }, 1500);
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                notePage(url);
                if (!browsing || !Policy.internal(url) || Policy.blocked(url)) return;
                final int page = generation;
                String configuredScript = script + "\nwindow.__ataraxiaStillness && window.__ataraxiaStillness.configure("
                    + roomsConfig() + ");";
                web.evaluateJavascript(configuredScript, value -> {
                    if (browsing && page == generation) web.setVisibility(View.VISIBLE);
                });
                CookieManager.getInstance().flush();
            }
            @Override public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                notePage(url);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && browsing) showError("Instagram could not load. Check the connection, then try the inbox.");
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler ssl, android.net.http.SslError error) {
                ssl.cancel();
                if (browsing) showError("The secure connection failed. Still will not skip a certificate error.");
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame() && browsing && response.getStatusCode() >= 400)
                    showError("Instagram returned an error (" + response.getStatusCode() + "). Try again later.");
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Policy.adHost(request.getUrl().getHost()))
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                return null;
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                content.removeView(web); web.destroy(); web = null; recreate(); return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean gesture, Message message) {
                Toast.makeText(MainActivity.this, "Pop-ups stay closed.", Toast.LENGTH_SHORT).show();
                return false;
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                if (!Policy.internal(web.getUrl())) { callback.onReceiveValue(null); return true; }
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(Intent.createChooser(intent, "Choose media to upload"), 42); }
                catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback = null; }
                return true;
            }
        });
        web.setDownloadListener((url, ua, disposition, mime, length) ->
            Toast.makeText(this, "Downloads stay in the browser.", Toast.LENGTH_LONG).show());
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 42 && fileCallback != null) {
            List<Uri> chosen = new ArrayList<>();
            if (result == RESULT_OK && data != null && Policy.internal(web.getUrl())) {
                if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++)
                    chosen.add(data.getClipData().getItemAt(i).getUri());
                else if (data.getData() != null) chosen.add(data.getData());
            }
            chosen.removeIf(u -> !"content".equals(u.getScheme()));
            fileCallback.onReceiveValue(chosen.isEmpty() ? null : chosen.toArray(new Uri[0]));
            fileCallback = null;
        }
    }

    private boolean allowNavigation(String url) {
        if (Policy.internal(url)) {
            if (Policy.blocked(url)) { rejectRoute(); return false; }
            if (!Policy.exempt(url) && limited()) { showLimit(); return false; }
            return true;
        }
        Uri uri = Uri.parse(url);
        if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) {
            String host = uri.getHost() == null ? "that site" : uri.getHost();
            confirm("Leave Still?", "Open " + host + " in your browser? Still's limits won't apply there.",
                "Open browser", () -> {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
                    catch (ActivityNotFoundException e) { Toast.makeText(this, "No browser available.", Toast.LENGTH_SHORT).show(); }
                });
        } else Toast.makeText(this, "Sign in on Instagram's own page.", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void navigate(String url) {
        rollDay(); refreshSession();
        if (!allowNavigation(url)) return;
        dismissOverlay();
        browsing = true;
        ((View) panel.getTag()).setVisibility(View.GONE);
        chrome(true);
        if (url.contains("/direct")) place = "inbox";
        else if (Policy.feed(url)) place = "feed";
        else place = "";
        markNav();
        web.setVisibility(View.VISIBLE);
        web.onResume();
        lastTick = SystemClock.elapsedRealtime();
        web.loadUrl(url);
    }

    private void rejectRoute() {
        web.stopLoading();
        showHome();
        Toast.makeText(this, "Reels, Explore, Shop, and Live stay out.", Toast.LENGTH_SHORT).show();
    }

    private void openFeed() { refreshSession(); if (limited()) showLimit(); else navigate(BASE + "/"); }

    private void notePage(String url) {
        String next = url == null ? "" : url;
        if (next.equals(pageUrl)) return;
        pageUrl = next;
        showRooms();
    }

    private void rollDay() {
        String today = LocalDate.now().toString();
        if (!today.equals(prefs.getString("day", "")))
            prefs.edit().putString("day", today).putLong("dailyMs", 0).apply();
    }

    private void refreshSession() {
        long until = prefs.getLong("cooldown", 0);
        if (until > 0 && System.currentTimeMillis() >= until) {
            prefs.edit().putLong("sessionMs", 0).putLong("cooldown", 0).remove("seen").apply();
        }
    }

    private boolean limited() {
        return Budget.limited(prefs.getLong("dailyMs", 0), prefs.getLong("sessionMs", 0), 0,
            prefs.getInt("dailyMinutes", 15), prefs.getInt("sessionMinutes", 5), prefs.getInt("postLimit", 10),
            prefs.getLong("cooldown", 0), System.currentTimeMillis());
    }

    private String summary() {
        long remaining = Math.max(0, prefs.getInt("dailyMinutes", 15) * 60000L - prefs.getLong("dailyMs", 0));
        return (remaining / 60000) + "m " + String.format(Locale.US, "%02d", (remaining / 1000) % 60) + "s left today";
    }

    private void loadRooms() {
        openRooms.clear();
        String saved = prefs.getString("rooms", null);
        if (saved == null) Collections.addAll(openRooms, "friends", "influencers", "celebrities");
        else for (String part : saved.split(",")) if (!part.isEmpty()) openRooms.add(part);
        try { people = new JSONObject(prefs.getString("people", "{}")); }
        catch (JSONException e) { people = new JSONObject(); }
    }

    private String roomsConfig() {
        JSONObject config = new JSONObject();
        try {
            config.put("rooms", new JSONArray(openRooms));
            config.put("people", people);
        } catch (JSONException ignored) { }
        return config.toString();
    }

    private void saveRooms() {
        prefs.edit().putString("rooms", String.join(",", openRooms)).putString("people", people.toString()).apply();
        if (web != null && browsing)
            web.evaluateJavascript("window.__ataraxiaStillness&&window.__ataraxiaStillness.configure(" + roomsConfig() + ")", null);
    }

    private void showRooms() {
        if (roomsBar == null) return;
        roomsBar.removeAllViews();
        String who = Policy.profileName(pageUrl);
        boolean feed = browsing && Policy.feed(pageUrl);
        boolean profile = browsing && who != null;
        if (!feed && !profile) { roomsBar.setVisibility(View.GONE); return; }
        roomsBar.setVisibility(View.VISIBLE);
        if (feed) {
            roomCell("Friends", openRooms.contains("friends"), () -> toggleRoom("friends"));
            roomCell("Influencers", openRooms.contains("influencers"), () -> toggleRoom("influencers"));
            roomCell("Celebrities", openRooms.contains("celebrities"), () -> toggleRoom("celebrities"));
        } else {
            TextView name = text(who, 13, MUTED);
            name.setPadding(dp(12), 0, dp(8), 0);
            name.setSingleLine(true);
            roomsBar.addView(name);
            String filed = people.optString(who, "");
            roomCell("Friend", "friend".equals(filed), () -> filePerson(who, "friend"));
            roomCell("Influencer", "influencer".equals(filed), () -> filePerson(who, "influencer"));
            roomCell("Celebrity", "celebrity".equals(filed), () -> filePerson(who, "celebrity"));
        }
    }

    private void toggleRoom(String room) {
        if (!openRooms.remove(room)) openRooms.add(room);
        saveRooms();
        showRooms();
    }

    private void filePerson(String name, String kind) {
        if (kind.equals(people.optString(name, ""))) people.remove(name);
        else try { people.put(name, kind); } catch (JSONException ignored) { }
        saveRooms();
        showRooms();
    }

    private void roomCell(String label, boolean on, Runnable action) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(0, dp(8), 0, dp(6));
        View line = new View(this);
        line.setBackgroundColor(on ? SAGE : PAPER);
        cell.addView(line, new LinearLayout.LayoutParams(dp(14), Math.max(1, dp(2))));
        TextView t = text(label, 13, on ? INK : MUTED);
        t.setTypeface(on ? medium : body);
        t.setPadding(0, dp(4), 0, 0);
        cell.addView(t);
        cell.setOnClickListener(v -> action.run());
        cell.setClickable(true);
        roomsBar.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    }

    private void basePanel() {
        generation++;
        browsing = false;
        dismissOverlay();
        web.setVisibility(View.GONE);
        web.onPause();
        ((View) panel.getTag()).setVisibility(View.VISIBLE);
        panel.removeAllViews();
        showRooms();
    }

    private void chrome(boolean show) {
        int v = show ? View.VISIBLE : View.GONE;
        bar.setVisibility(v);
        ruleTop.setVisibility(v);
        ruleBottom.setVisibility(v);
        nav.setVisibility(v);
        if (!show && roomsBar != null) roomsBar.setVisibility(View.GONE);
    }

    private void showIntro() {
        basePanel();
        place = "intro";
        chrome(false);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_mark);
        mark.setAdjustViewBounds(true);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(72), dp(40));
        mp.topMargin = dp(36);
        mp.bottomMargin = dp(8);
        panel.addView(mark, mp);
        TextView name = text("Still.", 22, INK);
        name.setTypeface(display);
        name.setLetterSpacing(-0.03f);
        name.setPadding(0, 0, 0, dp(18));
        panel.addView(name);
        panel.addView(headline("Instagram, quietly."));
        panel.addView(bodyCopy("You sign in on Instagram's own page. Still never sees the password."));
        space(18);
        button(panel, "Continue", true, () -> {
            prefs.edit().putBoolean("intro", true).apply();
            navigate(BASE + "/accounts/login/");
        });
    }

    private void showHome() {
        basePanel();
        place = "home";
        chrome(true);
        panel.addView(headline("Friends in one room."));
        panel.addView(bodyCopy("Open the inbox, or browse a little."));
        space(8);
        button(panel, "Open Instagram inbox", true, () -> navigate(BASE + "/direct/inbox/"));
        button(panel, "Browse a little", false, this::openFeed);
        card(prefs.getInt("sessionMinutes", 5) + " minutes a session\n"
            + prefs.getInt("dailyMinutes", 15) + " minutes of browsing a day\n"
            + "Then a 10-minute break.\n\n"
            + "Inbox does not spend that time.");
        panel.addView(note("Meta still sees the activity. Limits apply only inside Still."));
        markNav();
        status.setText(summary());
        showRooms();
    }

    private void showLimit() {
        if (prefs.getLong("cooldown", 0) == 0)
            prefs.edit().putLong("cooldown", System.currentTimeMillis() + 10 * 60000L).apply();
        basePanel();
        place = "limit";
        chrome(true);
        panel.addView(headline("Enough for now."));
        boolean daily = prefs.getLong("dailyMs", 0) >= prefs.getInt("dailyMinutes", 15) * 60000L;
        panel.addView(bodyCopy(daily
            ? "You've used today's browsing. The inbox is still open."
            : "This sitting is finished. The inbox is still open."));
        space(12);
        button(panel, "Open Instagram inbox", true, () -> navigate(BASE + "/direct/inbox/"));
        button(panel, "Back home", false, this::showHome);
        markNav();
    }

    private void showError(String message) {
        basePanel();
        place = "error";
        chrome(true);
        panel.addView(headline("Connection paused."));
        panel.addView(bodyCopy(message));
        space(12);
        button(panel, "Open Instagram inbox", true, () -> navigate(BASE + "/direct/inbox/"));
        markNav();
    }

    private void settings() {
        basePanel();
        place = "settings";
        chrome(true);
        panel.addView(headline("Settings"));
        panel.addView(bodyCopy("Open someone's profile, then file them as a friend, an influencer, or a celebrity. The feed row chooses which of those to keep."));
        choiceRow("Minutes a session", "sessionMinutes", new int[]{2, 5, 10}, 5);
        choiceRow("Minutes a day", "dailyMinutes", new int[]{5, 15, 30, 60}, 15);
        space(12);
        button(panel, "Clear Instagram login", false, () -> confirm(
            "Clear Instagram login?",
            "Removes the login stored in this app. Your limits remain. Instagram's own account is left as it is.",
            "Clear", () -> {
                web.stopLoading();
                showHome();
                web.loadUrl("about:blank");
                CookieManager.getInstance().removeAllCookies(value -> CookieManager.getInstance().flush());
                WebStorage.getInstance().deleteAllData();
                web.clearCache(true);
                web.clearHistory();
            }));
        panel.addView(note("Meta still sees the activity. Limits apply only inside Still."));
        markNav();
    }

    private void showChoices(String title, String key, int[] values, int fallback) {
        basePanel();
        place = "choices";
        chrome(true);
        panel.addView(headline(title));
        int current = prefs.getInt(key, fallback);
        for (int value : values) {
            int chosen = value;
            button(panel, String.valueOf(value), value == current, () -> {
                prefs.edit().putInt(key, chosen).apply();
                settings();
            });
        }
        button(panel, "Back", false, this::settings);
        markNav();
    }

    private void choiceRow(String label, String key, int[] values, int fallback) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setMinimumHeight(dp(48));
        row.setBackground(paperButton(false));
        row.setClickable(true);
        TextView l = text(label, 15, INK);
        l.setTypeface(medium);
        TextView v = text(String.valueOf(prefs.getInt(key, fallback)), 15, SAGE);
        v.setTypeface(medium);
        row.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(v);
        row.setOnClickListener(x -> showChoices(label, key, values, fallback));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.bottomMargin = dp(8);
        panel.addView(row, lp);
    }

    private void confirm(String title, String message, String ok, Runnable action) {
        dismissOverlay();
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setBackgroundColor(PAPER);
        sheet.setPadding(dp(20), dp(28), dp(20), dp(28));
        sheet.setGravity(Gravity.CENTER_VERTICAL);
        sheet.addView(headline(title));
        sheet.addView(bodyCopy(message));
        button(sheet, ok, true, () -> { dismissOverlay(); action.run(); });
        button(sheet, "Cancel", false, this::dismissOverlay);
        overlay = sheet;
        content.addView(sheet, new FrameLayout.LayoutParams(-1, -1));
    }

    private void dismissOverlay() {
        if (overlay != null) { content.removeView(overlay); overlay = null; }
    }

    private Typeface face(String asset, Typeface fallback) {
        try { return Typeface.createFromAsset(getAssets(), asset); }
        catch (RuntimeException e) { return fallback; }
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(body);
        return t;
    }

    private TextView headline(String s) {
        TextView t = text(s, 32, INK);
        t.setTypeface(display);
        t.setLetterSpacing(-0.03f);
        t.setLineSpacing(0, 1.05f);
        t.setPadding(0, dp(6), 0, dp(8));
        return t;
    }

    private TextView bodyCopy(String s) {
        TextView t = text(s, 16, MUTED);
        t.setLineSpacing(0, 1.4f);
        t.setPadding(0, dp(2), 0, dp(8));
        return t;
    }

    private TextView note(String s) {
        TextView t = text(s, 13, MUTED);
        t.setLineSpacing(0, 1.4f);
        t.setPadding(0, dp(8), 0, 0);
        return t;
    }

    private void space(int dpSize) {
        Space s = new Space(this);
        panel.addView(s, new LinearLayout.LayoutParams(1, dp(dpSize)));
    }

    private View rule() {
        View v = new View(this);
        v.setBackgroundColor(HAIR);
        return v;
    }

    private GradientDrawable paperButton(boolean primary) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        if (primary) bg.setColor(SAGE);
        else { bg.setColor(CARD); bg.setStroke(Math.max(1, dp(1)), HAIR); }
        return bg;
    }

    private void card(String bodyText) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(paperButton(false));
        TextView t = text(bodyText, 16, INK);
        t.setLineSpacing(dp(3), 1f);
        c.addView(t);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        lp.bottomMargin = dp(8);
        panel.addView(c, lp);
    }

    private void button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTransformationMethod(null);
        b.setTypeface(medium);
        b.setTextSize(15);
        b.setTextColor(primary ? PAPER : INK);
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setMinHeight(dp(48));
        b.setMinimumHeight(dp(48));
        b.setPadding(dp(16), 0, dp(16), 0);
        b.setBackground(paperButton(primary));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.bottomMargin = dp(8);
        parent.addView(b, lp);
    }

    private void addNav(String label, String id, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTag(id);
        b.setAllCaps(false);
        b.setTransformationMethod(null);
        b.setTextSize(13);
        b.setTypeface(body);
        b.setTextColor(MUTED);
        b.setStateListAnimator(null);
        b.setElevation(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(0, 0, 0, 0);
        b.setBackgroundColor(PAPER);
        b.setOnClickListener(v -> action.run());
        nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }

    private void markNav() {
        for (int i = 0; i < nav.getChildCount(); i++) {
            View child = nav.getChildAt(i);
            if (!(child instanceof Button)) continue;
            Button b = (Button) child;
            boolean on = place.equals(String.valueOf(b.getTag()))
                || ("choices".equals(place) && "settings".equals(b.getTag()));
            b.setTextColor(on ? INK : MUTED);
            b.setTypeface(on ? medium : body);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        active = true;
        lastTick = SystemClock.elapsedRealtime();
        if (web != null && browsing) web.onResume();
        handler.post(ticker);
    }

    @Override protected void onPause() {
        active = false;
        handler.removeCallbacks(ticker);
        if (web != null) web.onPause();
        super.onPause();
    }

    /** API 33 and above. Kept out of onCreate so older devices never resolve this class. */
    private void registerBack() {
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> {
            if (!interceptBack()) finish();
        });
    }

    private boolean interceptBack() {
        if (overlay != null) { dismissOverlay(); return true; }
        if ("choices".equals(place)) { settings(); return true; }
        if (browsing || "settings".equals(place) || "limit".equals(place) || "error".equals(place)) {
            showHome();
            return true;
        }
        return false;
    }

    @Override @SuppressWarnings("deprecation") public void onBackPressed() {
        if (!interceptBack()) super.onBackPressed();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(ticker);
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (web != null) { content.removeView(web); web.destroy(); }
        super.onDestroy();
    }
}
