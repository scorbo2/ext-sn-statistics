package ca.corbett.snotes.extensions.statistics;

/**
 * A simple record to represent a word and its occurrence count.
 * During our stats load, all text is normalized to lowercase, and stripped
 * of all punctuation, except apostrophes. So, words like "isn't" are preserved as "isn't",
 * but "hello!" and "hello" would both be normalized to "hello".
 *
 * @param word            The word in question. Must not be null.
 * @param occurrenceCount The number of times this word occurs in the (normalized) analyzed notes. Must be &gt; 0.
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public record Word(String word, int occurrenceCount) {

    public Word {
        if (word == null) {
            throw new NullPointerException("word cannot be null");
        }
        if (occurrenceCount <= 0) {
            throw new IllegalArgumentException("occurrenceCount cannot be negative or zero");
        }
    }
}
