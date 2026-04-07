package ca.corbett.snotes.extensions.statistics;

import java.util.List;

/**
 * Represents a list of words and their occurrence counts, as gathered from the analyzed notes.
 * This class also tracks the total number of unique words that were scanned in all notes.
 * That is, not just the top N, but the TOTAL number of unique words that were scanned.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class WordList {

    public static final int NO_DATA = -1;

    private final int uniqueWordCount;
    private final List<Word> words;

    /**
     * Constructs a WordList with the given list of words.
     * The list is expected to be sorted in descending order by occurrence count.
     * A null list is considered equivalent to an empty list.
     * The "total unique word count" feature is disabled with this constructor.
     * Use WordList(List&lt;Word&gt;, int) if you want to specify the total unique word count as well.
     */
    public WordList(List<Word> words) {
        this(words, NO_DATA);
    }

    /**
     * Constructs a WordList with the given list of words,
     * and also enables the "total unique word count" feature by accepting a count
     * of the total number of unique words that were scanned in all notes.
     */
    public WordList(List<Word> words, int uniqueWordCount) {
        this.words = words == null ? List.of() : words; // default to empty list if null
        this.uniqueWordCount = uniqueWordCount;
    }

    /**
     * Returns a copy of our word list.
     */
    public List<Word> getWords() {
        return List.copyOf(words); // return a copy to preserve immutability
    }

    /**
     * Returns the total number of unique words that were scanned in all notes.
     * This is {@link #NO_DATA} if the feature was not populated, including when the
     * current statistics workflow is given null or empty notes.
     */
    public int getUniqueWordCount() {
        return uniqueWordCount;
    }
}
