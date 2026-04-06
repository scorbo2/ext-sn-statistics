# Copilot Instructions for ext-sn-statistics

A plugin for the [Snotes](https://github.com/scorbo2/snotes) application that analyzes a user's notes and presents word/phrase/note statistics through an interactive heatmap-based UI.

## Build & Test

```bash
# Build
mvn clean install

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=StatisticsUtilTest

# Run a single test method
mvn test -Dtest=StatisticsUtilTest#findPhrases_shouldIgnoreSingleOccurrencePhrases
```

Java 17. No lint plugin — formatting is enforced via `.editorconfig` (4-space indent, 120-char line limit, LF line endings, UTF-8).

## Architecture

The extension follows the Snotes plugin pattern: `StatisticsExtension` extends `SnotesExtension`, loads metadata from `extInfo.json`, and registers a UI action that opens `StatisticsDialog`.

**Data flow:**
1. User triggers the Statistics action → `StatisticsLoaderThread` starts
2. Loader runs **6 parallel worker tasks** via `ExecutorService`: general counts, per-year, all-months, per-year+month, day-of-week, and phrase extraction
3. Results collected into the `Statistics` model
4. `StatisticsDialog` renders 5 tabs: Overview, Years, Months, Weeks, Phrases — each backed by custom heatmap chart components in the `charts/` subpackage

**Key classes:**
- `StatisticsUtil` — static analysis algorithms (word counting, phrase extraction, value normalization)
- `StatisticsLoaderThread` — background worker; extends `SimpleProgressWorker` from the Snotes framework
- `Statistics` — data bag with final fields passed from loader to dialog
- `PhraseList` — wraps `List<Phrase>` with a `filter(n, minPhraseLength)` method that sorts by frequency (tiebreaker: longer phrase wins)
- `Phrase` — Java `record`; immutable value object

**Chart components** (`charts/` package):
- `ValueCell` — single 24×24px heatmap cell; blends between configurable cold/hot colors
- `AllYearsChart`, `YearChart`, `WeekChart` — compose `ValueCell` instances to render heatmap-based views of statistics

## Key Conventions

**Phrase analysis constants** (in `StatisticsUtil`):
- Phrases are 2–10 words (`MIN_PHRASE_LENGTH`, `MAX_PHRASE_LENGTH`)
- Phrases appearing only once are discarded (`MIN_PHRASE_FREQUENCY = 2`)
- Text is normalized to lowercase with punctuation stripped, **except** apostrophes are preserved

**Performance-sensitive paths** in `StatisticsUtil.findPhrases()`:
- Uses a single-pass char-by-char loop (not regex) for normalization
- Uses explicit `HashMap.get()`/`put()` instead of lambdas to avoid allocation in tight loops
- Prunes single-occurrence phrases from the map *before* materializing to a list

**Heatmap color scale:** configurable cold/hot gradient stored as extension properties (`Statistics.Options.coldColor` / `Statistics.Options.hotColor`). Year charts are given a shared min/max so all year tabs use a consistent scale.

**Day-of-week:** uses North American convention (Sunday = 1), not ISO-8601.

**Tests** follow Arrange-Act-Assert with descriptive names like `filter_withTiedOccurrenceCount_shouldPrioritizeLongerPhrase`. Test classes live in the same package as the class under test.

**JavaDoc** is expected on all public classes and methods. Every class carries:
```java
@author <a href="https://github.com/scorbo2">scorbo2</a>
@since Snotes 2.0
```
