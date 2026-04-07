package ca.corbett.snotes.extensions.statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a gathered list of all phrases of two or more words that occur in the analyzed notes,
 * along with their occurrence counts.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class PhraseList {

    private final List<Phrase> phrases;

    /**
     * Constructs a PhraseList with the given list of phrases.
     */
    public PhraseList(List<Phrase> phrases) {
        this.phrases = phrases == null ? List.of() : phrases; // default to empty list if null
    }

    /**
     * Returns a copy of our list of Phrases.
     */
    public List<Phrase> getPhrases() {
        return new ArrayList<>(phrases); // return a copy to preserve immutability
    }

    /**
     * Filters this phrase list to just the top N (sorted in descending order by occurrence count),
     * with the specified minimum phrase length (in words). Only phrases that occur more than once will be included.
     * If two phrases share the same occurrence count, priority will be given to the phrase with the most
     * words, since it's more specific and therefore more interesting.
     * <p>
     * Note that the returned
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
     *
     * @param n How many Phrases to return (this is a maximum, not a guarantee, depending on how many match the filter).
     * @param minPhraseLength The minimum number of words a phrase must have. Must be >= 2 and <= MAX_PHRASE_LENGTH.
     * @return A list of up to n Phrases that match the specified filters, in descending order by occurrence count.
     */
    public List<Phrase> filter(int n, int minPhraseLength) {
        if (n <= 0) {
            return List.of();
        }

        // Clamp the requested phrase length to our defined bounds:
        final int minLength = Math.min(Math.max(minPhraseLength, StatisticsUtil.MIN_PHRASE_LENGTH),
                                       StatisticsUtil.MAX_PHRASE_LENGTH);
        return phrases.stream()
                      .filter(entry -> entry.occurrenceCount() >= StatisticsUtil.MIN_PHRASE_FREQUENCY)
                      .filter(entry -> entry.wordCount() >= minLength)
                           .sorted((p1, p2) -> {
                               // Sort by count, descending:
                               int cmp = Integer.compare(p2.occurrenceCount(), p1.occurrenceCount());
                               if (cmp != 0) { return cmp; }

                               // In the event of a tie, take the longer phrase first (since it's more specific):
                               return Integer.compare(p2.wordCount(), p1.wordCount());
                           })
                           .limit(n)
                           .toList();

    }
}
