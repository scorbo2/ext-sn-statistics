package ca.corbett.snotes.extensions.statistics;

/**
 * A simple record to represent a phrase and its occurrence count.
 *
 * @param phrase Any text phrase of two or more words. It can be empty, but it cannot be null.
 * @param wordCount The count of words in this phrase. A non-negative integer (zero if the phrase is empty).
 * @param occurrenceCount The number of times this phrase occurs in the analyzed notes. Must be greater than zero.
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public record Phrase(String phrase, int wordCount, int occurrenceCount) {

    public Phrase {
        if (phrase == null) {
            throw new NullPointerException("phrase cannot be null");
        }
        if (wordCount <= 0) {
            throw new IllegalArgumentException("wordCount cannot be negative or zero");
        }
        if (occurrenceCount <= 0) {
            throw new IllegalArgumentException("occurrenceCount cannot be negative or zero");
        }
    }
}
