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
4. `StatisticsDialog` renders 6 tabs: Overview, Years, Months, Weeks, Phrases, Words — each backed by custom heatmap chart components in the `charts/` subpackage

**Key classes:**
- `StatisticsUtil` — static analysis algorithms (word counting, phrase extraction, top-word ranking, value normalization)
- `StatisticsLoaderThread` — background worker; extends `SimpleProgressWorker` from the Snotes framework
- `Statistics` — data bag with final fields passed from loader to dialog
- `PhraseList` — wraps `List<Phrase>` with a `filter(n, minPhraseLength)` method that sorts by frequency (tiebreaker: longer phrase wins)
- `Phrase` — Java `record`; immutable value object
- `WordList` — wraps `List<Word>` and also tracks the total count of unique words scanned across all notes (`getUniqueWordCount()`)
- `Word` — Java `record`; immutable value object holding a word and its occurrence count
- `WordSearchThread` — on-demand background worker (extends `SimpleProgressWorker`) that calls `StatisticsUtil.findWords()` for a user-supplied set of specific words

**Chart components** (`charts/` package):
- `ValueCell` — single 24×24px heatmap cell; blends between configurable cold/hot colors
- `AllYearsChart`, `YearChart`, `WeekChart` — compose `ValueCell` instances to render heatmap-based views of statistics

## Key Conventions

**Phrase analysis constants** (in `StatisticsUtil`):
- Phrases are 2–10 words (`MIN_PHRASE_LENGTH`, `MAX_PHRASE_LENGTH`)
- Phrases appearing only once are discarded (`MIN_PHRASE_FREQUENCY = 2`)
- Text is normalized to lowercase with punctuation stripped, **except** apostrophes are preserved

**Word analysis constants** (in `StatisticsUtil`):
- Words shorter than 5 characters are skipped during general top-word collection (`MIN_WORD_LENGTH = 5`), to exclude noise like "a", "the", "but"
- `MIN_WORD_LENGTH` is **ignored** when searching for specific words (via the word search feature)
- The top-word list is capped at 25 entries (`TOP_N_WORDS = 25`)
- Single-occurrence words are pruned before materializing to a list (re-uses `MIN_PHRASE_FREQUENCY`)
- `StatisticsUtil.findWords(List<Note>, int)` returns a `WordList` for the general top-N use case
- `StatisticsUtil.findWords(List<Note>, Set<String>, int)` accepts a set of specific words to look up; `WordList.getUniqueWordCount()` is populated by both overloads

**Words tab (in `StatisticsDialog`):**
- Displays the top 25 words and the total number of unique words scanned across all notes
- Provides an interactive word search: the user enters a comma-separated list of words, `WordSearchThread` runs off the Swing EDT, and results are shown in a dialog
- Word search input is parsed via `TagList.fromRawString()` for convenient comma-separated, de-duplicated, normalised tokenisation

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
