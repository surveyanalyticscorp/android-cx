# QuestionPro CX Android SDK

An Android SDK for embedding QuestionPro Customer Experience (CX) surveys into your app. The SDK fetches intercept configurations from the QuestionPro backend, evaluates trigger rules, and launches surveys automatically when conditions are met.

---

## Requirements

| | Minimum |
|---|---|
| Android API | 21 (Android 5.0) |
| Java | 8+ (default methods on interfaces) |
| Kotlin | Compatible |

---

## Installation

### Maven (JitPack)

In your root `settings.gradle`:
```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

In your app's `build.gradle`:
```groovy
dependencies {
    implementation 'com.github.surveyanalyticscorp:android-cx:1.2.8'
}
```

---

## Manifest Setup

Add internet permission:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Optionally, set your API key in the manifest (can be overridden via `TouchPoint.Builder`):
```xml
<application ...>
    <meta-data
        android:name="cx_manifest_api_key"
        android:value="YOUR_API_KEY" />
</application>
```


---

## Initialization

Initialize once in your `Application.onCreate()` or entry `Activity.onCreate()`:

```java
TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US)
        .setApiKey("YOUR_API_KEY")   // optional if set in manifest
        .setPlatform(Platform.ANDROID)
        .build();

QuestionProCX.getInstance().init(
        this,
        touchPoint,
        new IQuestionProInitCallback() {

            @Override
            public void onInitializationSuccess(String message) {
                // SDK is ready — start tracking screens
            }

            @Override
            public void onInitializationFailure(String error) {
                Log.e("CX", "Init failed: " + error);
            }

            @Override
            public void onError(int interceptId, String errorMessage) {
                // Runtime SDK error (e.g. survey failed to load).
                // Forward to your crash reporting tool here.
                // Default is a no-op — override only if you need visibility.
                FirebaseCrashlytics.getInstance()
                        .log("CX SDK error [intercept=" + interceptId + "]: " + errorMessage);
            }
        }
);
```

---

## Tracking Screens (VIEW_COUNT rules)

Call `setScreenVisited()` when the user navigates to a screen. The tag name must match the rule key configured in the QuestionPro dashboard:

```java
@Override
protected void onResume() {
    super.onResume();
    QuestionProCX.getInstance().setScreenVisited("tag_name");
}
```

---

## Custom Data Mappings

Pass app-specific data to be merged into the survey response:

```java
HashMap<String, String> data = new HashMap<>();
data.put("user_id_key", "user_12345");
data.put("plan_key", "premium");
QuestionProCX.getInstance().setDataMappings(data);
```

Key names must match the `displayName` of the data mappings configured for the intercept in the dashboard.

---

## Survey URL Type (SURVEY_URL intercepts)

For intercepts of type `SURVEY_URL`, the SDK returns the resolved URL to your app instead of launching its own UI:

```java
QuestionProCX.getInstance().getSurveyUrl(new IQuestionProCallback() {
    @Override
    public void getSurveyUrl(String surveyUrl) {
        // Open in your own WebView, browser, or custom UI
    }
});
```

---

## Intercept Types

| Type | Behaviour |
|---|---|
| `PROMPT` | Survey displayed as a dialog overlay (configurable size and position) |
| `EMBED` | Survey displayed fullscreen |
| `SURVEY_URL` | SDK resolves the URL and returns it to the host app via `IQuestionProCallback` |

---

## Trigger Rules

Rules are configured in the QuestionPro dashboard and evaluated automatically by the SDK.

| Rule | When it fires |
|---|---|
| `TIME_SPENT` | App has been in the foreground for N seconds |
| `VIEW_COUNT` | A tracked screen tag has been visited N times |
| `DAY` | Current day of the week matches (e.g. `Monday,Friday`) |
| `DATE` | Current day of the month matches (e.g. `1,15`) |

**Conditions:**
- `AND` — all rules must be satisfied before the survey launches
- `OR` — any single rule fires the survey immediately

---

## Sampling

The SDK respects the `samplingRate` (0–100%) configured per intercept:

- `100` — show to every eligible visitor
- `50` — show to approximately 50% of visitors

The sampling decision is computed once per session using server-provided `matchedCount` / `excludedCount` and cached locally. Excluded visitors are reported to the backend automatically.

---

## Closing the Survey Programmatically

```java
QuestionProCX.getInstance().closeSurveyWindow();
```

---

## Error Handling

The SDK fails silently toward the end user — no error dialogs are shown to the user. All errors are:

1. **Logged to Logcat** (tags: `CXApiHandler`, `InteractionActivity`) — visible during development
2. **Reported via `onError`** in `IQuestionProInitCallback` — forward to your crash reporting tool in production

| Scenario | Behaviour |
|---|---|
| No internet connection | `onError` called, survey skipped silently |
| API returns 4xx | `onError` called with server error message |
| API returns non-JSON error body | `onError` called with generic message |
| Network timeout / SSL error | `onError` called with exception type |
| Survey URL missing in response | `onError` called, activity finishes silently |

---

## Testing Error Handling

**Option 1 — Turn off network**
After init (settings are cached), disable Wi-Fi and mobile data, then navigate to the screen that triggers the rules. `onError` is called with `"No internet connection."`.

**Option 2 — Wrong API key**
Pass an invalid key via `TouchPoint.Builder`. The server returns 4xx → `onError` fires during the survey URL fetch.

**Option 3 — Direct call (debug builds only)**
```java
QuestionProCX.getInstance().getInitCallback().onError(999, "Manual test error");
```

---

## Data Centers

| `DataCenter` | Region |
|---|---|
| `US` | United States |
| `EU` | Europe |
| `CA` | Canada |
| `SG` | Singapore |
| `AU` | Australia |
| `AE` | UAE |
| `SA` | Saudi Arabia (SurveyAnalytics) |
| `KSA` | Saudi Arabia (QuestionPro) |

---

## Callback Reference

### `IQuestionProInitCallback`

```java
void onInitializationSuccess(String message);

void onInitializationFailure(String error);

// No-op default — override to receive runtime errors
default void onError(int interceptId, String errorMessage) {}
```

### `IQuestionProCallback` (SURVEY_URL type only)

```java
void getSurveyUrl(String surveyUrl);
```

---

## Cross-Platform Support

Set the `Platform` in `TouchPoint` to match your host app framework:

```java
TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US)
        .setPlatform(Platform.FLUTTER)      // or REACT_NATIVE, ANDROID
        .build();
```

Flutter and React Native hosts automatically get the correct `runningActivities` offset.

---

## ProGuard / R8

```
-keep class com.questionpro.cxlib.** { *; }
-keep interface com.questionpro.cxlib.** { *; }
-keepclassmembers class com.questionpro.cxlib.model.** { *; }
```
