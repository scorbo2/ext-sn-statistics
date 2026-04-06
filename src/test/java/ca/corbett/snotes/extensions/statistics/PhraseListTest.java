package ca.corbett.snotes.extensions.statistics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhraseListTest {

    @Test
    public void filter_shouldReturnMostCommonPhrases() {
        // GIVEN a list of phrases with varying occurrence counts:
        Phrase phrase1 = new Phrase("common phrase", 2, 3);
        Phrase phrase2 = new Phrase("common phrase one", 3, 2);
        Phrase phrase3 = new Phrase("common phrase two", 3, 1);
        PhraseList phraseList = new PhraseList(List.of(phrase1, phrase2, phrase3));

        // WHEN we filter for the top 2 phrases:
        List<Phrase> topPhrases = phraseList.filter(2, 2);

        // THEN the most common phrase should be "common phrase" with count 3,
        // and the second most common should be "common phrase one" with count 2;
        // phrase3 is excluded because it only occurs once:
        assertEquals(2, topPhrases.size());
        assertEquals("common phrase", topPhrases.get(0).phrase());
        assertEquals(3, topPhrases.get(0).occurrenceCount());
        assertEquals("common phrase one", topPhrases.get(1).phrase());
        assertEquals(2, topPhrases.get(1).occurrenceCount());
    }

    @Test
    public void filter_withMinimumPhraseLength_shouldFilterByWordCount() {
        // GIVEN a list of phrases of varying lengths:
        Phrase phrase1 = new Phrase("common phrase", 2, 3);
        Phrase phrase2 = new Phrase("common phrase one", 3, 2);
        PhraseList phraseList = new PhraseList(List.of(phrase1, phrase2));

        // WHEN we filter for the top 2 phrases with minimum length 3:
        List<Phrase> topPhrases = phraseList.filter(2, 3);

        // THEN it should only return phrases that are at least 3 words long,
        // so "common phrase" (2 words) should be filtered out:
        assertEquals(1, topPhrases.size());
        assertEquals("common phrase one", topPhrases.get(0).phrase());
        assertEquals(2, topPhrases.get(0).occurrenceCount());
    }

    @Test
    public void filter_withTiedOccurrenceCount_shouldPrioritizeLongerPhrase() {
        // GIVEN two phrases with the same occurrence count but different word counts:
        Phrase shortPhrase = new Phrase("hello world", 2, 5);
        Phrase longPhrase = new Phrase("hello world today", 3, 5);
        PhraseList phraseList = new PhraseList(List.of(shortPhrase, longPhrase));

        // WHEN we filter for phrases:
        List<Phrase> topPhrases = phraseList.filter(2, 2);

        // THEN the longer phrase should come first because it is more specific:
        assertEquals(2, topPhrases.size());
        assertEquals("hello world today", topPhrases.get(0).phrase());
        assertEquals("hello world", topPhrases.get(1).phrase());
    }

    @Test
    public void filter_withNoPhrasesAboveMinFrequency_shouldReturnEmpty() {
        // GIVEN a list of phrases that all occur only once:
        Phrase phrase1 = new Phrase("only once", 2, 1);
        Phrase phrase2 = new Phrase("only once too", 3, 1);
        PhraseList phraseList = new PhraseList(List.of(phrase1, phrase2));

        // WHEN we request the top 10 phrases:
        List<Phrase> topPhrases = phraseList.filter(10, 2);

        // THEN the result should be empty because no phrases occur more than once:
        assertTrue(topPhrases.isEmpty());
    }

    @Test
    public void filter_withNegativeN_shouldReturnEmpty() {
        // GIVEN a non-empty phrase list:
        PhraseList phraseList = new PhraseList(List.of(new Phrase("common phrase", 2, 5)));

        // WHEN we filter with a negative n:
        List<Phrase> topPhrases = phraseList.filter(-1, 2);

        // THEN the result should be empty:
        assertTrue(topPhrases.isEmpty());
    }

    @Test
    public void filter_withZeroN_shouldReturnEmpty() {
        // GIVEN a non-empty phrase list:
        PhraseList phraseList = new PhraseList(List.of(new Phrase("common phrase", 2, 5)));

        // WHEN we filter with n = 0:
        List<Phrase> topPhrases = phraseList.filter(0, 2);

        // THEN the result should be empty:
        assertTrue(topPhrases.isEmpty());
    }

    @Test
    public void filter_withEmptyPhraseList_shouldReturnEmpty() {
        // GIVEN an empty phrase list:
        PhraseList phraseList = new PhraseList(List.of());

        // WHEN we filter:
        List<Phrase> topPhrases = phraseList.filter(10, 2);

        // THEN the result should be empty:
        assertTrue(topPhrases.isEmpty());
    }

    @Test
    public void filter_withNullPhraseList_shouldReturnEmpty() {
        // GIVEN a phrase list constructed with null:
        PhraseList phraseList = new PhraseList(null);

        // WHEN we filter:
        List<Phrase> topPhrases = phraseList.filter(10, 2);

        // THEN the result should be empty:
        assertTrue(topPhrases.isEmpty());
    }
}
