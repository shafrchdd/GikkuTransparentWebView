# Gikku Android share / open-with setup

The extension can READ incoming intents, but an Android Activity must REGISTER intent filters in the final APK manifest. Kodular extensions alone do not reliably modify Screen1's activity intent filters. Apply these filters to the exported APK's launcher Activity using an APK build pipeline that supports manifest merging (or a custom native wrapper).

```xml
<intent-filter>
  <action android:name="android.intent.action.SEND" />
  <category android:name="android.intent.category.DEFAULT" />
  <data android:mimeType="text/plain" />
</intent-filter>
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data android:scheme="https" android:host="www.google.com" android:pathPrefix="/maps" />
  <data android:scheme="https" android:host="maps.google.com" />
</intent-filter>
```

Call `ReadIncomingIntent` after the app screen initializes. Listen for `DestinationReceived` and present a Start Navigation confirmation before `CalculateRoute`.

**Limitations:** Android may prefer verified link owners and not show Gikku for all links. `maps.app.goo.gl` short links are NOT resolved by this initial parser; add a safe redirect resolver before supporting them. Never auto-start navigation on untrusted shared text. The parsing implementation is in `GikkuSharedLocationParser.java`.
