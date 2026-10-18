package com.pnbpaws.resume.mobile;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.util.Base64;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 10, SAVE_FILE = 11;
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private WebView web;
    private LinearLayout root;
    private String edition;
    private LinearLayout toolbar;
    private LinearLayout versionChoices;
    private int toolbarMode = -1;
    private ValueCallback<Uri[]> upload;
    private byte[] pendingDownload;
    private TextView error;
    private ProgressBar progress;
    private boolean pageFailed;
    private final Handler introHandler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 249, 252));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int available = right - left - root.getPaddingLeft() - root.getPaddingRight();
            if (available > 0) adaptChrome(available);
        });
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
            android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::back);
        if (isDevelopmentBuild() && getIntent().getBooleanExtra("localPreview", false)) openEdition(EditionPolicy.LOCAL);
        else showWelcome(true);
        if(isDevelopmentBuild() && getIntent().getBooleanExtra("pinShortcut",false)) {
            var manager=getSystemService(android.content.pm.ShortcutManager.class);
            if(manager.isRequestPinShortcutSupported()) {
                Intent launch=new Intent(this,MainActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                var shortcut=new android.content.pm.ShortcutInfo.Builder(this,"pnb-local-preview")
                    .setShortLabel("PNB Resume Prospective").setLongLabel("PNB Resume Prospective")
                    .setIcon(android.graphics.drawable.Icon.createWithResource(this,R.mipmap.ic_launcher)).setIntent(launch).build();
                manager.requestPinShortcut(shortcut,null);
            }
        }
    }

    private boolean isDevelopmentBuild() {
        return (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
    private boolean trusted(String url) { return EditionPolicy.trusted(url, edition, isDevelopmentBuild()); }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String label, int size) {
        TextView view = new TextView(this);
        view.setText(label); view.setTag(size); view.setTextSize(size); view.setTextColor(Color.rgb(19, 47, 88));
        view.setPadding(dp(12), dp(12), dp(12), dp(12));
        return view;
    }
    private Button button(String label, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false);
        button.setOnClickListener(view -> action.run()); return button;
    }
    private void disposeWeb() {
        introHandler.removeCallbacksAndMessages(null);
        if (upload != null) { upload.onReceiveValue(null); upload = null; }
        if (web != null) { root.removeView(web); web.stopLoading(); web.destroy(); web = null; }
    }
    private void showVersions() { showWelcome(false); }
    private void showWelcome(boolean intro) {
        disposeWeb(); root.removeAllViews(); edition=null; toolbar=null; versionChoices=null;
        web=new WebView(this);
        root.addView(web,new LinearLayout.LayoutParams(-1,-1));
        web.setBackgroundColor(Color.WHITE);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setTextZoom(100);
        var assets=new androidx.webkit.WebViewAssetLoader.Builder()
            .addPathHandler("/assets/",new androidx.webkit.WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                return assets.shouldInterceptRequest(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) {
                if(!request.isForMainFrame())return true;
                Uri uri=request.getUrl();
                if("welcome.pnb.invalid".equals(uri.getHost())) {
                    switch(uri.getPath()) {
                        case "/coach": openEdition(EditionPolicy.COACH); break;
                        case "/individual": openEdition(EditionPolicy.BETA); break;
                        case "/preview": if(isDevelopmentBuild())openEdition(EditionPolicy.LOCAL); break;
                    }
                } else if(request.hasGesture() && ("https".equals(uri.getScheme()) || "mailto".equals(uri.getScheme()))) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,uri)); }
                    catch(ActivityNotFoundException e) { toast("No app can open that link."); }
                }
                return true;
            }
        });
        web.loadUrl("https://appassets.androidplatform.net/assets/welcome/index.html?"+(intro?"intro=1":"skip=1")+(isDevelopmentBuild()?"&debug=1":""));
        WebView introView=web;
        if(intro)introHandler.postDelayed(()->{if(web==introView)web.evaluateJavascript("window.showSelection?.()",null);},5200);
    }

    private void openEdition(String selected) {
        disposeWeb(); root.removeAllViews(); edition = selected;
        versionChoices = null;
        toolbar = new LinearLayout(this);
        toolbarMode = -1;
        root.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        adaptChrome(getResources().getDisplayMetrics().widthPixels);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));
        error = text("", 14); error.setVisibility(View.GONE); root.addView(error);
        web = new WebView(this); root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setUserAgentString(settings.getUserAgentString()+" PNBAndroid/1");
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(true); settings.setBuiltInZoomControls(true); settings.setDisplayZoomControls(false);
        settings.setTextZoom(100); settings.setUseWideViewPort(true); settings.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "PnbDownloads", Collections.singleton(selected), (view, message, origin, mainFrame, reply) -> {
                if (!mainFrame || !trusted(origin.toString()) || !trusted(view.getUrl())) return;
                try {
                    String payload = message.getData();
                    if (payload == null || payload.length() > 28_000_000) throw new IllegalArgumentException();
                    JSONObject item = new JSONObject(payload);
                    String mime = item.getString("type");
                    boolean pdf = "application/pdf".equals(mime);
                    if (!pdf && !DOCX.equals(mime)) throw new IllegalArgumentException();
                    if (pendingDownload != null) { toast("Finish the current save first."); return; }
                    byte[] bytes = Base64.decode(item.getString("base64"), Base64.DEFAULT);
                    if (bytes.length == 0 || bytes.length > 20_000_000) throw new IllegalArgumentException();
                    pendingDownload = bytes;
                    Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(mime).putExtra(Intent.EXTRA_TITLE, EditionPolicy.safeFilename(item.optString("name", "Resume"), pdf));
                    startActivityForResult(save, SAVE_FILE);
                } catch (Exception e) { pendingDownload = null; toast("The file could not be saved. Please try again."); }
            });
        } else toast("Update Android System WebView to enable resume downloads.");
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int percent) { progress.setProgress(percent); progress.setVisibility(percent < 100 ? View.VISIBLE : View.GONE); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (!trusted(view.getUrl())) return false;
                if (upload != null) upload.onReceiveValue(null);
                upload = callback;
                try { startActivityForResult(params.createIntent(), PICK_FILE); }
                catch (ActivityNotFoundException e) { upload.onReceiveValue(null); upload = null; toast("No file picker is available."); }
                return true;
            }
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                String url = request.getUrl().toString();
                if (trusted(url)) return false;
                if (request.hasGesture() && Arrays.asList("https", "mailto", "tel").contains(request.getUrl().getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl())); }
                    catch (ActivityNotFoundException e) { toast("No app can open that link."); }
                }
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) { pageFailed = false; error.setVisibility(View.GONE); }
            @Override public void onPageFinished(WebView view, String url) {
                if (pageFailed || !trusted(url)) return;
                CookieManager.getInstance().flush();
                view.evaluateJavascript("document.documentElement.dataset.nativeWebview=\"android\"",null);
                view.evaluateJavascript(DOWNLOAD_SCRIPT, null);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError problem) {
                if (request.isForMainFrame()) showError("Unable to connect. Check your internet connection, then tap Reload.");
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) showError("This version is temporarily unavailable. Please try Reload shortly.");
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, android.net.http.SslError problem) {
                handler.cancel(); showError("A secure connection could not be established. Please try again later.");
            }
        });
        web.setDownloadListener((url, agent, disposition, type, length) -> {
            if (web == null || !trusted(web.getUrl()) || !url.startsWith("blob:" + edition + "/")) return;
            String name = URLUtil.guessFileName(url, disposition, type);
            web.evaluateJavascript("(async () => { try { const blob = await (await fetch(" + JSONObject.quote(url)
                + ")).blob(); const reader = new FileReader(); reader.onload = () => window.PnbDownloads.postMessage(JSON.stringify({name:"
                + JSONObject.quote(name) + ",type:blob.type,base64:reader.result.split(',')[1]})); reader.readAsDataURL(blob); }"
                + " catch { alert('Please try the download again.'); } })();", null);
        });
        web.loadUrl(EditionPolicy.LOCAL.equals(selected) ? selected + "/?walkthrough=sample" : selected);
    }

    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        refreshTextScale(root);
        toolbarMode = -1;
        if (web != null) { web.requestLayout(); web.invalidate(); }
        root.requestLayout();
        root.post(() -> adaptChrome(root.getWidth() - root.getPaddingLeft() - root.getPaddingRight()));
    }
    private void refreshTextScale(View view) {
        if (view instanceof TextView && view.getTag() instanceof Integer)
            ((TextView) view).setTextSize((Integer) view.getTag());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) refreshTextScale(group.getChildAt(i));
        }
    }
    private void adaptChrome(int availableWidth) {
        if (availableWidth <= 0) return;
        if (versionChoices != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) versionChoices.getLayoutParams();
            int target = Math.min(availableWidth, dp(640));
            if (params.width != target) { params.width = target; versionChoices.setLayoutParams(params); }
            int padding = availableWidth < dp(360) ? dp(10) : dp(22);
            versionChoices.setPadding(padding, dp(24), padding, dp(24));
        }
        if (toolbar == null) return;
        boolean compact = false;
        int mode = compact ? 1 : 0;
        if (toolbarMode == mode) return;
        toolbarMode = mode;
        toolbar.removeAllViews();
        toolbar.setOrientation(compact ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        LinearLayout actions = compact ? new LinearLayout(this) : toolbar;
        Button versions = button("← Versions", this::showVersions);
        Button reload = button("Reload", () -> { if (web != null) web.reload(); });
        if (compact) {
            actions.addView(versions, new LinearLayout.LayoutParams(0, -2, 1));
            actions.addView(reload, new LinearLayout.LayoutParams(0, -2, 1));
            toolbar.addView(actions, new LinearLayout.LayoutParams(-1, -2));
        } else actions.addView(versions);
        TextView title = text(EditionPolicy.LOCAL.equals(edition) ? "Local Preview" : EditionPolicy.BETA.equals(edition) ? "Individual" : "Coach and Mentor", 16);
        toolbar.addView(title, new LinearLayout.LayoutParams(compact ? -1 : 0, -2, compact ? 0 : 1));
        if (!compact) actions.addView(reload);
    }
    private void showError(String message) { pageFailed = true; error.setText(message); error.setVisibility(View.VISIBLE); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void back() {
        if (web == null) { finish(); return; }
        web.evaluateJavascript("window.pnbAppBack ? window.pnbAppBack() : false", result -> {
            if (!"true".equals(result)) showVersions();
        });
    }
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { back(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && upload != null) {
            upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data)); upload = null;
        }
        if (request == SAVE_FILE) {
            byte[] bytes = pendingDownload; pendingDownload = null;
            if (result != RESULT_OK || data == null || bytes == null) return;
            Uri destination = data.getData();
            new Thread(() -> {
                try (OutputStream out = getContentResolver().openOutputStream(destination)) {
                    if (out == null) throw new java.io.IOException();
                    out.write(bytes); runOnUiThread(() -> toast("Resume saved."));
                } catch (Exception e) { runOnUiThread(() -> toast("Could not save the file. Please download it again.")); }
            }).start();
        }
    }
    @Override protected void onPause() { CookieManager.getInstance().flush(); super.onPause(); }
    @Override protected void onDestroy() { pendingDownload = null; disposeWeb(); super.onDestroy(); }
    private static final String DOWNLOAD_SCRIPT = """
        (() => {
          if (window.pnbDownloadReady || !window.PnbDownloads) return;
          window.pnbDownloadReady = true;
          document.addEventListener('click', async (event) => {
            const link = event.target.closest?.('a[download]');
            if (!link || !link.href.startsWith('blob:')) return;
            event.preventDefault();
            try {
              const response = await fetch(link.href);
              const blob = await response.blob();
              const reader = new FileReader();
              reader.onload = () => window.PnbDownloads.postMessage(JSON.stringify({
                name: link.download || 'Resume', type: blob.type,
                base64: reader.result.split(',')[1]
              }));
              reader.readAsDataURL(blob);
            } catch { alert('Download failed. Please try again.'); }
          }, true);
        })();
        """;
}
