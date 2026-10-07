package com.gikku.transparentwebview;

import android.graphics.Color;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

@DesignerComponent(
    version = 1,
    description = "Minimal transparent WebView extension for Kodular/App Inventor.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = ""
)
@SimpleObject(external = true)
public class GikkuTransparentWebView extends AndroidNonvisibleComponent {

  private final ComponentContainer container;
  private WebView webView;

  public GikkuTransparentWebView(ComponentContainer container) {
    super(container.$form());
    this.container = container;
  }

  @SimpleFunction(description = "Create a transparent WebView inside an arrangement.")
  public void Create(AndroidViewComponent arrangement) {
    if (webView != null) {
      try {
        ViewGroup oldParent = (ViewGroup) webView.getParent();
        if (oldParent != null) oldParent.removeView(webView);
      } catch (Exception ignored) {}
    }

    webView = new WebView(container.$context());
    WebSettings settings = webView.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(true);
    settings.setAllowContentAccess(true);
    settings.setBuiltInZoomControls(false);
    settings.setDisplayZoomControls(false);

    webView.setBackgroundColor(Color.TRANSPARENT);
    webView.setWebChromeClient(new WebChromeClient());
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        view.setBackgroundColor(Color.TRANSPARENT);
        PageLoaded(url);
      }
    });

    try {
      ViewGroup group = (ViewGroup) arrangement.getView();
      group.addView(webView, new ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
      ));
    } catch (Exception e) {
      Error(e.getMessage() == null ? "Unable to attach WebView" : e.getMessage());
    }
  }

  @SimpleFunction(description = "Load a URL.")
  public void LoadUrl(String url) {
    if (webView != null) webView.loadUrl(url);
  }

  @SimpleFunction(description = "Load an HTML string with a transparent base.")
  public void LoadHtml(String html) {
    if (webView != null) {
      webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
  }

  @SimpleFunction(description = "Load an HTML file from app assets, e.g. speedometer.html.")
  public void LoadAsset(String fileName) {
    if (webView != null) {
      String clean = fileName.startsWith("/") ? fileName.substring(1) : fileName;
      webView.loadUrl("file:///android_asset/" + clean);
    }
  }

  @SimpleFunction(description = "Execute JavaScript in the WebView.")
  public void EvaluateJavaScript(String code) {
    if (webView != null) {
      if (android.os.Build.VERSION.SDK_INT >= 19) {
        webView.evaluateJavascript(code, null);
      } else {
        webView.loadUrl("javascript:" + code);
      }
    }
  }

  @SimpleFunction(description = "Set the WebView background transparent or white.")
  public void Transparent(boolean enabled) {
    if (webView != null) {
      webView.setBackgroundColor(enabled ? Color.TRANSPARENT : Color.WHITE);
    }
  }

  @SimpleFunction(description = "Call window.setSpeed(speed) in the loaded page.")
  public void SetSpeed(int speed) {
    EvaluateJavaScript("if(window.setSpeed){window.setSpeed(" + speed + ");}");
  }

  @SimpleFunction(description = "Call window.setGear(gear) in the loaded page.")
  public void SetGear(String gear) {
    String safe = gear == null ? "" : gear.replace("\\", "\\\\").replace("'", "\\'");
    EvaluateJavaScript("if(window.setGear){window.setGear('" + safe + "');}");
  }

  @SimpleFunction(description = "Call window.setSpeed(speed, gear) in the loaded page.")
  public void SetSpeedAndGear(int speed, String gear) {
    String safe = gear == null ? "" : gear.replace("\\", "\\\\").replace("'", "\\'");
    EvaluateJavaScript("if(window.setSpeed){window.setSpeed(" + speed + ",'" + safe + "');}");
  }

  @SimpleFunction(description = "Reload the current page.")
  public void Reload() {
    if (webView != null) webView.reload();
  }

  @SimpleFunction(description = "Whether the WebView can navigate back.")
  public boolean CanGoBack() {
    return webView != null && webView.canGoBack();
  }

  @SimpleFunction(description = "Navigate back.")
  public void GoBack() {
    if (webView != null && webView.canGoBack()) webView.goBack();
  }

  @SimpleEvent(description = "Fires when a page finishes loading.")
  public void PageLoaded(String url) {
    EventDispatcher.dispatchEvent(this, "PageLoaded", url);
  }

  @SimpleEvent(description = "Fires when the extension encounters an error.")
  public void Error(String message) {
    EventDispatcher.dispatchEvent(this, "Error", message);
  }
}
