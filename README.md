# GikkuTransparentWebView

Minimal transparent WebView extension for Kodular / App Inventor.

## Main blocks
- Create(Arrangement)
- LoadUrl(url)
- LoadHtml(html)
- LoadAsset(fileName)
- Transparent(boolean)
- EvaluateJavaScript(code)
- SetSpeed(speed)
- SetGear(gear)
- SetSpeedAndGear(speed, gear)
- Reload()
- CanGoBack()
- GoBack()

## Speedometer
Your HTML can expose:

```js
window.setSpeed = setSpeed;
window.setGear = setGear;
```

Then Kodular can call `SetSpeedAndGear(72, "4")`.


Build trigger: corrected AIX workflow.
