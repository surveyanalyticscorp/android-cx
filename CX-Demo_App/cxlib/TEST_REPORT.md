# cxlib Unit Test Report

**Date:** 2026-05-13
**Module:** `cxlib`
**Build variant:** `debugUnitTest`
**Total tests:** 57 | **Passed:** 57 | **Failed:** 0

---

## Test Suites

| Suite | Type | Tests | Status |
|---|---|---|---|
| `ExampleUnitTest` | Pure JUnit | 1 | PASS |
| `WidgetSettingsTest` | Pure JUnit | 13 | PASS |
| `InterceptParsingTest` | Pure JUnit | 14 | PASS |
| `SharedPreferenceManagerTest` | Robolectric | 12 | PASS |
| `QuestionProCXSamplingTest` | Robolectric | 17 | PASS |

---

## ExampleUnitTest
*Sanity test — ensures the test runner itself is functional.*

| # | Test | Assertion |
|---|---|---|
| 1 | `addition_isCorrect` | `2 + 2 == 4` |

---

## WidgetSettingsTest
*Covers `WidgetSettings.fromJSON()` — field parsing, all 9 position combinations, defaults, and edge cases.*
*No Android framework required — runs as pure JVM.*

### Field parsing

| # | Test | Assertion |
|---|---|---|
| 1 | `fromJSON_parsesAllFields` | All six fields parsed from a complete JSON object |

### Position splitting (all 9 combinations)

| # | Test | Input | verticalPosition | horizontalPosition |
|---|---|---|---|---|
| 2 | `fromJSON_position_BOTTOM_RIGHT` | `BOTTOM_RIGHT` | `BOTTOM` | `RIGHT` |
| 3 | `fromJSON_position_BOTTOM_LEFT` | `BOTTOM_LEFT` | `BOTTOM` | `LEFT` |
| 4 | `fromJSON_position_BOTTOM_CENTER` | `BOTTOM_CENTER` | `BOTTOM` | `CENTER` |
| 5 | `fromJSON_position_TOP_RIGHT` | `TOP_RIGHT` | `TOP` | `RIGHT` |
| 6 | `fromJSON_position_TOP_LEFT` | `TOP_LEFT` | `TOP` | `LEFT` |
| 7 | `fromJSON_position_TOP_CENTER` | `TOP_CENTER` | `TOP` | `CENTER` |
| 8 | `fromJSON_position_CENTER_RIGHT` | `CENTER_RIGHT` | `CENTER` | `RIGHT` |
| 9 | `fromJSON_position_CENTER_LEFT` | `CENTER_LEFT` | `CENTER` | `LEFT` |
| 10 | `fromJSON_position_CENTER_CENTER` | `CENTER_CENTER` | `CENTER` | `CENTER` |

### Defaults and edge cases

| # | Test | Assertion |
|---|---|---|
| 11 | `fromJSON_missingFields_usesDefaults` | Missing fields use: `position=BOTTOM_RIGHT`, `height=80`, `width=90`, colors/title="" |
| 12 | `fromJSON_positionWithoutUnderscore_fallsBackToDefault` | `UNKNOWN` (no underscore) falls back to `BOTTOM` / `RIGHT` |
| 13 | `fromJSON_widgetWindowPercentages_boundaryValues` | Values `0` and `100` are accepted as-is |

---

## InterceptParsingTest
*Covers `fromJSON()` for all five model classes: `Intercept`, `InterceptRule`, `InterceptSettings`, `InterceptMetadata`, `DataMapping`.*
*No Android framework required — runs as pure JVM.*

### Intercept.fromJSON

| # | Test | Assertion |
|---|---|---|
| 1 | `intercept_parsesRequiredFields` | `id`, `type`, `condition` parsed correctly |
| 2 | `intercept_missingId_defaultsToZero` | Absent `id` key → `0` |
| 3 | `intercept_missingType_defaultsToEmpty` | Absent `type` key → `""` |
| 4 | `intercept_missingCondition_defaultsToOR` | Absent `condition` key → `"OR"` |
| 5 | `intercept_parsesWidgetSettings_whenPresent` | `widgetSettings` block parsed; position split applied |
| 6 | `intercept_widgetSettingsNull_whenAbsent` | No `widgetSettings` key → field is `null` |
| 7 | `intercept_parsesInterceptRules` | Rules array produces correct `interceptRule` list |

### InterceptRule.fromJSON

| # | Test | Assertion |
|---|---|---|
| 8 | `interceptRule_missingFields_defaultToEmpty` | All seven fields default to `""` when JSON is empty |

### InterceptSettings.fromJSON

| # | Test | Assertion |
|---|---|---|
| 9 | `interceptSettings_defaults` | `allowMultipleResponse=false`, `samplingRate=100`, `triggerDelay=0`, etc. |
| 10 | `interceptSettings_parsesAllFields` | All five fields parsed from a complete JSON object |

### InterceptMetadata.fromJSON

| # | Test | Assertion |
|---|---|---|
| 11 | `interceptMetadata_defaults_whenEmpty` | `matchedCount=0`, `excludedCount=0`, `visitorStatus=null` |
| 12 | `interceptMetadata_parsesAllFields` | `matchedCount`, `excludedCount`, `visitorStatus` parsed correctly |

### DataMapping.fromJSON

| # | Test | Assertion |
|---|---|---|
| 13 | `dataMapping_parsesVariableAndDisplayName` | `variable` and `displayName` parsed from JSON array |
| 14 | `dataMapping_emptyArray_returnsEmptyList` | Empty JSON array → empty `ArrayList` |

---

## SharedPreferenceManagerTest
*Covers the `SharedPreferenceManager` singleton — view counts, survey launch tracking, custom data mappings, UUID persistence, and reset.*
*Uses Robolectric (`@Config(sdk = 33)`) to run Android SharedPreferences on the JVM.*

### View count

| # | Test | Assertion |
|---|---|---|
| 1 | `updateViewCount_incrementsCorrectly` | Successive calls on same tag return 1, 2, 3 |
| 2 | `updateViewCount_differentTags_areIndependent` | Counts for `"home"` and `"cart"` do not affect each other |
| 3 | `updateViewCount_stopsAtMaxCap` | Count never exceeds `MAX_VIEW_COUNT` (10,000) |
| 4 | `resetViewCount_resetsToZero` | After reset, next call returns 1 again |

### Survey launch tracking

| # | Test | Assertion |
|---|---|---|
| 5 | `isSurveyAlreadyLaunched_falseBeforeLaunch` | Returns `false` when intercept has never been saved |
| 6 | `isSurveyAlreadyLaunched_trueAfterSave` | Returns `true` after `saveInterceptIdForLaunchedSurvey` |
| 7 | `isSurveyAlreadyLaunched_differentInterceptsAreIndependent` | Saving id=101 does not mark id=202 as launched |

### Custom data mappings

| # | Test | Assertion |
|---|---|---|
| 8 | `saveAndGetCustomDataMappings_roundTrip` | Saved map is serialized and retrievable as JSON |
| 9 | `saveCustomDataMappings_mergesWithExisting` | Second `save` merges new keys without losing old ones |
| 10 | `saveCustomDataMappings_overwritesExistingKey` | Saving the same key replaces the old value |

### Visitor UUID

| # | Test | Assertion |
|---|---|---|
| 11 | `saveAndGetVisitorUUID_roundTrip` | Saved UUID can be retrieved unchanged |

### Reset

| # | Test | Assertion |
|---|---|---|
| 12 | `resetPreferences_clearsViewCounts` | After reset, view count for any tag starts from 0 |

---

## QuestionProCXSamplingTest
*Covers `QuestionProCX.checkShouldShowSampling()` (protected) and `shouldSurveyLaunch()` (private, accessed via reflection).*
*Uses Robolectric (`@Config(sdk = 33)`); `appContext` injected via reflection to avoid triggering `init()` and its network call.*

### checkShouldShowSampling — visitorStatus branch
*When `visitorStatus` is set, the API has already categorised the visitor; local sampling math is skipped.*

| # | Test | Input | Expected |
|---|---|---|---|
| 1 | `sampling_visitorStatus_excluded_returnsFalse` | `visitorStatus=EXCLUDED`, rate=100 | `false` |
| 2 | `sampling_visitorStatus_matched_returnsTrue` | `visitorStatus=MATCHED`, rate=0 | `true` |
| 3 | `sampling_visitorStatus_launched_returnsTrue` | `visitorStatus=LAUNCHED`, rate=0 | `true` |

### checkShouldShowSampling — rate-based (no visitorStatus)
*Formula: `(matchedCount × 100) / (matchedCount + excludedCount + 1) < samplingRate`*

| # | Test | samplingRate | matchedCount | excludedCount | Math | Expected |
|---|---|---|---|---|---|---|
| 4 | `sampling_rate100_nullVisitorStatus_alwaysIncluded` | 100 | 0 | 0 | short-circuits | `true` |
| 5 | `sampling_rate100_emptyVisitorStatus_alwaysIncluded` | 100 | 0 | 0 | short-circuits | `true` |
| 6 | `sampling_rate100_withHighMatchCount_alwaysIncluded` | 100 | 999 | 1 | short-circuits | `true` |
| 7 | `sampling_rate0_noHistory_excluded` | 0 | 0 | 0 | `0 < 0` → false | `false` |
| 8 | `sampling_rate50_noHistory_included` | 50 | 0 | 0 | `0 < 50` → true | `true` |
| 9 | `sampling_rate50_balancedHistory_included` | 50 | 50 | 50 | `49 < 50` → true | `true` |
| 10 | `sampling_rate50_highMatchRatio_excluded` | 50 | 100 | 0 | `99 < 50` → false | `false` |
| 11 | `sampling_rateBoundary99_noHistory_included` | 99 | 0 | 0 | `0 < 99` → true | `true` |
| 12 | `sampling_rate1_allMatched_excluded` | 1 | 99 | 0 | `99 < 1` → false | `false` |

### shouldSurveyLaunch — via reflection
*Logic: `return allowMultipleResponse || !doesSurveyAlreadyLaunched`*

| # | Test | allowMultipleResponse | Prior launch? | Expected |
|---|---|---|---|---|
| 13 | `shouldLaunch_multipleAllowed_noPriorLaunch_returnsTrue` | `true` | No | `true` |
| 14 | `shouldLaunch_multipleAllowed_withPriorLaunch_returnsTrue` | `true` | Yes | `true` — multiple always allowed |
| 15 | `shouldLaunch_singleResponse_noPriorLaunch_returnsTrue` | `false` | No | `true` |
| 16 | `shouldLaunch_singleResponse_withPriorLaunch_returnsFalse` | `false` | Yes | `false` — blocked |
| 17 | `shouldLaunch_differentInterceptsAreIndependent` | `false` | Yes (id=205) | `true` — id=206 unaffected |

---

## Test Infrastructure Notes

- **Layer 1 (Pure JUnit):** `WidgetSettingsTest`, `InterceptParsingTest` — no Android dependencies, runs on plain JVM. Uses the `org.json:json:20231013` stub.
- **Layer 2 (Robolectric):** `SharedPreferenceManagerTest`, `QuestionProCXSamplingTest` — Android SharedPreferences and singletons on the JVM. Requires `includeAndroidResources = true` in `testOptions`.
- **Layer 3 (Espresso):** Not yet implemented — requires a device or emulator. Planned for `InteractionActivity` UI flows.

### Test isolation
`SharedPreferenceManager` uses two separate stores:

| Store name | Contents | Cleared by `resetPreferences()` |
|---|---|---|
| `questionpro_cx` | View counts, project JSON, custom data mappings | Yes |
| `Intercepts` | Survey launch history, visitor UUID | **No** (persists across sessions by design) |

Both test classes that use Robolectric clear **both** stores in `@Before setUp()` to prevent cross-test contamination.
