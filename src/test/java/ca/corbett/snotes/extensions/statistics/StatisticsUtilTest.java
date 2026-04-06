package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsUtilTest {

    @Test
    public void countWords_withNullOrEmptyNote_shouldReturnZero() {
        // GIVEN a null or empty note
        Note nullNote = null;
        Note blankNote = new Note();
        blankNote.setText("");
        Note noteWithNullText = new Note();
        noteWithNullText.setText(null);

        // WHEN we countWords for these notes:
        int nullCount = StatisticsUtil.countWords(nullNote);
        int blankCount = StatisticsUtil.countWords(blankNote);
        int nullTextCount = StatisticsUtil.countWords(noteWithNullText);

        // THEN they should all be zero:
        assertEquals(0, nullCount);
        assertEquals(0, blankCount);
        assertEquals(0, nullTextCount);
    }

    @Test
    public void countWords_withOnlyPunctuation_shouldReturnZero() {
        // GIVEN a note that contains only punctuation:
        Note note = new Note();
        note.setText("!@#$%^&*()");

        // WHEN we countWords for this note:
        int count = StatisticsUtil.countWords(note);

        // THEN it should return zero, because there are no actual words:
        assertEquals(0, count);
    }

    @Test
    public void countWords_withRegularText_shouldReturnCorrectCount() {
        // GIVEN a note with regular text, including punctuation and contractions:
        Note note = new Note();
        note.setText("Hello, world! This is a test. Isn't it great?");

        // WHEN we countWords for this note:
        int count = StatisticsUtil.countWords(note);

        // THEN it should return the correct word count (9 words):
        assertEquals(9, count);
    }

    @Test
    public void countWords_withMultipleSpaces_shouldReturnCorrectCount() {
        // GIVEN a note with multiple spaces between words:
        Note note = new Note();
        note.setText("This   is  a   test");

        // WHEN we countWords for this note:
        int count = StatisticsUtil.countWords(note);

        // THEN it should return the correct word count (4 words):
        assertEquals(4, count);
    }

    @Test
    public void countWords_withListOfNotes_shouldSumAll() {
        // GIVEN a list of notes with text:
        Note note1 = new Note();
        note1.setText("Hello world");
        Note note2 = new Note();
        note2.setText("This is a test");
        Note note3 = new Note();
        note3.setText("Isn't it great?");
        List<Note> notes = List.of(note1, note2, note3);

        // WHEN we countWords on the whole list:
        int count = StatisticsUtil.countWords(notes);

        // THEN it should return the sum of all words (2 + 4 + 3 = 9):
        assertEquals(9, count);
    }

    @Test
    public void countWords_withFilteredList_shouldFilterAndSum() {
        // GIVEN a list of notes and a query that filters some of them out:
        Note note1 = new Note();
        note1.setText("Hello world");
        note1.setDate(new YMDDate("1999-01-01"));
        Note note2 = new Note();
        note2.setText("This is a test");
        note2.setDate(new YMDDate("1999-06-10"));
        Note note3 = new Note();
        note3.setText("Isn't it great?");
        note3.setDate(new YMDDate("2000-05-05"));
        List<Note> notes = List.of(note1, note2, note3);
        Query query = new Query();
        query.addFilter(new YearFilter(1999, DateFilterType.ON)); // only include notes from 1999

        // WHEN we countWords with this list and this query:
        int count = StatisticsUtil.countWords(notes, query);

        // THEN it should sum only the notes that match the query:
        // note1 has 2 words, note2 has 4 words, note3 is filtered out, so total should be 6:
        assertEquals(6, count);
    }

    @Test
    public void findPhrases_withNullNotes_shouldReturnEmptyPhraseList() {
        // GIVEN a null note list:
        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(null);

        // THEN we should get an empty phrase list:
        assertTrue(result.getPhrases().isEmpty());
    }

    @Test
    public void findPhrases_withEmptyNotes_shouldReturnEmptyPhraseList() {
        // GIVEN an empty note list:
        List<Note> notes = List.of();

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(notes);

        // THEN we should get an empty phrase list:
        assertTrue(result.getPhrases().isEmpty());
    }

    @Test
    public void findPhrases_withQueryThatFiltersAllNotes_shouldReturnEmptyPhraseList() {
        // GIVEN a note from 1999 and a query that only matches notes from 2000:
        Note note = new Note();
        note.setText("hello world hello world");
        note.setDate(new YMDDate("1999-01-01"));
        Query query = new Query();
        query.addFilter(new YearFilter(2000, DateFilterType.ON));

        // WHEN we call findPhrases with this filter query:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note), query);

        // THEN the result should be empty because the query filters out all notes:
        assertTrue(result.getPhrases().isEmpty());
    }

    @Test
    public void findPhrases_withNotesThatNormalizeToWhitespace_shouldBeIgnored() {
        // GIVEN a note whose text consists entirely of punctuation (normalizes to whitespace):
        Note punctuationNote = new Note();
        punctuationNote.setText("!!! ??? ...");

        // AND a note with real recurring text:
        Note realNote = new Note();
        realNote.setText("hello world hello world");

        // WHEN we call findPhrases on both notes:
        PhraseList result = StatisticsUtil.findPhrases(List.of(punctuationNote, realNote));

        // THEN the punctuation-only note should be ignored, and phrases from the real note should be present:
        List<Phrase> filtered = result.filter(10, 2);
        assertFalse(filtered.isEmpty());
        assertEquals("hello world", filtered.get(0).phrase());
    }

    @Test
    public void findPhrases_shouldStripPunctuationAndNormalizeToLowercase() {
        // GIVEN four notes each containing just "hello world" with different cases and punctuation:
        Note note1 = new Note();
        note1.setText("Hello, World!");
        Note note2 = new Note();
        note2.setText("hello world!");
        Note note3 = new Note();
        note3.setText("HELLO WORLD.");
        Note note4 = new Note();
        note4.setText("hello, world");

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note1, note2, note3, note4));

        // THEN punctuation should be stripped and text normalized to lowercase,
        // so all four occurrences count towards the same normalized phrase "hello world":
        List<Phrase> filtered = result.filter(10, 2);
        assertEquals(1, filtered.size());
        assertEquals("hello world", filtered.get(0).phrase());
        assertEquals(4, filtered.get(0).occurrenceCount());
    }

    @Test
    public void findPhrases_shouldPruneSingleOccurrencePhrases() {
        // GIVEN notes where every phrase appears exactly once (no overlapping words between notes):
        Note note1 = new Note();
        note1.setText("alpha beta gamma delta");
        Note note2 = new Note();
        note2.setText("echo foxtrot golf hotel");

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note1, note2));

        // THEN the result should be empty because all phrases occur only once:
        assertTrue(result.getPhrases().isEmpty());
    }

    @Test
    public void findPhrases_shouldReturnPhrasesInDecreasingOrderOfOccurrence() {
        // GIVEN a note with a phrase that appears more often than others:
        // "hello world" appears 3 times, while other 2-word phrases appear only twice
        Note note = new Note();
        note.setText("hello world today hello world today hello world");

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note));

        // THEN the phrases should be returned in decreasing order of occurrence count:
        List<Phrase> filtered = result.filter(10, 2);
        assertFalse(filtered.isEmpty());
        for (int i = 0; i < filtered.size() - 1; i++) {
            assertTrue(filtered.get(i).occurrenceCount() >= filtered.get(i + 1).occurrenceCount(),
                       "Phrases should be in decreasing order of occurrence count");
        }
        // The most common 2-word phrase should be first:
        assertEquals("hello world", filtered.get(0).phrase());
        assertEquals(3, filtered.get(0).occurrenceCount());
    }

    @Test
    public void findPhrases_shouldPreserveApostrophes() {
        // GIVEN notes where words contain apostrophes:
        Note note1 = new Note();
        note1.setText("isn't it great isn't it great");
        Note note2 = new Note();
        note2.setText("isn't it great");

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note1, note2));

        // THEN apostrophes should be preserved so "isn't" is treated as one word,
        // not split into "isn" and "t":
        List<Phrase> filtered = result.filter(10, 2);
        assertTrue(filtered.stream().anyMatch(p -> p.phrase().equals("isn't it")),
                   "Expected phrase \"isn't it\" to be present");
        assertEquals(3, filtered.stream()
                                 .filter(p -> p.phrase().equals("isn't it"))
                                 .findFirst().get().occurrenceCount(),
                     "\"isn't it\" should occur 3 times across both notes");
        assertFalse(filtered.stream().anyMatch(p -> p.phrase().startsWith("isn ") || p.phrase().contains(" t ")),
                    "Apostrophe should not cause \"isn't\" to be split");
    }

    @Test
    public void findPhrases_withWhitespaceOnlyNotes_shouldReturnEmptyPhraseList() {
        // GIVEN notes with non-empty but whitespace-only text:
        Note note1 = new Note();
        note1.setText("   ");
        Note note2 = new Note();
        note2.setText("\t\n");

        // WHEN we call findPhrases:
        PhraseList result = StatisticsUtil.findPhrases(List.of(note1, note2));

        // THEN no phrases should be returned because all note text is whitespace:
        assertTrue(result.getPhrases().isEmpty());
    }
}