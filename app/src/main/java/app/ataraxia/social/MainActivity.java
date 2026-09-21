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
public class MainActivity extends Activity {
    private static final String BASE = "https://www.instagram.com";
    private static final int BG = 0xff101916, CARD = 0xff1c2b24, INK = 0xffedf5ee, MUTED = 0xffa2b5a9, ACCENT = 0xffb9e5c8;
    private SharedPreferences prefs;
    private WebView web;
    private LinearLayout root, panel;
    private TextView status;
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
            if (!nextSummary.contentEquals(status.getText())) status.setText(nextSummary);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("quiet-local", MODE_PRIVATE);
        rollDay();
        seen.addAll(prefs.getStringSet("seen", Collections.emptySet()));
        try (InputStream in = getAssets().open("filter.js")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer,0,count);
            script = out.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) { throw new IllegalStateException("Missing bundled rules", e); }
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(14),dp(8),dp(14),dp(8));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(dp(14)+bars.left,dp(8)+bars.top,dp(14)+bars.right,dp(8)+bars.bottom);
            return insets;
        });
        TextView title = text("ATARAXIA  /  PILOT", 15, ACCENT); title.setTypeface(null,Typeface.BOLD);
        root.addView(title);
        status = text(summary(),12,MUTED); root.addView(status);
        content = new FrameLayout(this);
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        web = new WebView(this); web.setBackgroundColor(BG);
        content.addView(web,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(0,dp(30),0,dp(20));
        scroll.addView(panel); content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        panel.setTag(scroll);
        LinearLayout nav = new LinearLayout(this);
        addNav(nav,"Home",()->showHome()); addNav(nav,"Inbox",()->navigate(BASE+"/direct/inbox/"));
        addNav(nav,"Browse",()->{if(focused())visitProfile();else openFeed();}); addNav(nav,"Settings",()->settings()); root.addView(nav);
        setContentView(root);
        configureWeb();
        if (prefs.getBoolean("philosophyIntro",false)) showHome();
        else showPhilosophy(0,true);
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
        web.setVisibility(View.VISIBLE); web.onResume(); lastTick=SystemClock.elapsedRealtime(); web.loadUrl(url);
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
    }
    private void showHome() {
        basePanel();
        panel.addView(text("Your attention.\nYour choice.",34,INK));
        panel.addView(text(focused()?"Focused · Messages and deliberate profile visits.":"Balanced · Messages and a short, capped feed.",17,MUTED));
        space(); card("INBOX FIRST","Messages remain available after your browsing limit. No background message notifications in this pilot.");
        button(panel,"Open Instagram inbox",()->navigate(BASE+"/direct/inbox/"));
        if (!focused()) button(panel,"Browse a little",()->openFeed());
        button(panel,"Visit a profile",()->visitProfile());
        button(panel,"Saved profiles ("+savedProfiles().size()+")",()->showSavedProfiles());
        button(panel,"Change mode",()->chooseMode());
        button(panel,"Why Ataraxia?",()->showPhilosophy(0,false));
        space(); card("YOUR BOUNDARIES",prefs.getInt("postLimit",10)+" posts per session\n"+prefs.getInt("sessionMinutes",5)+" minutes per session\n"+prefs.getInt("dailyMinutes",15)+" minutes browsing per day\n10-minute break between capped sessions");
        panel.addView(text("Reels + Explore blocked · Sponsored posts filtered where detected\n\nInstagram still processes your account activity. Filtering can miss ads and recommendations. Limits apply only here.",13,MUTED));
    }
    private void showPhilosophy(int page,boolean onboarding) {
        basePanel();
        if(page==0) {
            panel.addView(text("About 4,000 weeks.",13,ACCENT));
            panel.addView(text("Time is the one thing\nyou cannot replace.",34,INK));
            panel.addView(text("An 80-year life is roughly 4,174 weeks. The point is not to fear that number. It is to remember that attention is how life is spent.",17,MUTED));
            space();
            card("A FINITE LIFE","Infinite feeds behave as though your time has no edge. Your life does. Ataraxia makes the boundary visible.");
            card("THE AIM","Use social media deliberately: connect, create, respond—then return to the life beyond the screen.");
            button(panel,"Continue · What is Ataraxia?",()->showPhilosophy(1,onboarding));
            if(!onboarding) button(panel,"Back home",()->showHome());
            return;
        }
        if(page==1) {
            panel.addView(text("Ataraxia",13,ACCENT));
            panel.addView(text("Freedom from\nunnecessary disturbance.",34,INK));
            panel.addView(text("The ancient Greek ideal was not numbness or withdrawal. It was a steadier mind—less governed by noise, impulse and manufactured urgency.",17,MUTED));
            space();
            card("ATTENTION","Notice what is asking for your mind before giving it away.");
            card("INTENTION","Open with a purpose instead of surrendering to whatever appears next.");
            card("MODERATION","Enough is a complete experience. More is not automatically better.");
            card("AGENCY","The boundary belongs to you. Ataraxia supports your choice; it does not claim control over you.");
            button(panel,"Continue · Choose how to enter",()->showPhilosophy(2,onboarding));
            button(panel,"Back",()->showPhilosophy(0,onboarding));
            return;
        }
        panel.addView(text("Choose with intention.",13,ACCENT));
        panel.addView(text("What are you here to do?",34,INK));
        panel.addView(text("Both modes keep Reels and Explore blocked. You can change modes and boundaries whenever you choose.",17,MUTED));
        space();
        card("FOCUSED","Messages and deliberate profile visits. The home feed stays unavailable.");
        button(panel,"Begin in Focused mode",()->finishPhilosophy(true,onboarding));
        card("BALANCED","Messages, profiles and a short feed contained by your post, session and daily limits.");
        button(panel,"Begin in Balanced mode",()->finishPhilosophy(false,onboarding));
        button(panel,"Back",()->showPhilosophy(1,onboarding));
        space();
        panel.addView(text("Ataraxia is free and has no developer ads or analytics. It displays Instagram's website, so Meta still processes your account activity. Filtering is best effort and limits apply only inside this app.",13,MUTED));
    }
    private void finishPhilosophy(boolean focusedMode,boolean onboarding) {
        prefs.edit().putBoolean("focused",focusedMode).putBoolean("philosophyIntro",true).putBoolean("intro",true).apply();
        web.stopLoading();
        showHome();
        if(onboarding) Toast.makeText(this,focusedMode?"Focused mode selected.":"Balanced mode selected.",Toast.LENGTH_SHORT).show();
    }
    private void showLimit() {
        if(prefs.getLong("cooldown",0)==0) prefs.edit().putLong("cooldown",System.currentTimeMillis()+10*60000L).apply();
        basePanel(); panel.addView(text("Enough for now.",34,INK));
        boolean daily=prefs.getLong("dailyMs",0)>=prefs.getInt("dailyMinutes",15)*60000L;
        panel.addView(text(daily?"You've used today's browsing allowance. Your inbox is still available.":"You've reached your session boundary. Take a 10-minute break; reopening the app won't reset it.",18,MUTED));
        space(); button(panel,"Go to inbox",()->navigate(BASE+"/direct/inbox/")); button(panel,"Back home",()->showHome());
    }
    private void showError(String message) { basePanel(); panel.addView(text("Connection paused",28,INK)); panel.addView(text(message,17,MUTED)); button(panel,"Try inbox again",()->navigate(BASE+"/direct/inbox/")); }
    private void settings() {
        new AlertDialog.Builder(this).setTitle("Your boundaries").setItems(new String[]{"Posts per session","Minutes per session","Daily browsing minutes","Privacy and limitations","Clear Instagram login","Refresh current page","Filter status","Focused / Balanced mode","Export settings","Import settings","Philosophy and purpose"},(d,which)->{
            if(which==0) choose("postLimit","Posts per session",new int[]{5,10,15,20});
            if(which==1) choose("sessionMinutes","Minutes per session",new int[]{2,5,10});
            if(which==2) choose("dailyMinutes","Daily browsing minutes",new int[]{5,15,30,60});
            if(which==3) new AlertDialog.Builder(this).setTitle("Local controls, honest limits")
                .setMessage("Only Internet permission. No analytics, admin, Accessibility, VPN or notification access.\n\nInstagram login cookies remain in this app's private WebView storage; Meta still receives activity. Android backup is disabled. Settings and counters stay on-device.\n\nSponsored-post detection currently targets English and Afrikaans labels in recognised feed markup. It can miss ads. Post counting can miss unsupported layouts; native time limits remain active.\n\nNo calls, background notifications, downloads or Facebook sign-in support. Use username/password and 2FA on Instagram's own page.\n\nLimits are voluntary: settings, clock changes, clearing app data or other apps can bypass them. This is not parental-control enforcement.")
                .setPositiveButton("Done",null).show();
            if(which==8) settingsFile(true);
            if(which==9) settingsFile(false);
            if(which==10) showPhilosophy(0,false);
            if(which==7) chooseMode();
            if(which==5) refreshPage();
            if(which==6) filterStatus();
            if(which==4) new AlertDialog.Builder(this).setTitle("Clear Instagram session?").setMessage("Removes this app's login cookies, website storage and cache. Your limits remain.")
                .setNegativeButton("Cancel",null).setPositiveButton("Clear",(a,b)->{
                    web.stopLoading(); showHome(); web.loadUrl("about:blank");
                    CookieManager.getInstance().removeAllCookies(value->CookieManager.getInstance().flush());
                    WebStorage.getInstance().deleteAllData(); web.clearCache(true); web.clearHistory();
                }).show();
        }).show();
    }
    private void chooseMode() {
        new AlertDialog.Builder(this).setTitle("Choose your mode")
            .setSingleChoiceItems(new String[]{"Focused — inbox and profiles", "Balanced — add a capped feed"},focused()?0:1,(d,n)->{
                prefs.edit().putBoolean("focused",n==0).apply();
                // Mode changes never reset time, post counts or the cooldown.
                web.stopLoading(); d.dismiss(); showHome();
            }).setNegativeButton("Cancel",null).show();
    }
    private void visitProfile() {
        EditText input=new EditText(this); input.setSingleLine(true); input.setHint("Instagram username");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Visit a profile").setView(input)
            .setNegativeButton("Cancel",null).setPositiveButton("Open",null).create();
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL,"Save & open",(DialogInterface.OnClickListener)null);
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->openEnteredProfile(input,dialog,false));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->openEnteredProfile(input,dialog,true));
        });
        dialog.show();
    }
    private SortedSet<String> savedProfiles() {
        SortedSet<String> result=new TreeSet<>();
        for (String raw:prefs.getStringSet("savedProfiles",Collections.emptySet())) {
            String name=Policy.profileName(raw); if(name!=null && result.size()<20) result.add(name);
        }
        return result;
    }
    private void openEnteredProfile(EditText input,AlertDialog dialog,boolean save) {
        String name=Policy.profileName(input.getText().toString());
        if (name==null) {input.setError("Enter a profile username, not a link or Instagram section.");return;}
        if(save) {
            Set<String> names=savedProfiles();
            if(names.size()>=20 && !names.contains(name)) {input.setError("You have 20 saved profiles. Remove one first.");return;}
            names.add(name);prefs.edit().putStringSet("savedProfiles",new HashSet<>(names)).apply();
        }
        dialog.dismiss();navigate(BASE+"/"+name+"/");
    }
    private void showSavedProfiles() {
        String[] names=savedProfiles().toArray(new String[0]);
        if(names.length==0) {
            new AlertDialog.Builder(this).setTitle("Saved profiles")
                .setMessage("Save up to 20 usernames for deliberate visits. They stay on this device; nothing is fetched until you open a profile.")
                .setPositiveButton("Add a profile",(d,w)->visitProfile()).setNegativeButton("Close",null).show();return;
        }
        new AlertDialog.Builder(this).setTitle("Saved profiles · on this device").setItems(names,(d,n)->{
            String name=names[n];
            new AlertDialog.Builder(this).setTitle("@"+name)
                .setPositiveButton("Open",(a,b)->navigate(BASE+"/"+name+"/"))
                .setNeutralButton("Remove saved profile",(a,b)->{
                    Set<String> saved=savedProfiles();saved.remove(name);
                    prefs.edit().putStringSet("savedProfiles",new HashSet<>(saved)).apply();showHome();
                }).setNegativeButton("Cancel",null).show();
        }).setPositiveButton("Add a profile",(d,w)->visitProfile()).setNegativeButton("Close",null).show();
    }
    private void settingsFile(boolean export) {
        String message=export
            ? "Exports your mode, boundaries and saved usernames as a readable file. It excludes Instagram login, messages and usage history. Choose a location you trust; cloud document providers may upload it."
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
            prefs.getInt("sessionMinutes",5),prefs.getInt("dailyMinutes",15),savedProfiles());
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
                +incoming.profiles.size()+" saved profiles\n\nReplaces current preferences and saved profiles. Existing login, usage counters and cooldown stay unchanged.")
            .setNegativeButton("Cancel",null).setPositiveButton("Replace settings",(d,w)->{
                prefs.edit().putBoolean("focused",incoming.focused).putInt("postLimit",incoming.posts)
                    .putInt("sessionMinutes",incoming.sessionMinutes).putInt("dailyMinutes",incoming.dailyMinutes)
                    .putStringSet("savedProfiles",new HashSet<>(incoming.profiles)).apply();
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
    private TextView text(String s,int size,int color){ TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(6),0,dp(10));return t; }
    private void space(){ Space s=new Space(this);panel.addView(s,new LinearLayout.LayoutParams(1,dp(18))); }
    private void card(String title,String body){ LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(18),dp(12),dp(18),dp(12));GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(dp(18));c.setBackground(bg);c.addView(text(title,12,ACCENT));c.addView(text(body,16,INK));panel.addView(c);space(); }
    private void button(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(BG);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));b.setOnClickListener(v->action.run());parent.addView(b,new LinearLayout.LayoutParams(-1,dp(54)));}
    private void addNav(LinearLayout nav,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);b.setPadding(0,0,0,0);b.setTextColor(INK);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(CARD));b.setOnClickListener(v->action.run());nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}
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
