package ca.corbett.snotes.extensions.statistics;

/**
 * A simple record to represent a phrase and its occurrence count.
 *
 * @param phrase Any text phrase of two or more words. It can be empty, but it cannot be null.
 * @param occurrenceCount The number of times this phrase occurs in the analyzed notes. A non-negative integer.
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public record Phrase(String phrase, int occurrenceCount) {

    public Phrase {
        if (phrase == null) {
            throw new NullPointerException("phrase cannot be null");
        }
        if (occurrenceCount < 0) {
            throw new IllegalArgumentException("occurrenceCount cannot be negative");
        }
    }
}
