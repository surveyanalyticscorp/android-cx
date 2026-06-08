# QuestionPro CX SDK — Android

## Overview

The QuestionPro CX SDK (`cxlib`) enables Android apps to display targeted surveys based on configurable intercept rules. The host app initialises the SDK once; the SDK handles all rule evaluation, survey triggering, and display autonomously.

> **Note:** This repository contains two modules — `cxlib` and `app`. All development, bug fixes, and feature work must target the **`cxlib`** module only. The `app` module is a demo e-commerce application used solely to test changes made in `cxlib`; it is not part of the SDK deliverable and should not be modified as part of SDK work.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Integration](#integration)
3. [Initialisation](#initialisation)
4. [TouchPoint Configuration](#touchpoint-configuration)
5. [Intercept Rules](#intercept-rules)
6. [Intercept Settings](#intercept-settings)
7. [Data Mapping](#data-mapping)
8. [Public API Reference](#public-api-reference)
9. [Callback Interfaces](#callback-interfaces)
10. [Enums & Constants](#enums--constants)
11. [SDK Flow](#sdk-flow)
12. [Demo App Integration](#demo-app-integration)

---

## Architecture

```
cxlib/
├── QuestionProCX.java              # Singleton — main SDK entry point
├── CXApiHandler.java               # HTTP client (intercept config, survey URL, feedback)
├── CXGlobalInfo.java               # Global payload/header builder; data mapping logic
├── CXConstants.java                # API endpoints and data-center base URLs
├── MonitorAppEvents.java           # Rule event monitoring (timers, view counts)
├── ActivityLifecycleCallbacks.java # App foreground/background detection
├── ActivityLifecycleManager.java   # Per-activity start/stop tracking
├── InteractionActivity.java        # WebView activity that renders surveys
├── SharedPreferenceManager.java    # Persistence layer (intercepts, visitor UUID, counts)
├── model/
│   ├── TouchPoint.java             # SDK configuration (Builder pattern)
│   ├── Intercept.java              # Single intercept with rules and settings
│   ├── InterceptRule.java          # Individual rule definition
│   ├── InterceptSettings.java      # Per-intercept behaviour settings
│   ├── DataMapping.java            # Survey variable ↔ custom data mapping
│   └── InterceptMetadata.java      # Visitor match/launch/exclude tracking
├── interfaces/
│   ├── IQuestionProInitCallback.java
│   ├── IQuestionProCallback.java
│   ├── IQuestionProApiCallback.java
│   └── IQuestionProRulesCallback.java
└── enums/
    ├── DataCenter.java
    ├── Platform.java
    ├── VisitorStatus.java
    └── ConfigType.java
```

---

## Integration

### Gradle dependency

Add the `cxlib` module to your project and declare it as a dependency in your app's `build.gradle`:

```groovy
implementation project(':cxlib')
```

### AndroidManifest — API key

The SDK reads the API key from the app's manifest. Add the following inside `<application>`:

```xml
<meta-data
    android:name="com.questionpro.cxlib.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

Alternatively, the API key can be supplied programmatically via `TouchPoint.Builder`.

---

## Initialisation

Call `init()` once from your `Application` class (or the first `Activity`). The SDK fetches intercept rules from the server, caches them locally, and begins monitoring rules immediately on success.

```java
TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US)
        .build();

QuestionProCX.getInstance().init(
        this,            // Application or Activity context
        touchPoint,
        new IQuestionProInitCallback() {
            @Override
            public void onInitializationSuccess(String message) {
                // SDK ready; intercept rules loaded
            }

            @Override
            public void onInitializationFailure(String error) {
                // Handle error (network issue, invalid key, etc.)
            }
        }
);
```

---

## TouchPoint Configuration

`TouchPoint` is built with a fluent Builder and passed to `init()`.

| Builder method | Type | Required | Description |
|---|---|---|---|
| `Builder(DataCenter)` | — | Yes | Base constructor; sets the data center |
| `Builder(DataCenter, String apiKey)` | — | Yes* | Pass API key programmatically instead of via manifest |
| `.setPlatform(Platform)` | `Platform` | No | Defaults to `ANDROID` |
| `.build()` | — | Yes | Constructs the `TouchPoint` object |

**Example with explicit API key:**

```java
TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.EU, "your-api-key")
        .setPlatform(Platform.ANDROID)
        .build();
```

---

## Intercept Rules

After `init()` succeeds the SDK iterates the received intercept list. For each intercept it evaluates its rules. When the rules are satisfied the SDK triggers the associated survey.

### Rule types

#### 1. TIME_SPENT

Fires after the user has been in the app for a configured number of seconds. The SDK starts a background timer on session start; when the timer expires the rule is marked satisfied.

```
intercept rule: name = "TIME_SPENT", value = "30"  →  fire after 30 s
```

#### 2. VIEW_COUNT

The host app reports screen visits by calling `setScreenVisited(tagName)`. The SDK compares the tag against every `VIEW_COUNT` rule across all intercepts. When the cumulative count for that tag reaches the configured threshold the rule is satisfied.

```java
// Call this whenever a screen is visited
QuestionProCX.getInstance().setScreenVisited("product_detail");
```

```
intercept rule: name = "VIEW_COUNT", key = "product_detail", value = "3"
→ fire on the 3rd visit to "product_detail"
```

View counts are persisted across sessions and reset per intercept after a survey launches.

#### 3. DAY

Checked immediately on SDK setup. The rule carries a target day of the week (e.g. `"MONDAY"`). If today's day matches, the rule is satisfied.

```
intercept rule: name = "DAY", value = "FRIDAY"  →  fires only on Fridays
```

#### 4. DATE

Same as DAY but compared against a specific calendar date.

```
intercept rule: name = "DATE", value = "2024-12-25"  →  fires on that date
```

### Rule combination (`condition`)

Each intercept carries a `condition` field that controls how its rules are combined:

| Value | Behaviour |
|---|---|
| `AND` | All rules must be satisfied before the survey fires |
| `OR` | Any single rule being satisfied triggers the survey |

---

## Intercept Settings

Each intercept carries an `InterceptSettings` object that controls survey behaviour after a rule fires.

| Field | Type | Description |
|---|---|---|
| `allowMultipleResponse` | `boolean` | If `false` (default), the survey is shown only once per device. If `true`, it re-shows on every rule match. |
| `triggerDelayInSeconds` | `int` | Seconds to wait after a rule is satisfied before displaying the survey. |
| `samplingRate` | `int` | Percentage (0–100) of matched visitors who actually see the survey. Others are marked `EXCLUDED` and reported to the server. |
| `autoLanguageSelection` | `boolean` | When `true`, the device locale is sent with the survey request so the survey is displayed in the user's language. |
| `autoCloseOnCompletion` | `boolean` | When `true`, the survey WebView closes automatically after the user submits a response. |

---

## Data Mapping

Data mapping lets you pass custom context variables (e.g. user ID, account type) into the survey response, so results can be segmented by that data on the QuestionPro dashboard.

### Step 1 — Store custom data in the SDK

Call `setDataMappings()` with a `HashMap` where the **key** is the variable display name configured in the intercept settings on the server, and the **value** is the data you want to attach.

```java
HashMap<String, String> customData = new HashMap<>();
customData.put("userName", "Jane Doe");
customData.put("accountType", "premium");
customData.put("userId", "usr_98765");

QuestionProCX.getInstance().setDataMappings(customData);
```

`setDataMappings()` can be called at any time before or after `init()`. Values are persisted in SharedPreferences and sent when any survey is launched.

### Step 2 — Server-side configuration

In the QuestionPro intercept settings, create data-mapping entries that pair:
- **Variable** — the survey question variable name
- **Display Name** — the key you used in the `HashMap` above

### How it works internally

When the SDK builds the survey URL request payload it:

1. Retrieves the custom data map from SharedPreferences.
2. Iterates each `DataMapping` entry in the intercept.
3. Looks up `displayName` (case-insensitive) in the custom data map.
4. Appends `{ variableName, value }` pairs to the `data` array of the API request.

```
Custom data:   { "userName": "Jane Doe" }
DataMapping:   { variable: "q1_name", displayName: "userName" }
API payload:   { data: [{ variableName: "q1_name", value: "Jane Doe" }] }
```

---

## Public API Reference

All methods are accessed via the singleton: `QuestionProCX.getInstance()`

### `init()`

```java
void init(Context context, TouchPoint touchPoint, IQuestionProInitCallback callback)
```

Initialises the SDK. Fetches intercept configuration from the server and begins rule monitoring. Must be called before any other SDK method.

| Parameter | Description |
|---|---|
| `context` | Application or Activity context |
| `touchPoint` | SDK configuration object |
| `callback` | Success/failure callback |

---

### `setScreenVisited()`

```java
void setScreenVisited(String tagName)
```

Reports a screen visit to the SDK. Triggers evaluation of all `VIEW_COUNT` rules. Call this in `onResume()` of any screen that is tracked by an intercept rule.

| Parameter | Description |
|---|---|
| `tagName` | Tag string matching the `key` field of the intercept rule (case-sensitive) |

---

### `setDataMappings()`

```java
void setDataMappings(HashMap<String, String> customDataMappings)
```

Stores custom data to be attached to survey responses via data mapping. Can be called at any time; values are persisted across sessions.

---

### `getSurveyUrl()`

```java
void getSurveyUrl(IQuestionProCallback callback)
```

Fetches the survey URL for the currently active intercept and returns it via callback. Useful when the host app wants to embed or open the survey manually.

---

### `closeSurveyWindow()`

```java
void closeSurveyWindow()
```

Programmatically closes the active survey WebView (`InteractionActivity`).

---

### `onStart()` / `onStop()`

```java
void onStart(Activity activity)
void onStop(Activity activity)
```

Activity lifecycle hooks. Register these in your base `Activity` (or each `Activity`) to help the SDK distinguish foreground from background state.

---

### `clearSession()`

```java
void clearSession()
```

Resets all in-memory state and SharedPreferences. Stops all active timers. Called automatically when the app moves to the background.

---

## Callback Interfaces

### `IQuestionProInitCallback`

```java
public interface IQuestionProInitCallback {
    void onInitializationSuccess(String message);
    void onInitializationFailure(String error);
}
```

Returned by `init()`. `onInitializationSuccess` is called once intercept rules are successfully fetched and cached. `onInitializationFailure` is called on network error or invalid API key.

---

### `IQuestionProCallback`

```java
public interface IQuestionProCallback {
    void getSurveyUrl(String surveyUrl);
}
```

Used with `getSurveyUrl()` to receive the resolved survey URL.

---

### `IQuestionProApiCallback` _(internal)_

```java
void onApiCallbackSuccess(Intercept intercept, String surveyUrl);
void OnApiCallbackFailed(JSONObject error);
```

Internal interface implemented by `QuestionProCX` to receive API responses from `CXApiHandler`.

---

### `IQuestionProRulesCallback` _(internal)_

```java
void onTimeSpendSatisfied(int interceptId);
void onViewCountRuleSatisfied(int interceptId);
```

Internal interface implemented by `QuestionProCX`; called by `MonitorAppEvents` when a rule fires.

---

## Enums & Constants

### `DataCenter`

| Value | Region |
|---|---|
| `US` | United States |
| `EU` | Europe |
| `CA` | Canada |
| `SG` | Singapore |
| `AU` | Australia |
| `AE` | UAE |
| `SA` | South Africa |
| `KSA` | Saudi Arabia |

### `Platform`

| Value | Description |
|---|---|
| `ANDROID` | Native Android (default) |
| `IOS` | iOS (for cross-platform wrappers) |
| `FLUTTER` | Flutter |
| `REACT_NATIVE` | React Native |

### `VisitorStatus`

| Value | Meaning |
|---|---|
| `MATCHED` | Rules were satisfied; visitor eligible for survey |
| `LAUNCHED` | Survey was actually shown to visitor |
| `EXCLUDED` | Visitor was excluded by sampling rate |

---

## SDK Flow

```
Host App
    │
    ▼
init(context, touchPoint, callback)
    │
    ▼
CXApiHandler.getIntercept()   ──────────► QuestionPro API
    │                                      (GET /api/v1/visitor/mobile)
    ▼
Intercept JSON received & cached in SharedPreferences
    │
    ▼
setUpIntercept() — iterate each intercept:
    ├── Check samplingRate → excluded?  ──► excludedFeedback() ──► EXCLUDED
    ├── Start TIME_SPENT timer (Handler.postDelayed)
    ├── Evaluate DAY rule
    └── Evaluate DATE rule
    │
    │  (ongoing — during app session)
    ▼
setScreenVisited(tagName)
    └── MonitorAppEvents.setTagNameCheckRules()
            └── increment view count in SharedPreferences
                └── count >= threshold? → onViewCountRuleSatisfied(interceptId)

TIME_SPENT timer fires → onTimeSpendSatisfied(interceptId)
    │
    ▼
checkAllRulesForIntercept(interceptId)
    ├── condition = OR  → any rule satisfied? → proceed
    └── condition = AND → all rules satisfied? → proceed
    │
    ▼
shouldSurveyLaunch()
    ├── allowMultipleResponse = false & already launched? → skip
    └── pass → submitFeedback(MATCHED)
    │
    ▼
launchFeedbackSurvey(intercept)
    ├── triggerDelayInSeconds > 0 → Handler.postDelayed(delay)
    └── on trigger:
            ├── type = SURVEY_URL → getInterceptSurvey() → open URL
            └── type = PROMPT / EMBED → start InteractionActivity
                                            └── WebView loads survey
    │
    ▼
submitFeedback(LAUNCHED)
    │
    ▼ (autoCloseOnCompletion)
Survey WebView closes
```

---

## Demo App Integration

The `app` module is a sample e-commerce app used to test `cxlib` changes.

### SDK Initialisation — `AppController.java`

```java
public class AppController extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US).build();

        QuestionProCX.getInstance().init(this, touchPoint,
                new IQuestionProInitCallback() {
                    @Override
                    public void onInitializationSuccess(String message) {
                        Log.d("CX", "SDK initialized: " + message);
                    }

                    @Override
                    public void onInitializationFailure(String error) {
                        Log.e("CX", "SDK init failed: " + error);
                    }
                });
    }
}
```

### Screen Tracking — `ShoppingCartActivity.java`

```java
@Override
protected void onResume() {
    super.onResume();
    QuestionProCX.getInstance().setScreenVisited("vistaSurvey");
    QuestionProCX.getInstance().setScreenVisited("CierreSesion");
}
```

### Retrieving Survey URL manually — `ECartHomeActivity.java`

```java
QuestionProCX.getInstance().getSurveyUrl(new IQuestionProCallback() {
    @Override
    public void getSurveyUrl(String surveyUrl) {
        // Open surveyUrl in a browser or custom WebView
    }
});
```
