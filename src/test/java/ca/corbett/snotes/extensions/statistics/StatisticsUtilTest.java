package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    public void findPhrases_shouldReturnMostCommonPhrases() {
        // GIVEN a list of phrases:
        Phrase phrase1 = new Phrase("common phrase", 2, 3);
        Phrase phrase2 = new Phrase("common phrase one", 3, 2);
        Phrase phrase3 = new Phrase("common phrase two", 3, 1);
        PhraseList phraseList = new PhraseList(List.of(phrase1, phrase2, phrase3));

        // WHEN we find the top 2 phrases:
        List<Phrase> topPhrases = phraseList.filter(2, 2);

        // THEN the most common phrase should be "common phrase" with count 3,
        // and the second most common should be "common phrase one" with count 2
        assertEquals(2, topPhrases.size());
        assertEquals("common phrase", topPhrases.get(0).phrase());
        assertEquals(3, topPhrases.get(0).occurrenceCount());
        assertEquals("common phrase one", topPhrases.get(1).phrase());
        assertEquals(2, topPhrases.get(1).occurrenceCount());
    }

    @Test
    public void findPhrases_withMinimumPhraseLength_shouldFilter() {
        // GIVEN a list of phrases of varying lengths:
        Phrase phrase1 = new Phrase("common phrase", 2, 3);
        Phrase phrase2 = new Phrase("common phrase one", 3, 2);
        PhraseList phraseList = new PhraseList(List.of(phrase1, phrase2));

        // WHEN we find the top 2 phrases with minimum length 3:
        List<Phrase> topPhrases = phraseList.filter(2, 3);

        // THEN it should only return phrases that are at least 3 words long,
        // so "common phrase" (2 words) should be filtered out, and "common phrase one" (3 words) should be the top result
        assertEquals(1, topPhrases.size());
        assertEquals("common phrase one", topPhrases.get(0).phrase());
        assertEquals(2, topPhrases.get(0).occurrenceCount());
    }
}