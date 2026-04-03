package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Handy utility methods for gathering statistics from lists of notes.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsUtil {

    private StatisticsUtil() {
    }

    /**
     * For the given Note, examines the text and returns the count of words within it.
     */
    public static int countWords(Note note) {
        if (note == null || note.getText() == null || note.getText().isBlank()) {
            return 0; // easy check
        }

        // Replace everything that isn't a letter, number, or apostrophe with a space, then split on whitespace:
        // (we include apostrophes so that words like "isn't" don't get split into "isn t").
        String[] words = note.getText()
                             .replaceAll("[^\\w']", " ") // keep letters, numbers, apostrophes; turn rest into spaces
                             .trim() // remove leading/trailing spaces
                             .split("\\s+"); // split on one or more whitespace characters

        return words.length;
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
     * Given a list of notes, finds the most common phrases across all of them and
     * returns the top N as a list of Phrase objects. Longer phrases are prioritized
     * over shorter phrases, given the same occurrence count. Note that the returned
     * phrases will be listed in descending order of occurrence count, and they will
     * be stripped of punctuation and normalized to lowercase
     * (so "Hello world!" and "hello, world" would both count towards the same phrase: "hello world").
     * <p>
     * <b>Note:</b> In rare cases, the normalization described above might
     * cause false positives. For example, the phrase "this is not fun" may
     * appear legitimately several times in the given notes, but it may also appear accidentally
     * across two sentences: "A work of art, this is not. Fun though it may be, it is not art".
     * When that example sentence is normalized, it appears to contain the phrase
     * "this is not fun", even though it doesn't actually appear as a contiguous phrase in the original text.
     * This is probably rare in practice, but just keep in mind that the results
     * returned here may not be 100% accurate.
     * </p>
     * <p>
     * If you wish to only consider a filtered subset of the given list, then use
     * findTopNPhrases(List&lt;Note&gt;, int, Query) instead and pass in your desired query.
     * </p>
     */
    public static List<Phrase> findTopNPhrases(List<Note> notes, int n) {
        return findTopNPhrases(notes, n, null);
    }

    /**
     * Similar to findTopNPhrases(List&lt;Note&gt;, int), but allows you to
     * specify a Query to filter the given list of notes before counting phrases.
     * If the given Query is null, this is equivalent to findTopNPhrases(List&lt;Note&gt;, int).
     */
    public static List<Phrase> findTopNPhrases(List<Note> notes, int n, Query query) {
        // You give me nothing, you get nothing:
        if (notes == null || notes.isEmpty() || n <= 0) {
            return List.of();
        }

        // If a query is provided, filter the notes first:
        if (query != null) {
            notes = query.execute(notes);
        }

        // If our query filtered out everything, I guess we're done here:
        if (notes.isEmpty()) {
            return List.of();
        }

        final int MAX_PHRASE_LENGTH = 10; // very rare we'd get more than 10 words in a common phrase
        Map<String, Integer> phraseCounts = new HashMap<>();
        for (Note note : notes) {

            // We don't care about case for this search, and we should strip out punctuation:
            String text = note.getText().toLowerCase(Locale.ROOT);
            text = text.replaceAll("[^\\w']", " "); // keep apostrophes! "don't", "isn't", etc.

            // Now we can split on whitespace:
            String[] words = text.split("\\s+");

            // We can do this in one pass, using a sliding window approach:
            for (int i = 0; i < words.length; i++) {
                StringBuilder phrase = new StringBuilder(words[i]);
                for (int len = 2; len <= MAX_PHRASE_LENGTH && (i + len - 1) < words.length; len++) {
                    phrase.append(" ").append(words[i + len - 1]);
                    phraseCounts.merge(phrase.toString(), 1, Integer::sum);
                }
            }
        }

        // Filter to only those with count > 1 and convert to an ordered list to get the top N (descending):
        return phraseCounts.entrySet().stream()
                           .filter(entry -> entry.getValue() > 1)
                           .map(entry -> new Phrase(entry.getKey(), entry.getValue()))
                           .sorted((p1, p2) -> {
                               // Sort by count, descending:
                               int cmp = Integer.compare(p2.occurrenceCount(), p1.occurrenceCount());
                               if (cmp != 0) { return cmp; }

                               // In the event of a tie, take the longer phrase first (since it's more specific):
                               return Integer.compare(p2.phrase().length(),
                                                      p1.phrase().length());
                           })
                           .limit(n)
                           .toList();
    }
}
