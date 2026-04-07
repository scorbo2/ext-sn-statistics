package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handy utility methods for gathering statistics from lists of notes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsUtil {

    public static final int MIN_PHRASE_LENGTH = 2; // a phrase must be at least 2 words long to be interesting
    public static final int MAX_PHRASE_LENGTH = 10; // very rare we'd get more than 10 words in a common phrase
    public static final int MIN_PHRASE_FREQUENCY = 2; // single-occurrence phrases are not interesting and are very expensive to store
    public static final int MIN_WORD_LENGTH = 5; // short words are very common and not interesting: a, an, the, that...
    public static final int TOP_N_WORDS = 25; // Completely arbitrary choice: top 25 words.

    /**
     * ISO-8601 defines Monday as the first day of the week (1) and Sunday as the last day of the week (7).
     * But that's stupid. Weeks start with Sunday, and end with Saturday (at least here in North America).
     * So, let's map from Java's implementation to our own:
     */
    public static final DayOfWeek[] DAYS_OF_WEEK = new DayOfWeek[]{
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
    };

    private StatisticsUtil() {
    }

    /**
     * For the given text, counts the number of words it contains and returns that count as an integer.
     */
    public static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0; // easy check
        }

        // Replace everything that isn't a letter, number, or apostrophe with a space, then split on whitespace:
        // (we include apostrophes so that words like "isn't" don't get split into "isn t").
        String normalizedText = text
                .replaceAll("[^\\w']", " ") // keep letters, numbers, apostrophes; turn rest into spaces
                .trim(); // remove leading/trailing spaces

        // If the normalization left us with nothing, then we have zero words:
        // (this can happen for punctuation-only text like "!!!")
        if (normalizedText.isBlank()) {
            return 0;
        }

        // Now we can split on whitespace and count the words:
        String[] words = normalizedText.split("\\s+");
        return words.length;
    }

    /**
     * For the given Note, examines the text and returns the count of words within it.
     */
    public static int countWords(Note note) {
        if (note == null || note.getText() == null || note.getText().isBlank()) {
            return 0; // easy check
        }

        return countWords(note.getText());
    }

    /**
     * Given a list of notes, returns the total word count across all of them.
     * If you only want to consider a filtered subset of the given list, then
     * use countWords(List&lt;Note&gt;, Query) instead and pass in your desired query.
     */
    public static int countWords(List<Note> notes) {
        return countWords(notes, null);
    }

    /**
     * Given a list of notes and an optional filtering Query, counts all words
     * across all notes that match the given Query. If the query is null,
     * this is equivalent to countWords(List&lt;Note&gt;).
     */
    public static int countWords(List<Note> notes, Query query) {
        if (notes == null || notes.isEmpty()) {
            return 0;
        }

        // If a query is provided, filter the notes first:
        if (query != null) {
            notes = query.execute(notes);
        }

        // If our query filtered out everything, I guess we're done here:
        if (notes.isEmpty()) {
            return 0;
        }

        // Sum the word counts for all notes:
        return notes.stream()
                    .mapToInt(StatisticsUtil::countWords)
                    .sum();
    }

    /**
     * Returns a WordList containing the top N most common words across the given list of notes,
     * along with their occurrence counts.
     */
    public static WordList findWords(List<Note> notes, int N) {
        return findWords(notes, null, N);
    }

    /**
     * Returns a WordList containing the top N most common words across the given list of notes,
     * only considering the given specific words, and ignoring all others. You can pass null
     * or an empty set for specificWords to consider all words, in which case, this is
     * equivalent to findWords(List&lt;Note&gt;, int).
     * <p>
     * Note: MIN_WORD_LENGTH is ignored if you pass in specificWords to search for!
     * The unique word count feature will still work, though.
     * </p>
     */
    public static WordList findWords(List<Note> notes, Set<String> specificWords, int N) {
        if (notes == null || notes.isEmpty() || N <= 0) {
            return new WordList(null); // default to empty list if null or invalid N
        }
        if (specificWords == null) {
            specificWords = Set.of(); // default to empty set if null
        }

        Map<String, Integer> wordCounts = new HashMap<>();
        Set<String> uniqueWords = new HashSet<>(); // tracks total unique words scanned, for the uniqueWordCount feature
        for (Note note : notes) {

            // Cache getText() to avoid repeated virtual calls:
            String noteText = note == null ? null : note.getText();

            // Skip null notes, and notes with no text:
            if (noteText == null || noteText.isBlank()) {
                continue;
            }

            // Normalize the text and ensure we have something left to work with after normalization:
            String text = normalizeText(noteText);
            if (text.isBlank()) {
                continue;
            }

            // Now we can split on whitespace:
            String[] words = text.split("\\s+");
            for (String word : words) {
                uniqueWords.add(word); // track this word, even if it's a short one.

                // Are we only considering specific words?
                if (!specificWords.isEmpty()) {
                    if (specificWords.contains(word)) {
                        wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    }
                    continue; // skip the rest of this loop
                }

                // We are considering all words. But, we should
                // skip short words when counting occurrences, since they are very
                // common and not interesting: a, an, the, but, etc.
                if (word.length() < MIN_WORD_LENGTH) {
                    continue;
                }
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        // Prune all single-occurrence words before materializing into a list.
        // In large datasets, they are the overwhelming majority of map entries,
        // which may cause an OutOfMemoryException during .toList().
        // Note: we re-use MIN_PHRASE_FREQUENCY here. It serves the same purpose.
        // Also note: we don't do this prune if we were given specific words to search for.
        // That's because our list will be MUCH smaller, and we want to count even single occurrences:
        if (specificWords.isEmpty()) {
            wordCounts.values().removeIf(count -> count < MIN_PHRASE_FREQUENCY);
        }

        // Convert to WordList and return, sorted by count descending:
        return new WordList(wordCounts.entrySet()
                                      .stream()
                                      .map(entry -> new Word(entry.getKey(), entry.getValue()))
                                      .sorted((w1, w2) -> Integer.compare(w2.occurrenceCount(),
                                                                          w1.occurrenceCount())) // sort by count, descending
                                      .limit(N)
                                      .toList(),
                            uniqueWords.size()); // total unique words scanned, for the uniqueWordCount feature
    }

    /**
     * Given a list of notes, finds all phrases of length 2 to MAX_PHRASE_LENGTH that appear
     * in the text of those notes, and counts how many times each phrase appears.
     * <p>
     * If you wish to only consider a filtered subset of the given list, then use
     * findPhrases(List&lt;Note&gt;, Query) instead and pass in your desired query.
     * </p>
     */
    public static PhraseList findPhrases(List<Note> notes) {
        return findPhrases(notes, null);
    }

    /**
     * Similar to findPhrases(List&lt;Note&gt;), but allows you to
     * specify a Query to filter the given list of notes before counting phrases.
     * If the given Query is null, this is equivalent to findPhrases(List&lt;Note&gt;).
     */
    public static PhraseList findPhrases(List<Note> notes, Query query) {
        // You give me nothing, you get nothing:
        if (notes == null || notes.isEmpty()) {
            return new PhraseList(null); // default to empty list if null
        }

        // If a query is provided, filter the notes first:
        if (query != null) {
            notes = query.execute(notes);
        }

        // If our query filtered out everything, I guess we're done here:
        if (notes.isEmpty()) {
            return new PhraseList(null);
        }

        // int[] stores { wordCount, frequency } per phrase, so we don't have to re-parse
        // the phrase string to count words at the end (we already know the count from `len`).
        Map<String, int[]> phraseCounts = new HashMap<>();
        for (Note note : notes) {

            // Cache getText() to avoid repeated virtual calls:
            String noteText = note == null ? null : note.getText();

            // Skip null notes, and notes with no text:
            if (noteText == null || noteText.isBlank()) {
                continue;
            }

            // Normalize the text and ensure we have something left to work with after normalization:
            String text = normalizeText(noteText);
            if (text.isBlank()) {
                continue;
            }

            // Now we can split on whitespace:
            String[] words = text.split("\\s+");

            // We can do this in one pass, using a sliding window approach:
            for (int i = 0; i < words.length; i++) {
                StringBuilder phrase = new StringBuilder(words[i]);
                for (int len = 2; len <= MAX_PHRASE_LENGTH && (i + len - 1) < words.length; len++) {
                    phrase.append(' ').append(words[i + len - 1]);
                    String key = phrase.toString();
                    // Explicit get/put avoids lambda allocation on every inner-loop iteration:
                    int[] counts = phraseCounts.get(key);
                    if (counts == null) {
                        phraseCounts.put(key, new int[]{len, 1});
                    }
                    else {
                        counts[1]++;
                    }
                }
            }
        }

        // Prune all single-occurrence phrases before materializing into a list.
        // In large datasets, they are the overwhelming majority
        // of map entries, which may cause an OutOfMemoryException during .toList().
        phraseCounts.values().removeIf(v -> v[1] < MIN_PHRASE_FREQUENCY);

        // Convert to PhraseList and return.
        // Word count comes directly from the stored int[0], not re-parsed from the string:
        return new PhraseList(phraseCounts
                                      .entrySet()
                                      .stream()
                                      .map(entry -> new Phrase(entry.getKey(),
                                                               entry.getValue()[0],
                                                               entry.getValue()[1]))
                                      .toList());
    }

    /**
     * Performs a single-pass normalization of the given text, converting it to lowercase and
     * replacing all non-word characters (except apostrophes) with spaces. This replaces the
     * old two-pass approach of using toLowerCase() followed by replaceAll(), which required
     * two full scans of the text and incurred regex overhead.
     *
     * @param text Any text string.
     * @return A normalized version of the given text. Might be empty if normalization stripped everything.
     */
    public static String normalizeText(String text) {
        if (text == null || text.isEmpty()) {
            return ""; // easy check
        }

        int len = text.length();
        StringBuilder normalized = new StringBuilder(len);
        for (int ci = 0; ci < len; ci++) {
            char c = text.charAt(ci);
            if (Character.isLetterOrDigit(c) || c == '\'') {
                normalized.append(Character.toLowerCase(c));
            }
            else {
                normalized.append(' ');
            }
        }
        return normalized.toString().trim();
    }

    /**
     * We have to guard against very small values being incorrectly rounded down to 0,
     * because our ValueCell treats 0 as a special case meaning "no data". So, we'll
     * put a very small floor in place if the given value is non-zero but would
     * otherwise be normalized to a value very close to 0.
     */
    public static float normalizeValue(int value, int minValue, int maxValue) {
        if (value == 0) {
            return 0f; // preserve the "0 means no data" contract
        }
        if (maxValue <= minValue) {
            return 1f; // non-zero value with no valid range: use full intensity safely
        }

        float normalized = (float) (value - minValue) / (maxValue - minValue);
        if (normalized < 0.05f) {
            return 0.05f; // enforce a minimum color intensity for non-zero values
        }
        return normalized;
    }
}
