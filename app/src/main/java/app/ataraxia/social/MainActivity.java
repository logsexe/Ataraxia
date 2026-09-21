package app.ataraxia.social;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

/** Small, dependency-free Android pilot. All browsing happens in this app's WebView. */
public class MainActivity extends androidx.activity.ComponentActivity {
    private static final String BASE = "https://www.instagram.com";
    private static final int BG = 0xff0b1411, SURFACE = 0xff111e19, CARD = 0xff182721, CARD_ALT = 0xff20332a;
    private static final int INK = 0xfff3f0e7, MUTED = 0xff9fb1a7, ACCENT = 0xffbce8c9, ACCENT_STRONG = 0xff7fd29b;
    private static final int LINE = 0xff2d4137, DANGER = 0xffffb4a8;
    private SharedPreferences prefs;
    private WebView web;
    private LinearLayout root, panel;
    private AtaraxiaTopBarView composeTopBar;
    private AtaraxiaBottomBarView composeBottomBar;
    private FrameLayout content;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active, browsing, waitingSnapshot;
    private long lastTick;
    private String script;
    private ValueCallback<Uri[]> fileCallback;
    private final java.util.concurrent.ExecutorService backupIO=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final Set<String> seen = new HashSet<>();
    private int generation, sessionGeneration;
    private final Runnable ticker = new Runnable() {
        public void run() {
            if (!active) return;
            rollDay();
            refreshSession();
            long now = SystemClock.elapsedRealtime();
            long delta = Math.min(5000, Math.max(0, now - lastTick));
            lastTick = now;
            String url = web.getUrl();
            if (browsing && Policy.internal(url)) {
                if (Policy.blocked(url,focused())) { rejectRoute(); }
                else if (!Policy.exempt(url)) {
                    prefs.edit().putLong("dailyMs", prefs.getLong("dailyMs",0) + delta)
                        .putLong("sessionMs", prefs.getLong("sessionMs",0) + delta).apply();
                    if (limited()) showLimit();
                }
                if (browsing && !waitingSnapshot && Policy.feed(url)) pollPosts();
            }
            String nextSummary = summary();
            composeTopBar.update(focused()?"FOCUSED":"BALANCED",nextSummary);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setNavigationBarDividerColor(BG);
        prefs = getSharedPreferences("quiet-local", MODE_PRIVATE);
        prefs.edit().remove("savedProfiles").apply();
        rollDay();
        seen.addAll(prefs.getStringSet("seen", Collections.emptySet()));
        try (InputStream in = getAssets().open("filter.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            script = out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) { throw new IllegalStateException("Missing bundled rules", e); }
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(20),dp(8),dp(20),dp(10));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(dp(20)+bars.left,dp(8)+bars.top,dp(20)+bars.right,dp(10)+bars.bottom);
            return insets;
        });
        composeTopBar = new AtaraxiaTopBarView(this);
        composeTopBar.update(focused()?"FOCUSED":"BALANCED",summary());
        root.addView(composeTopBar,new LinearLayout.LayoutParams(-1,-2));
        content = new FrameLayout(this);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        web = new WebView(this); web.setBackgroundColor(BG);
        content.addView(web,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(0,dp(28),0,dp(28));
        scroll.addView(panel); content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        panel.setTag(scroll);
        composeBottomBar = new AtaraxiaBottomBarView(this);
        composeBottomBar.setActions(()->showHome(),()->navigate(BASE+"/direct/inbox/"),()->openFeed(),()->settings());
        root.addView(composeBottomBar,new LinearLayout.LayoutParams(-1,dp(80)));
        setContentView(root);
        configureWeb();
        showLanding();
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
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                return !allowNavigation(request.getUrl().toString());
            }
            @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap icon) {
                generation++; waitingSnapshot=false;
                if (!Policy.internal(url)) { web.stopLoading(); showHome(); return; }
                if (Policy.blocked(url,focused())) { rejectRoute(); return; }
                if (!Policy.exempt(url) && limited()) { web.stopLoading(); showLimit(); return; }
                if (browsing) web.setVisibility(View.INVISIBLE);
            }
            @Override public void onPageFinished(WebView view,String url) {
                if (!browsing || !Policy.internal(url) || Policy.blocked(url,focused())) return;
                final int page = generation;
                String configuredScript = script + "\nwindow.__ataraxiaStillness && window.__ataraxiaStillness.configure({focused:" + focused() + ",limit:"
                    + prefs.getInt("postLimit",10) + ",ids:" + new JSONArray(new ArrayList<>(seen)).toString() + "});";
                web.evaluateJavascript(configuredScript, value -> {
                    if (browsing && page==generation) web.setVisibility(View.VISIBLE);
                });
                CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView view,WebResourceRequest request,WebResourceError error) {
                if (request.isForMainFrame() && browsing) showError("Instagram could not load. Check your connection and VPN, then try again.");
            }
            @Override public void onReceivedSslError(WebView view,SslErrorHandler ssl,android.net.http.SslError error) {
                ssl.cancel(); if(browsing) showError("Secure connection failed. Check your connection or try later; certificate errors cannot be bypassed.");
            }
            @Override public void onReceivedHttpError(WebView view,WebResourceRequest request,WebResourceResponse response) {
                if (request.isForMainFrame() && browsing && response.getStatusCode()>=400)
                    showError("Instagram returned an error ("+response.getStatusCode()+"). Try again later. Login restrictions may affect web clients.");
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                if (Policy.adHost(request.getUrl().getHost())) return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                return null;
            }
            @Override public boolean onRenderProcessGone(WebView view,RenderProcessGoneDetail detail) {
                content.removeView(web); web.destroy(); web=null; recreate(); return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest request) { request.deny(); }
            @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean gesture,Message message) {
                Toast.makeText(MainActivity.this,"Pop-ups are disabled in this pilot.",Toast.LENGTH_SHORT).show(); return false;
            }
            @Override public boolean onShowFileChooser(WebView view,ValueCallback<Uri[]> callback,FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                if (!Policy.internal(web.getUrl())) { callback.onReceiveValue(null); return true; }
                fileCallback=callback;
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(Intent.createChooser(intent,"Choose media to upload"),42); }
                catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback=null; }
                return true;
            }
        });
        web.setDownloadListener((url,ua,disposition,mime,length)->Toast.makeText(this,"Downloads are not supported in this pilot.",Toast.LENGTH_LONG).show());
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==43 || request==44) {
            if(result==RESULT_OK && data!=null && data.getData()!=null && "content".equals(data.getData().getScheme()))
                transferSettings(request==43,data.getData());
            return;
        }
        if (request==42 && fileCallback!=null) {
            List<Uri> chosen=new ArrayList<>();
            if (result==RESULT_OK && data!=null && Policy.internal(web.getUrl())) {
                if (data.getClipData()!=null) for(int i=0;i<data.getClipData().getItemCount();i++) chosen.add(data.getClipData().getItemAt(i).getUri());
                else if(data.getData()!=null) chosen.add(data.getData());
            }
            // Accept system content grants, never arbitrary file:// or network URLs.
            chosen.removeIf(u->!"content".equals(u.getScheme()));
            fileCallback.onReceiveValue(chosen.isEmpty()?null:chosen.toArray(new Uri[0])); fileCallback=null;
        }
    }
    private boolean allowNavigation(String url) {
        if (Policy.internal(url)) {
            if (Policy.blocked(url,focused())) { rejectRoute(); return false; }
            if (!Policy.exempt(url) && limited()) { showLimit(); return false; }
            return true;
        }
        Uri uri=Uri.parse(url);
        if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
            new AlertDialog.Builder(this).setTitle("Leave Ataraxia?")
                .setMessage("Open "+uri.getHost()+" in your browser? Ataraxia's limits won't apply there.")
                .setNegativeButton("Stay here",null).setPositiveButton("Open browser",(d,w)->{
                    try { startActivity(new Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE)); }
                    catch(ActivityNotFoundException e){ Toast.makeText(this,"No browser available",Toast.LENGTH_SHORT).show(); }
                }).show();
        else Toast.makeText(this,"App redirects are blocked. Use Instagram web login.",Toast.LENGTH_SHORT).show();
        return false;
    }
    private void navigate(String url) {
        rollDay(); refreshSession();
        if (!allowNavigation(url)) return;
        browsing=true; ((View)panel.getTag()).setVisibility(View.GONE);
        selectNav(Policy.exempt(url)?"Inbox":"Feed");
        web.setAlpha(0f);web.setVisibility(View.VISIBLE);web.onResume();lastTick=SystemClock.elapsedRealtime();web.loadUrl(url);
        web.animate().alpha(1f).setDuration(180).start();
    }
    private boolean focused() { return prefs.getBoolean("focused",true); }
    private void rejectRoute() {
        web.stopLoading(); showHome(); Toast.makeText(this,focused()?"Focused mode: home feed, Reels and Explore are off.":"Reels and Explore are switched off.",Toast.LENGTH_SHORT).show();
    }
    private void openFeed() { if(focused()){rejectRoute();return;} refreshSession(); if(limited()) showLimit(); else navigate(BASE+"/"); }
    private void pollPosts() {
        waitingSnapshot=true; final int page=generation, session=sessionGeneration;
        web.evaluateJavascript("JSON.stringify(window.__ataraxiaStillness ? window.__ataraxiaStillness.snapshot() : null)",raw->{
            waitingSnapshot=false;
            if(!browsing || page!=generation || session!=sessionGeneration || !Policy.internal(web.getUrl()) || !Policy.feed(web.getUrl())) return;
            try {
                Object parsed=new JSONTokener(raw).nextValue();
                if(!(parsed instanceof String)) return;
                JSONObject obj=new JSONObject((String)parsed); JSONArray ids=obj.optJSONArray("ids");
                if(ids!=null) for(int i=0;i<Math.min(ids.length(),500);i++) {
                    String id=ids.optString(i); if(id.matches("[A-Za-z0-9_-]{1,80}")) seen.add(id);
                }
                if (!seen.equals(prefs.getStringSet("seen",Collections.emptySet())))
                    prefs.edit().putStringSet("seen",new HashSet<>(seen)).apply();
                if(limited()) showLimit();
            } catch(Exception ignored) { /* Unrecognised markup: native time cap still applies. */ }
        });
    }
    private void rollDay() {
        String today=LocalDate.now().toString();
        if(!today.equals(prefs.getString("day",""))) {
            prefs.edit().putString("day",today).putLong("dailyMs",0).apply();
        }
    }
    private void refreshSession() {
        long until=prefs.getLong("cooldown",0);
        if(until>0 && System.currentTimeMillis()>=until) {
            sessionGeneration++;
            prefs.edit().putLong("sessionMs",0).putLong("cooldown",0).remove("seen").apply(); seen.clear();
            // Also reset a live SPA document: returning from Inbox may not reload it.
            if (web != null && Policy.internal(web.getUrl())) web.evaluateJavascript(
                "window.__ataraxiaStillness && window.__ataraxiaStillness.configure({reset:true,limit:"
                    + prefs.getInt("postLimit",10) + ",ids:[]});",null);
        }
    }
    private boolean limited() {
        return Budget.limited(prefs.getLong("dailyMs",0),prefs.getLong("sessionMs",0),seen.size(),
            prefs.getInt("dailyMinutes",15),prefs.getInt("sessionMinutes",5),prefs.getInt("postLimit",10),
            prefs.getLong("cooldown",0),System.currentTimeMillis());
    }
    private String summary() {
        long remaining=Math.max(0,prefs.getInt("dailyMinutes",15)*60000L-prefs.getLong("dailyMs",0));
        long breakSeconds = Math.max(0,(prefs.getLong("cooldown",0)-System.currentTimeMillis()+999)/1000);
        if (remaining == 0) return "Daily allowance reached · Inbox available";
        if (breakSeconds > 0) return "Break: " + (breakSeconds/60) + "m " + (breakSeconds%60) + "s · Inbox available";
        return (remaining/60000)+"m "+((remaining/1000)%60)+"s left today  ·  "+seen.size()+" / "+prefs.getInt("postLimit",10)+" posts";
    }
    private void basePanel() {
        generation++; browsing=false; waitingSnapshot=false;
        web.setVisibility(View.GONE); web.onPause();
        ((View)panel.getTag()).setVisibility(View.VISIBLE); panel.removeAllViews();
        panel.setPadding(0,dp(28),0,dp(28));
        panel.animate().cancel();panel.setAlpha(0f);panel.setTranslationY(dp(10));
        panel.post(()->panel.animate().alpha(1f).translationY(0f).setDuration(220).start());
        composeTopBar.update(focused()?"FOCUSED":"BALANCED",summary());
    }
    private void showLanding() {
        basePanel();selectNav("Home");panel.setPadding(0,0,0,0);
        AtaraxiaLandingView landing=new AtaraxiaLandingView(this);
        landing.setActions(()->{if(prefs.getBoolean("philosophyIntro",false))showHome();else showPhilosophy(0,true);},
            ()->showPhilosophy(0,!prefs.getBoolean("philosophyIntro",false)));
        panel.addView(landing,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showHome() {
        basePanel();selectNav("Home");panel.setPadding(0,0,0,0);
        int daily=prefs.getInt("dailyMinutes",15);
        float progress=Math.min(1f,prefs.getLong("dailyMs",0)/(daily*60000f));
        AtaraxiaHomeView home=new AtaraxiaHomeView(this);
        home.update(focused(),summary(),progress,prefs.getInt("postLimit",10),
            prefs.getInt("sessionMinutes",5),daily);
        home.setActions(()->navigate(BASE+"/direct/inbox/"),()->openFeed(),()->chooseMode(),()->showPhilosophy(0,false));
        panel.addView(home,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showPhilosophy(int page,boolean onboarding) {
        basePanel();selectNav("Home");panel.setPadding(0,0,0,0);
        AtaraxiaPhilosophyView philosophy=new AtaraxiaPhilosophyView(this);
        philosophy.update(page,onboarding);
        philosophy.setActions(
            ()->showPhilosophy(Math.min(2,page+1),onboarding),
            ()->{if(page==0)showHome();else showPhilosophy(page-1,onboarding);},
            ()->finishPhilosophy(true,onboarding),
            ()->finishPhilosophy(false,onboarding));
        panel.addView(philosophy,new LinearLayout.LayoutParams(-1,-2));
    }
    private void finishPhilosophy(boolean focusedMode,boolean onboarding) {
        prefs.edit().putBoolean("focused",focusedMode).putBoolean("philosophyIntro",true).putBoolean("intro",true).apply();
        web.stopLoading();
        showHome();
        if(onboarding) Toast.makeText(this,focusedMode?"Focused mode selected.":"Balanced mode selected.",Toast.LENGTH_SHORT).show();
    }
    private void showLimit() {
        if(prefs.getLong("cooldown",0)==0) prefs.edit().putLong("cooldown",System.currentTimeMillis()+10*60000L).apply();
        basePanel();selectNav("Home");panel.setPadding(0,0,0,0);
        boolean daily=prefs.getLong("dailyMs",0)>=prefs.getInt("dailyMinutes",15)*60000L;
        AtaraxiaEndView end=new AtaraxiaEndView(this);
        end.update(daily);
        end.setActions(()->navigate(BASE+"/direct/inbox/"),()->showHome());
        panel.addView(end,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showError(String message) { basePanel(); panel.addView(text("Connection paused",28,INK)); panel.addView(text(message,17,MUTED)); button(panel,"Try inbox again",()->navigate(BASE+"/direct/inbox/")); }
    private void settings() {
        basePanel();selectNav("Settings");panel.setPadding(0,0,0,0);
        AtaraxiaSettingsView settings=new AtaraxiaSettingsView(this);
        settings.update(focused(),prefs.getInt("postLimit",10),prefs.getInt("sessionMinutes",5),prefs.getInt("dailyMinutes",15));
        settings.setActions(
            ()->choose("postLimit","Posts per session",new int[]{5,10,15,20}),
            ()->choose("sessionMinutes","Minutes per session",new int[]{2,5,10}),
            ()->choose("dailyMinutes","Daily browsing minutes",new int[]{5,15,30,60}),
            ()->chooseMode(),()->showPrivacy(),()->clearInstagramSession(),
            ()->settingsFile(true),()->settingsFile(false),()->showPhilosophy(0,false));
        panel.addView(settings,new LinearLayout.LayoutParams(-1,-2));
    }
    private void showPrivacy() {
        new AlertDialog.Builder(this).setTitle("Local controls, honest limits")
            .setMessage("Only Internet permission. No analytics, admin, Accessibility, VPN or notification access.\n\nInstagram login cookies remain in this app's private WebView storage; Meta still receives activity. Android backup is disabled. Settings and counters stay on-device.\n\nSponsored-post detection targets recognised feed markup and can miss ads. Post counting can miss unsupported layouts; native time limits remain active.\n\nNo calls, background notifications, downloads or Facebook sign-in support. Limits are voluntary, not parental-control enforcement.")
            .setPositiveButton("Done",null).show();
    }
    private void clearInstagramSession() {
        new AlertDialog.Builder(this).setTitle("Clear Instagram session?")
            .setMessage("Removes this app's login cookies, website storage and cache. Your limits remain.")
            .setNegativeButton("Cancel",null).setPositiveButton("Clear",(a,b)->{
                web.stopLoading(); showHome(); web.loadUrl("about:blank");
                CookieManager.getInstance().removeAllCookies(value->CookieManager.getInstance().flush());
                WebStorage.getInstance().deleteAllData(); web.clearCache(true); web.clearHistory();
            }).show();
    }
    private void chooseMode() {
        new AlertDialog.Builder(this).setTitle("Choose your mode")
            .setSingleChoiceItems(new String[]{"Focused — inbox only", "Balanced — add a capped feed"},focused()?0:1,(d,n)->{
                prefs.edit().putBoolean("focused",n==0).apply();
                // Mode changes never reset time, post counts or the cooldown.
                web.stopLoading(); d.dismiss(); showHome();
            }).setNegativeButton("Cancel",null).show();
    }
    private void settingsFile(boolean export) {
        String message=export
            ? "Exports your mode and boundaries as a readable file. It excludes Instagram login, messages and usage history. Choose a location you trust; cloud document providers may upload it."
            : "Select an Ataraxia settings backup. You can review its mode and boundaries before replacing your settings. Login and current usage counters are not changed.";
        new AlertDialog.Builder(this).setTitle(export?"Export settings":"Import settings").setMessage(message)
            .setNegativeButton("Cancel",null).setPositiveButton("Choose file",(d,w)->{
                Intent intent=new Intent(export?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(export?"text/plain":"*/*");
                if(export) intent.putExtra(Intent.EXTRA_TITLE,"Ataraxia-settings.properties");
                try{startActivityForResult(intent,export?43:44);}
                catch(ActivityNotFoundException e){Toast.makeText(this,"No document picker available.",Toast.LENGTH_LONG).show();}
            }).show();
    }
    private void backupUi(Runnable action) {
        handler.post(()->{if(!isFinishing() && !isDestroyed())action.run();});
    }
    private void transferSettings(boolean export,Uri uri) {
        final SettingsBackup snapshot=new SettingsBackup(focused(),prefs.getInt("postLimit",10),
            prefs.getInt("sessionMinutes",5),prefs.getInt("dailyMinutes",15),new TreeSet<>());
        backupIO.execute(()->{
            try {
                if(export) {
                    try(OutputStream out=getContentResolver().openOutputStream(uri,"wt")) {
                        if(out==null)throw new IOException("No output");
                        out.write(snapshot.encode());out.flush();
                    }
                    backupUi(()->Toast.makeText(this,"Settings exported. Login and usage history were excluded.",Toast.LENGTH_LONG).show());
                } else {
                    final SettingsBackup incoming;
                    try(InputStream in=getContentResolver().openInputStream(uri)) {incoming=SettingsBackup.decode(in);}
                    backupUi(()->confirmSettingsImport(incoming));
                }
            } catch(IOException|IllegalArgumentException|SecurityException e) {
                backupUi(()->new AlertDialog.Builder(this).setTitle(export?"Export failed":"Import rejected")
                    .setMessage(export?"Could not finish writing the file. Remove any incomplete file and try another location.":"The file is unreadable, too large or not a supported settings backup. Your settings have not changed.")
                    .setPositiveButton("Done",null).show());
            }
        });
    }
    private void confirmSettingsImport(SettingsBackup incoming) {
        new AlertDialog.Builder(this).setTitle("Replace settings?")
            .setMessage((incoming.focused?"Focused":"Balanced")+" mode\n"+incoming.posts+" posts per session\n"
                +incoming.sessionMinutes+" minutes per session\n"+incoming.dailyMinutes+" minutes per day\n"
                +"\n\nReplaces current mode and boundaries. Legacy saved profiles are ignored. Existing login, usage counters and cooldown stay unchanged.")
            .setNegativeButton("Cancel",null).setPositiveButton("Replace settings",(d,w)->{
                prefs.edit().putBoolean("focused",incoming.focused).putInt("postLimit",incoming.posts)
                    .putInt("sessionMinutes",incoming.sessionMinutes).putInt("dailyMinutes",incoming.dailyMinutes).apply();
                sessionGeneration++;web.stopLoading();showHome();
                Toast.makeText(this,"Settings restored.",Toast.LENGTH_SHORT).show();
            }).show();
    }
    private void refreshPage() {
        String url=web.getUrl();
        if (!browsing || !Policy.internal(url)) {
            Toast.makeText(this,"Open Feed or Inbox first.",Toast.LENGTH_SHORT).show(); return;
        }
        rollDay(); refreshSession();
        if (allowNavigation(url)) web.reload();
    }
    private void filterStatus() {
        if (!browsing || !Policy.internal(web.getUrl())) {
            new AlertDialog.Builder(this).setTitle("Filter status").setMessage("Open Feed or Inbox to check the current page.")
                .setPositiveButton("Done",null).show(); return;
        }
        final int page=generation;
        web.evaluateJavascript("JSON.stringify(window.__ataraxiaStillness ? window.__ataraxiaStillness.snapshot() : null)",raw->{
            if (isFinishing() || page!=generation || !browsing) return;
            String message="Filter not ready. Try refreshing the page. Native time limits remain active.";
            try {
                Object parsed=new JSONTokener(raw).nextValue();
                if (parsed instanceof String) {
                    JSONObject state=new JSONObject((String)parsed);
                    if (!state.optBoolean("feed")) message="Feed filtering is inactive on this page. Messages are not inspected.";
                    else message="Filter loaded\nPosts or suggestions hidden on this page: "+state.optInt("hiddenAds")
                        +"\nPost containers detected: "+state.optInt("containers")
                        +"\n\n"+(state.optInt("containers")==0
                            ? "Post counting is unavailable in this layout. Native time limits remain active."
                            : "Post counting requires supported links within these containers. Ad filtering is best effort.");
                }
            } catch(Exception ignored) { }
            new AlertDialog.Builder(this).setTitle("Filter status").setMessage(message)
                .setPositiveButton("Done",null).show();
        });
    }
    private void choose(String key,String title,int[] values) {
        String[] labels=Arrays.stream(values).mapToObj(String::valueOf).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle(title).setItems(labels,(d,n)->{prefs.edit().putInt(key,values[n]).apply();showHome();}).show();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable surface(int color,int radius,int strokeColor,int strokeWidth){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));if(strokeWidth>0)g.setStroke(dp(strokeWidth),strokeColor);return g;}
    private android.graphics.drawable.Drawable ripple(int color,GradientDrawable shape){return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(color),shape,null);}
    private LinearLayout container(int color,int radius,int strokeColor,int strokeWidth){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);v.setBackground(surface(color,radius,strokeColor,strokeWidth));v.setElevation(dp(2));return v;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(0,1.12f);t.setFontFeatureSettings("kern");t.setPadding(0,dp(5),0,dp(9));return t;}
    private TextView pill(String label){TextView t=text(label,10,ACCENT);t.setGravity(Gravity.CENTER);t.setLetterSpacing(.12f);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setPadding(dp(12),dp(7),dp(12),dp(7));t.setBackground(surface(CARD_ALT,20,LINE,1));return t;}
    private void eyebrow(String label){TextView t=text(label,11,ACCENT);t.setLetterSpacing(.16f);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));panel.addView(t);}
    private void heading(String label){TextView t=text(label,36,INK);t.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));t.setLineSpacing(dp(2),1.0f);t.setAccessibilityHeading(true);panel.addView(t);}
    private void section(String label){TextView t=text(label,11,MUTED);t.setLetterSpacing(.14f);t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));t.setPadding(0,dp(5),0,dp(12));panel.addView(t);}
    private void space(){Space s=new Space(this);panel.addView(s,new LinearLayout.LayoutParams(1,dp(18)));}
    private void card(String title,String body){LinearLayout box=container(CARD,22,LINE,1);box.setPadding(dp(20),dp(16),dp(20),dp(14));TextView h=text(title,11,ACCENT);h.setLetterSpacing(.13f);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));box.addView(h);box.addView(text(body,16,INK));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);panel.addView(box,p);}
    private void actionTile(String title,String body,String action,Runnable run){LinearLayout box=container(CARD,24,LINE,1);box.setPadding(dp(20),dp(16),dp(20),dp(14));TextView h=text(title,20,INK);h.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));box.addView(h);box.addView(text(body,14,MUTED));TextView a=text(action+"  →",14,ACCENT);a.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));box.addView(a);box.setClickable(true);box.setFocusable(true);box.setForeground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x227fd29b),null,null));box.setOnClickListener(v->run.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);panel.addView(box,p);}
    private void metricRow(String a,String av,String b,String bv,String d,String dv){LinearLayout row=new LinearLayout(this);row.setWeightSum(3);metric(row,a,av);metric(row,b,bv);metric(row,d,dv);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(14);panel.addView(row,p);}
    private void metric(LinearLayout row,String label,String value){LinearLayout box=container(SURFACE,18,LINE,1);box.setPadding(dp(8),dp(12),dp(8),dp(10));TextView v=text(value,17,INK);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));box.addView(v);TextView l=text(label,9,MUTED);l.setGravity(Gravity.CENTER);l.setLetterSpacing(.08f);box.addView(l);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(3),0,dp(3),0);row.addView(box,p);}
    private void stepDots(int page){LinearLayout dots=new LinearLayout(this);dots.setGravity(Gravity.CENTER);for(int i=0;i<3;i++){View dot=new View(this);dot.setBackground(surface(i==page?ACCENT_STRONG:LINE,8,0,0));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(i==page?28:8),dp(8));p.setMargins(dp(4),0,dp(4),dp(18));dots.addView(dot,p);}panel.addView(dots);}
    private void button(LinearLayout parent,String label,Runnable action){TextView b=text(label,15,BG);b.setGravity(Gravity.CENTER);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setPadding(dp(18),0,dp(18),0);b.setBackground(ripple(0x33000000,surface(ACCENT,18,0,0)));b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->{v.animate().scaleX(.98f).scaleY(.98f).setDuration(70).withEndAction(()->{v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();action.run();}).start();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(56));p.bottomMargin=dp(10);parent.addView(b,p);}
    private void secondaryButton(LinearLayout parent,String label,Runnable action){TextView b=text(label,14,INK);b.setGravity(Gravity.CENTER);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setBackground(ripple(0x227fd29b,surface(SURFACE,17,LINE,1)));b.setClickable(true);b.setFocusable(true);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(parent==panel?-1:0,dp(52),parent==panel?0:1);p.setMargins(dp(3),0,dp(3),dp(10));parent.addView(b,p);}
    private void selectNav(String label){if(composeBottomBar!=null)composeBottomBar.select(label);}
    @Override protected void onResume(){super.onResume();active=true;lastTick=SystemClock.elapsedRealtime();if(web!=null && browsing)web.onResume();handler.post(ticker);}
    @Override protected void onPause(){active=false;handler.removeCallbacks(ticker);if(web!=null)web.onPause();super.onPause();}
    @Override public void onBackPressed(){
        if (!browsing) { super.onBackPressed(); return; }
        rollDay(); refreshSession();
        if (web.canGoBack()) {
            WebBackForwardList history=web.copyBackForwardList();
            WebHistoryItem previous=history.getItemAtIndex(history.getCurrentIndex()-1);
            if (previous!=null && Policy.internal(previous.getUrl()) && !Policy.blocked(previous.getUrl(),focused())) {
                if (!Policy.exempt(previous.getUrl()) && limited()) showLimit();
                else web.goBack();
                return;
            }
        }
        showHome();
    }
    @Override protected void onDestroy(){backupIO.shutdownNow();handler.removeCallbacks(ticker);if(fileCallback!=null)fileCallback.onReceiveValue(null);if(web!=null){content.removeView(web);web.destroy();}super.onDestroy();}
}
