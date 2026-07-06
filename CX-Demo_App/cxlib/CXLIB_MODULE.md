# CXLib Module Documentation

## Overview

`cxlib` is an Android library module for QuestionPro's Customer Experience (CX) SDK. It connects to the QuestionPro backend to fetch intercept configurations, evaluates trigger rules, and launches surveys in a WebView when criteria are met.

**Package:** `com.questionpro.cxlib`

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Package Structure](#package-structure)
3. [Initialization Flow](#initialization-flow)
4. [Public API Surface](#public-api-surface)
5. [Core Classes](#core-classes)
6. [Data Models](#data-models)
7. [Enumerations](#enumerations)
8. [Interfaces & Callbacks](#interfaces--callbacks)
9. [API Endpoints](#api-endpoints)
10. [Request & Response Formats](#request--response-formats)
11. [Rule Evaluation Logic](#rule-evaluation-logic)
12. [Survey Launch Flow](#survey-launch-flow)
13. [Threading Model](#threading-model)
14. [Configuration Constants](#configuration-constants)
15. [Data Flow Diagram](#data-flow-diagram)

---

## Architecture Overview

```
Host App
   │
   ▼
QuestionProCX (Singleton)        ← Main entry point
   │
   ├── CXGlobalInfo              ← Configuration & headers
   ├── CXApiHandler              ← HTTP API calls
   ├── SharedPreferenceManager   ← Local persistence
   ├── MonitorAppEvents          ← Rule timers & counters
   ├── ActivityLifecycleCallbacks← Foreground/background detection
   └── InteractionActivity       ← Survey WebView UI
```

---

## Package Structure

| Package | Purpose |
|---|---|
| `com.questionpro.cxlib` | Core classes: SDK entry point, API handler, config, lifecycle |
| `com.questionpro.cxlib.model` | Data models: Intercept, Rule, WidgetSettings, TouchPoint, etc. |
| `com.questionpro.cxlib.enums` | Enumerations: DataCenter, Platform, InterceptType, etc. |
| `com.questionpro.cxlib.interfaces` | Public and internal callback interfaces |
| `com.questionpro.cxlib.dataconnect` | HTTP client, response wrapper, payload builder |
| `com.questionpro.cxlib.util` | Date/time utilities, device ID, network helpers |
| `com.questionpro.cxlib.interaction` | WebView activity and Chrome client |

---

## Initialization Flow

```
Host App
  │
  ▼
QuestionProCX.getInstance().init(context, touchPoint, callback)
  │
  ├── Registers ActivityLifecycleCallbacks with Application
  ├── Reads API key from AndroidManifest meta-data: cx_manifest_api_key
  ├── Stores TouchPoint in CXGlobalInfo
  ├── Gets or generates device UUID via CXUtils.getUniqueDeviceId()
  │
  └── CXApiHandler.getIntercept()
        │
        ├── Background GET → /api/v1/visitor/mobile
        ├── Parses JSON response: intercepts list + visitor UUID
        ├── Saves to SharedPreferenceManager
        │
        └── QuestionProCX.setUpIntercept()
              ├── Registers TIME_SPENT timers in MonitorAppEvents
              ├── Checks DAY / DATE rules immediately
              └── Callback: IQuestionProInitCallback.onInitializationSuccess()
```

**Re-fetch on App Foreground:**
Every time the app transitions from background to foreground (`runningActivities` counter goes from 0 to 1), `fetchInterceptSettings()` is called again after a 500 ms delay to refresh intercept configs.

---

## Public API Surface

```java
// 1. Initialize the SDK
QuestionProCX.getInstance().init(
    Context context,
    TouchPoint touchPoint,
    IQuestionProInitCallback callback
);

// 2. Track screen or tag visits (triggers VIEW_COUNT rules)
QuestionProCX.getInstance().setScreenVisited(String tagName);

// 3. Pass custom data mappings (merged into survey URL payload)
QuestionProCX.getInstance().setDataMappings(HashMap<String, String> customDataMappings);

// 4. Retrieve a survey URL directly (non-intercept flow)
QuestionProCX.getInstance().getSurveyUrl(IQuestionProCallback callback);

// 5. Programmatically close the survey WebView
QuestionProCX.getInstance().closeSurveyWindow();

// 6. Clean up SDK state
QuestionProCX.getInstance().cleanup();

// 7. Activity lifecycle hooks (call from host Activity)
QuestionProCX.getInstance().onStart(Activity activity);
QuestionProCX.getInstance().onStop(Activity activity);
```

**TouchPoint Builder:**
```java
TouchPoint touchPoint = new TouchPoint.Builder(DataCenter.US)
    .setApiKey("your-api-key")
    .setPlatform(Platform.ANDROID)
    .build();
```

---

## Core Classes

### `QuestionProCX` (Singleton)

Main SDK orchestrator. Coordinates initialization, rule evaluation, sampling checks, and survey launch.

**Key responsibilities:**
- Registers `ActivityLifecycleCallbacks` to detect foreground/background transitions
- Maintains `interceptSatisfiedRules: HashMap<Integer, Set<String>>` — tracks which rules have fired per intercept ID
- Evaluates AND/OR rule conditions when a rule fires via `checkAllRulesForIntercept()`
- Performs sampling check (`checkShouldShowSampling()`) before launching survey
- Delegates API calls to `CXApiHandler`
- Tracks `runningActivities: AtomicInteger` to detect foreground state

**Key methods:**

| Method | Description |
|---|---|
| `init()` | Initialize SDK, register lifecycle, fetch intercepts |
| `fetchInterceptSettings()` | Call API to refresh intercept configs |
| `setUpIntercept()` | Register rule timers/counters after fetch |
| `checkAllRulesForIntercept(interceptId)` | Evaluate if all/any rules satisfied |
| `checkShouldShowSampling(intercept)` | Sampling rate gate before launch |
| `launchFeedbackSurvey(intercept)` | Trigger delay, then fetch survey URL and launch |
| `setScreenVisited(tagName)` | Delegate to `MonitorAppEvents` for VIEW_COUNT |
| `setDataMappings(map)` | Store custom data in `SharedPreferenceManager` |

---

### `CXApiHandler`

Executes all HTTP calls on a background `ExecutorService` thread; posts results back on the main thread via `Handler(Looper.getMainLooper())`.

| Method | HTTP | Endpoint | Purpose |
|---|---|---|---|
| `getIntercept()` | GET | `/api/v1/visitor/mobile` | Fetch all intercept configs |
| `getInterceptSurvey(intercept)` | POST | `/api/v1/data-mapping/mobile/survey-url` | Get survey URL for an intercept |
| `getSurvey(surveyId)` | GET | `/a/api/v2/surveys/{surveyId}` | Get standalone survey URL |
| `submitFeedback()` | POST | `/api/v1/visitor/mobile/survey-feedback` | Record MATCHED/LAUNCHED/EXCLUDED |
| `excludedFeedback()` | POST | `/api/v1/visitor/mobile/excluded-feedback` | Record excluded visitor |
| `logError(message, httpStatus, path, exception, errorType)` | POST | `/api/v1/error-logs/mobile` | Fire-and-forget error report to backend |

---

### `CXGlobalInfo` (Singleton)

Centralized config holder. Stores API key, visitor UUID, `TouchPoint`, and custom data. Provides header and payload building methods used by `CXApiHandler`.

**Key data:**
- `apiKey` — from `TouchPoint` or manifest
- `visitorId` (UUID) — device-specific unique ID
- `dataCenter` — selected data center
- `customDataMappings: HashMap<String, String>` — set by host app

---

### `SharedPreferenceManager` (Singleton)

Persists SDK state between app launches.

**Stored data:**

| Key | Type | Content |
|---|---|---|
| Intercept project | JSON String | Full intercepts list |
| Visitor UUID | String | Unique visitor ID |
| View count per tag | Int | Per-tag visit counter |
| Launched surveys | JSON Map | interceptId → launch timestamp |
| Custom data mappings | JSON Map | App-provided variable values |

Two `SharedPreferences` instances:
- **Main prefs** — general SDK state
- **Persisted prefs** — cross-session data (launched surveys, visitor UUID)

---

### `MonitorAppEvents` (Singleton)

Manages time-based and count-based rule triggers.

**TIME_SPENT rule:**
- Schedules `Handler.postDelayed(runnable, seconds * 1000L)` for each intercept
- On fire: calls `IQuestionProRulesCallback.onTimeSpendSatisfied(interceptId)`
- Stored in `runnableMap: HashMap<Integer, Runnable>` for cancellation

**VIEW_COUNT rule:**
- `setTagNameCheckRules(tagName)` increments the counter for that tag name
- Checks if counter >= rule threshold
- Calls `IQuestionProRulesCallback.onViewCountRuleSatisfied(interceptId)`

---

### `ActivityLifecycleCallbacks`

Monitors `Application.ActivityLifecycleCallbacks`.

- Increments `runningActivities` on `onActivityStarted()`; triggers fetch on 0→1 transition
- Decrements on `onActivityStopped()`; clears session on 1→0 transition (all activities stopped)
- Clears view counts, timers, and session flags on background transition

---

### `InteractionActivity` (FragmentActivity)

Hosts the survey WebView. Supports two layout modes:

| Mode | Description |
|---|---|
| **PROMPT** | Dialog overlay, configurable size (% of screen) and position |
| **FULLSCREEN** | Full-screen activity |

**Widget customization from `WidgetSettings`:**
- Background color, text color, icon color
- Window height/width as percentage of screen
- Vertical position (TOP/BOTTOM) and horizontal position (LEFT/CENTER/RIGHT)
- Widget title text

**Survey lifecycle:**
- Loads survey URL in `WebView`
- Tracks load completion via `MyWebChromeClient.onProgressChanged()`
- Auto-closes when URL contains `#autoClose` (if `autoCloseOnCompletion = true`)
- Records launch timestamp on open

---

### `ActivityLifecycleManager`

Tracks activity sessions with START/STOP events.

- 10-second timeout to detect if a new session has started vs. resuming
- Detects crashes (START without STOP in persisted queue)
- Uses `PersistentSessionQueue` (backed by `SharedPreferencesPersistentSessionQueue`) for persistence

---

## Data Models

### `Intercept` (Serializable)

Root model for an intercept configuration.

| Field | Type | Description |
|---|---|---|
| `id` | int | Unique intercept ID |
| `type` | String | `PROMPT`, `EMBED`, or `SURVEY_URL` |
| `surveyId` | int | Associated survey ID |
| `ruleGroupId` | int | Rule group for feedback submission |
| `condition` | String | `AND` or `OR` |
| `settings` | InterceptSettings | Behavior configuration |
| `rules` | `ArrayList<InterceptRule>` | Trigger rules |
| `dataMappings` | `ArrayList<DataMapping>` | Variable → app data key mappings |
| `widgetSettings` | WidgetSettings | UI customization |
| `metaData` | InterceptMetadata | Sampling counters and visitor status |

---

### `InterceptRule` (Serializable)

A single trigger rule within an intercept.

| Field | Description |
|---|---|
| `name` | Rule type: `TIME_SPENT`, `VIEW_COUNT`, `DAY`, `DATE` |
| `key` | Tag name (for VIEW_COUNT) or unused |
| `value` | Threshold value or comma-separated list (e.g., `"Monday,Friday"`, `"1,15"`) |
| `operand` | Comparison operator (e.g., `>=`) |
| `rangeValue` | Optional upper bound for range rules |
| `variable` | Variable reference |
| `type` | Rule type enum string |

---

### `InterceptSettings` (Serializable)

| Field | Default | Description |
|---|---|---|
| `allowMultipleResponse` | false | Allow re-triggering after first launch |
| `autoLanguageSelection` | false | Include device language in survey URL payload |
| `triggerDelayInSeconds` | 0 | Delay before showing survey after rule fires |
| `autoCloseOnCompletion` | false | Auto-close WebView on survey completion |
| `samplingRate` | 100 | Percentage of visitors who see the survey |

---

### `WidgetSettings` (Serializable)

| Field | Description |
|---|---|
| `textColor` | Hex color string for text |
| `iconColor` | Hex color string for icon |
| `backgroundColor` | Hex color string for background |
| `widgetTitle` | Display title |
| `position` | Combined position string, e.g., `"BOTTOM_RIGHT"` |
| `verticalPosition` | Derived: `TOP` or `BOTTOM` |
| `horizontalPosition` | Derived: `LEFT`, `CENTER`, or `RIGHT` |
| `widgetWindowHeight` | Window height as % of screen |
| `widgetWindowWidth` | Window width as % of screen |

---

### `InterceptMetadata` (Serializable)

| Field | Description |
|---|---|
| `matchedCount` | Total visitors who matched this intercept |
| `excludedCount` | Total visitors excluded by sampling |
| `visitorStatus` | Current visitor's status: `MATCHED`, `LAUNCHED`, or `EXCLUDED` |

---

### `TouchPoint` (Serializable — Builder pattern)

Host-app-provided configuration passed to `init()`.

| Field | Required | Default | Description |
|---|---|---|---|
| `dataCenter` | Yes | — | Target data center (`DataCenter` enum) |
| `apiKey` | No | From manifest | Override API key |
| `platform` | No | `ANDROID` | Platform identifier |
| `transactionDate` | No | Current time | Transaction timestamp |

---

### `DataMapping` (Serializable)

Maps a survey variable name to a host-app data key.

| Field | Description |
|---|---|
| `variable` | Survey variable name (server-side) |
| `displayName` | Key to look up in the host app's custom data map |

---

### `CXInteraction` (Serializable)

Lightweight model passed to `InteractionActivity` for generic survey launch.

| Field | Description |
|---|---|
| `url` | Survey URL to load |
| `isDialog` | True for dialog/prompt mode |
| `themeColor` | Theme color hex string |

---

## Enumerations

| Enum | Values |
|---|---|
| `DataCenter` | `US`, `EU`, `CA`, `SG`, `AU`, `AE`, `SA`, `KSA` |
| `Platform` | `ANDROID`, `IOS`, `FLUTTER`, `REACT_NATIVE` |
| `VisitorStatus` | `MATCHED`, `LAUNCHED`, `EXCLUDED` |
| `InterceptType` | `PROMPT`, `EMBED`, `SURVEY_URL` |
| `InterceptRuleType` | `TIME_SPENT`, `VIEW_COUNT`, `DAY`, `DATE` |
| `InterceptCondition` | `AND`, `OR` |
| `ConfigType` | `SURVEY`, `INTERCEPT` |

---

## Interfaces & Callbacks

### Public Interfaces

**`IQuestionProInitCallback`**
```java
void onInitializationSuccess(String message);
void onInitializationFailure(String error);
```

**`IQuestionProCallback`**
```java
void getSurveyUrl(String surveyUrl);
```

### Internal Interfaces

**`IQuestionProApiCallback`**
```java
void onApiCallbackSuccess(Intercept intercept, String surveyUrl);
void OnApiCallbackFailed(JSONObject error);
```

**`IQuestionProRulesCallback`**
```java
void onTimeSpendSatisfied(int interceptId);
void onViewCountRuleSatisfied(int interceptId);
```

**`PersistentSessionQueue`**
```java
void addEvents(SessionEvent... events);
void deleteEvents(SessionEvent... events);
void deleteAllEvents();
List<SessionEvent> getAllEvents();
```

---

## API Endpoints

### Base URLs

| Data Center | Base URL |
|---|---|
| US (staging hardcoded) | `https://cx-intercept-staging-api.questionpro.com` |
| EU | `https://api.questionpro.eu` |
| CA | `https://api.questionpro.ca` |
| SG | `https://api.questionpro.sg` |
| AU | `https://api.questionpro.au` |
| AE | `https://api.questionpro.ae` |
| SA | `https://api.surveyanalytics.com` |
| KSA | `https://api.questionprosa.com` |

> **Note:** `CXConstants` currently has US endpoints hardcoded to the staging environment.

### Endpoint Summary

| Endpoint | Method | Purpose |
|---|---|---|
| `/api/v1/visitor/mobile` | GET | Fetch all intercept configurations |
| `/api/v1/data-mapping/mobile/survey-url` | POST | Get survey URL for a matched intercept |
| `/api/v1/visitor/mobile/survey-feedback` | POST | Submit MATCHED/LAUNCHED/EXCLUDED status |
| `/api/v1/visitor/mobile/excluded-feedback` | POST | Submit excluded visitor feedback |
| `/a/api/v2/surveys/{surveyId}` | GET | Get standalone survey URL |
| `/api/v1/error-logs/mobile` | POST | Report SDK internal errors for diagnostics |

### Request Headers

**GET Intercepts (`/api/v1/visitor/mobile`):**
```
x-app-key: <apiKey>
visitor-id: <visitorUUID>
package-name: <app package name>
x-platform: ANDROID
x-device-id: <device unique ID>
```

**POST Survey URL (`/api/v1/data-mapping/mobile/survey-url`):**
```
x-app-key: <apiKey>
package-name: <app package name>
Content-Type: application/json
```

**POST Feedback:**
```
x-app-key: <apiKey>
visitor-id: <visitorUUID>
package-name: <app package name>
Content-Type: application/json
```

**POST Error Log (`/api/v1/error-logs/mobile`):**
```
x-app-key: <apiKey>
visitor-id: <visitorUUID>
package-name: <app package name>
Content-Type: application/json
```

Request body:
```json
{
  "message": "Failed to load survey: timeout",
  "httpStatus": 504,
  "path": "/api/v1/data-mapping/mobile/survey-url",
  "stacktrace": "java.net.SocketTimeoutException: timeout\n  at ...",
  "context": {
    "platform": "android",
    "sdkVersion": "2.3.1",
    "errorType": "NetworkError"
  }
}
```

`httpStatus` values: actual HTTP code when available, `0` for no network, `500` for SDK-internal exceptions. `stacktrace` is `"N/A"` when no exception is available.

---

## Request & Response Formats

### GET `/api/v1/visitor/mobile` — Response

```json
{
  "project": {
    "intercepts": [
      {
        "id": 1,
        "type": "PROMPT",
        "surveyId": 123,
        "ruleGroupId": 456,
        "condition": "AND",
        "settings": {
          "allowMultipleResponse": false,
          "autoLanguageSelection": true,
          "triggerDelayInSeconds": 2,
          "samplingRate": 80,
          "autoCloseOnCompletion": true
        },
        "rules": [
          {
            "name": "VIEW_COUNT",
            "key": "home_screen",
            "value": "3",
            "operand": ">="
          },
          {
            "name": "TIME_SPENT",
            "value": "60"
          }
        ],
        "dataMappings": [
          { "variable": "userId", "displayName": "user_id_key" }
        ],
        "widgetSettings": {
          "textColor": "#FFFFFF",
          "iconColor": "#FF0000",
          "backgroundColor": "#000000",
          "widgetTitle": "Feedback",
          "position": "BOTTOM_RIGHT",
          "widgetWindowHeight": 60,
          "widgetWindowWidth": 100
        },
        "metaData": {
          "matchedCount": 10,
          "excludedCount": 2,
          "visitorStatus": "MATCHED"
        }
      }
    ]
  },
  "visitor": {
    "uuid": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

### POST `/api/v1/data-mapping/mobile/survey-url` — Request

```json
{
  "packageName": "com.example.app",
  "visitedUserId": "550e8400-e29b-41d4-a716-446655440000",
  "interceptId": 1,
  "surveyId": 123,
  "surveyLanguage": "en-US",
  "data": [
    { "variableName": "userId", "value": "user_123" }
  ]
}
```

### POST `/api/v1/data-mapping/mobile/survey-url` — Response

```json
{
  "surveyURL": "https://www.questionpro.com/a/TakeSurvey?tt=abc123"
}
```

### POST `/api/v1/visitor/mobile/survey-feedback` — Request

```json
{
  "interceptId": 1,
  "ruleGroupId": 456,
  "surveyId": 123,
  "surveyType": "LAUNCHED"
}
```

### GET `/a/api/v2/surveys/{surveyId}` — Response

```json
{
  "response": {
    "url": "https://www.questionpro.com/a/TakeSurvey?tt=xyz789"
  }
}
```

---

## Rule Evaluation Logic

### Rule Types

| Rule | Trigger Condition | Evaluated By |
|---|---|---|
| `TIME_SPENT` | App has been in foreground for N seconds | `MonitorAppEvents` timer |
| `VIEW_COUNT` | A specific tag has been visited N times | `MonitorAppEvents` counter |
| `DAY` | Current day of week matches (e.g., `"Monday,Friday"`) | Immediate check in `setUpIntercept()` |
| `DATE` | Current day of month matches (e.g., `"1,15"`) | Immediate check in `setUpIntercept()` |

### AND vs. OR Condition

```
Rule fires → add to interceptSatisfiedRules[interceptId]

checkAllRulesForIntercept(interceptId):
  │
  ├── Condition == AND:
  │     satisfiedCount == totalRuleCount? → proceed
  │
  └── Condition == OR:
        any rule satisfied? → proceed immediately
```

### Sampling Check

```
checkShouldShowSampling(intercept):
  │
  ├── visitorStatus already set?
  │     └── EXCLUDED → skip
  │         MATCHED/LAUNCHED → proceed
  │
  └── visitorStatus not set:
        formula: (matchedCount / (matchedCount + excludedCount + 1)) * 100
        result < samplingRate? → proceed
        result >= samplingRate? → submitFeedback(EXCLUDED), skip
```

### Survey Already Launched Check

```
shouldSurveyLaunch(intercept):
  │
  ├── allowMultipleResponse == true → always launch
  └── allowMultipleResponse == false:
        check SharedPreferences launched map
        interceptId present? → skip
        not present? → launch
```

---

## Survey Launch Flow

### Intercept Survey (type = PROMPT / EMBED / SURVEY_URL)

```
launchFeedbackSurvey(intercept)
  │
  ├── delay triggerDelayInSeconds (Handler.postDelayed)
  │
  ├── CXApiHandler.getInterceptSurvey(intercept)
  │     └── POST /api/v1/data-mapping/mobile/survey-url
  │           └── Response: { "surveyURL": "..." }
  │
  ├── Save interceptId + timestamp to SharedPreferences
  ├── submitFeedback(intercept, VisitorStatus.LAUNCHED)
  │     └── POST /api/v1/visitor/mobile/survey-feedback
  │
  └── Launch InteractionActivity
        ├── type == SURVEY_URL → load surveyURL in WebView directly
        ├── type == PROMPT → dialog overlay layout
        └── type == EMBED → fullscreen layout
```

### Standalone Survey (getSurveyUrl)

```
QuestionProCX.getSurveyUrl(callback)
  │
  └── CXApiHandler.getSurvey(surveyId)
        └── GET /a/api/v2/surveys/{surveyId}
              └── Response: { "response": { "url": "..." } }
                    └── callback.getSurveyUrl(url)
```

### Auto-Close

When `autoCloseOnCompletion = true` and the survey URL changes to include `#autoClose` (e.g., after survey submission), `InteractionActivity` calls `finish()` automatically.

---

## Threading Model

| Thread | Used For |
|---|---|
| **Main Thread** | UI updates, WebView, activity lifecycle, `Handler` timers |
| **ExecutorService** | API HTTP calls (one thread per call via `newSingleThreadExecutor()`) |

**Pattern:**
```java
executor.execute(() -> {
    // background: HTTP call via CXUploadClient
    CXHttpResponse response = CXUploadClient.uploadCXApi(...);
    // parse response
    handler.post(() -> {
        // main thread: deliver callback
        callback.onApiCallbackSuccess(...);
    });
});
```

**Thread-safe fields:**
- `isSessionAlive` — `volatile boolean`
- `runningActivities` — `AtomicInteger`
- `runnableMap` in `MonitorAppEvents` — concurrent access guarded by method synchronization

---

## Configuration Constants

**`CXConstants` key values:**

```java
// Staging API base (hardcoded — US staging only)
"https://cx-intercept-staging-api.questionpro.com"

// Paths
INTERCEPTS_PATH        = "/api/v1/visitor/mobile"
INTERCEPT_SURVEY_PATH  = "/api/v1/data-mapping/mobile/survey-url"
SURVEY_FEEDBACK_PATH   = "/api/v1/visitor/mobile/survey-feedback"
EXCLUDED_FEEDBACK_PATH = "/api/v1/visitor/mobile/excluded-feedback"
CORE_SURVEY_PATH       = "/a/api/v2/surveys/{id}"

// JSON field keys
STATUS, RESPONSE, PROJECT, VISITOR, CX_SURVEY_URL,
CORE_SURVEY_URL, IS_DIALOG, THEME_COLOR, ID, MESSAGE

// Manifest meta-data key
"cx_manifest_api_key"
```

**HTTP Client (`CXUploadClient`):**
- Connect timeout: 30 seconds
- Socket (read) timeout: 30 seconds
- Supports GZIP response decompression (auto-detected from `Content-Encoding` header)

---

## Data Flow Diagram

```
Host App
  │
  │  init(touchPoint)
  ▼
QuestionProCX ──────────────────► CXGlobalInfo
  │                                (stores apiKey, UUID, dataCenter)
  │
  │  GET /api/v1/visitor/mobile
  ▼
CXApiHandler ──────────────────► QuestionPro Backend
  │                               (returns intercepts + visitor UUID)
  ▼
SharedPreferenceManager
  (persists intercepts, UUID, launched survey map)
  │
  ▼
MonitorAppEvents
  (schedules TIME_SPENT timers, registers VIEW_COUNT counters)


              ─── During Session ───

Host App
  │  setScreenVisited("home")
  ▼
MonitorAppEvents
  (increments view count for "home")
  │  VIEW_COUNT rule threshold reached?
  ▼
QuestionProCX.checkAllRulesForIntercept(interceptId)
  │  All/any rules satisfied?
  │  Sampling check passed?
  ▼
CXApiHandler ──────────────────► QuestionPro Backend
  │  POST /api/v1/data-mapping/mobile/survey-url
  │  (returns survey URL)
  ▼
InteractionActivity
  (loads survey URL in WebView)
  │  Survey completed → URL contains #autoClose
  ▼
CXApiHandler ──────────────────► QuestionPro Backend
  POST /api/v1/visitor/mobile/survey-feedback
  { surveyType: "LAUNCHED" }
```

---

## Bridge / Cross-platform Integration

This SDK is consumed as a native layer by the QuestionPro CX **React Native** and **Flutter** wrapper packages. The bridge layer acts as the host app from the Android SDK's perspective — it implements the SDK callbacks and forwards events across the JS/Dart bridge channel.

### What the bridge must implement

#### 1. `IQuestionProInitCallback` — all three methods

```java
// React Native bridge example (Java/Kotlin module)
QuestionProCX.getInstance().init(context, touchPoint, new IQuestionProInitCallback() {

    @Override
    public void onInitializationSuccess(String message) {
        // forward to JS via bridge channel
        sendEvent("onInitializationSuccess", message);
    }

    @Override
    public void onInitializationFailure(String error) {
        sendEvent("onInitializationFailure", error);
    }

    @Override
    public void onError(int interceptId, String errorMessage) {
        // REQUIRED: forward SDK errors to the JS/Dart layer
        // Without this, errors are silently swallowed inside the bridge
        WritableMap params = Arguments.createMap();
        params.putInt("interceptId", interceptId);
        params.putString("errorMessage", errorMessage);
        sendEvent("onError", params);
    }
});
```

**`onError` is the most commonly missed method** because it has a default no-op implementation (so the bridge compiles without it), but skipping it means SDK errors are invisible to the end developer using the RN/Flutter package.

#### 2. Error log queue flushes on `init()`

The SDK queues failed error log POSTs in `SharedPreferences` and flushes them automatically at the start of every `init()` call. The bridge must ensure `init()` is called **once per app session** (not on every JS reload / hot restart) to avoid double-flushing or missing the flush window.

#### 3. `platform` field in `TouchPoint`

The bridge must set the correct platform value so the backend can distinguish traffic sources:

```java
// React Native bridge
touchPoint.setPlatform(Platform.REACT_NATIVE);

// Flutter bridge
touchPoint.setPlatform(Platform.FLUTTER);
```

This also flows into the `context.platform` field of every error log payload sent to `/api/v1/error-logs/mobile`, so backend error dashboards can filter by platform correctly.

#### 4. `context.getPackageName()` resolves correctly

The SDK uses `mContext.getPackageName()` for the `package-name` request header. When called through a bridge, this resolves to the **host app's** package name (not the bridge package), which is the correct behaviour — no special handling needed.

### Error log payload platform field

The `context.platform` field in every error log payload is resolved from `CXGlobalInfo.getInstance().getPlatform()`, which reads the value set on `TouchPoint`. This means error logs from a React Native bridge will carry `"react_native"` and Flutter will carry `"flutter"` — the backend error dashboard can filter by platform without any extra work from the bridge developer.

---

## Notes & Limitations

- The staging API base URL is hardcoded in `CXConstants`. Switching to production requires updating those constants or making them configurable per data center.
- `CXPayloadWorker` class is largely commented out / unused in the current implementation.
- `InteractionFragment` exists as an alternative fragment-based WebView but is not actively used; `InteractionActivity` is the primary UI entry point.
- The library reads the API key from `AndroidManifest.xml` meta-data (`cx_manifest_api_key`) but allows override via `TouchPoint.Builder.setApiKey()`.
- View counts (for VIEW_COUNT rules) are reset when the app goes to background (`onSessionEnd()`).
