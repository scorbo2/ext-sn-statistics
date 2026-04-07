package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.YMDDate;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.YearFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

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

    // -----------------------------------------------------------------------------------------------------------------
    // findWords tests
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void findWords_withNullNotes_shouldReturnEmptyWordList() {
        // GIVEN a null note list:
        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(null, 10);

        // THEN we should get an empty word list:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withEmptyNotes_shouldReturnEmptyWordList() {
        // GIVEN an empty note list:
        List<Note> notes = List.of();

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(notes, 10);

        // THEN we should get an empty word list:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withZeroN_shouldReturnEmptyWordList() {
        // GIVEN a note with repeating content:
        Note note = new Note();
        note.setText("hello world hello world");

        // WHEN we call findWords with N = 0:
        WordList result = StatisticsUtil.findWords(List.of(note), 0);

        // THEN the result should be empty because N=0 means "return nothing":
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withNegativeN_shouldReturnEmptyWordList() {
        // GIVEN a note with repeating content:
        Note note = new Note();
        note.setText("hello world hello world");

        // WHEN we call findWords with a negative N:
        WordList result = StatisticsUtil.findWords(List.of(note), -1);

        // THEN the result should be empty because N <= 0 is invalid:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withNullNoteText_shouldBeIgnored() {
        // GIVEN a note with null text alongside a note with valid repeating text:
        Note nullTextNote = new Note();
        nullTextNote.setText(null);
        Note realNote = new Note();
        realNote.setText("hello world hello world");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(nullTextNote, realNote), 10);

        // THEN the null-text note should be silently ignored,
        // and words from the real note should still be found:
        assertFalse(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withOnlyPunctuation_shouldReturnEmptyWordList() {
        // GIVEN a note that contains only punctuation:
        Note note = new Note();
        note.setText("!@#$%^&*()");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note), 10);

        // THEN it should return an empty word list, because there are no actual words:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_withWhitespaceOnlyNotes_shouldReturnEmptyWordList() {
        // GIVEN notes with non-empty but whitespace-only text:
        Note note1 = new Note();
        note1.setText("   ");
        Note note2 = new Note();
        note2.setText("\t\n");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN no words should be returned because all note text is whitespace:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_shouldIgnoreShortWords() {
        // GIVEN notes where all recurring words are shorter than MIN_WORD_LENGTH (4 chars):
        Note note1 = new Note();
        note1.setText("the cat sat on the mat");
        Note note2 = new Note();
        note2.setText("the cat sat on the mat");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN short words like "the", "cat", "sat", "on" should be ignored
        // because they are shorter than MIN_WORD_LENGTH:
        assertTrue(result.getWords().stream().noneMatch(w -> w.word().length() < StatisticsUtil.MIN_WORD_LENGTH),
                   "No words shorter than MIN_WORD_LENGTH should be returned");
    }

    @Test
    public void findWords_shouldPruneSingleOccurrenceWords() {
        // GIVEN notes where every long-enough word appears exactly once:
        Note note1 = new Note();
        note1.setText("alpha bravo charlie delta");
        Note note2 = new Note();
        note2.setText("echo foxtrot golf hotel");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN the result should be empty because all words occur only once:
        assertTrue(result.getWords().isEmpty());
    }

    @Test
    public void findWords_shouldStripPunctuationAndNormalizeToLowercase() {
        // GIVEN four notes each containing "hello" and "world" with different cases and punctuation:
        Note note1 = new Note();
        note1.setText("Hello, World!");
        Note note2 = new Note();
        note2.setText("hello world!");
        Note note3 = new Note();
        note3.setText("HELLO WORLD.");
        Note note4 = new Note();
        note4.setText("hello, world");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2, note3, note4), 10);

        // THEN punctuation should be stripped and text normalized to lowercase,
        // so all four occurrences of each word count towards the same normalized form:
        List<Word> words = result.getWords();
        assertTrue(words.stream().anyMatch(w -> w.word().equals("hello") && w.occurrenceCount() == 4),
                   "Expected \"hello\" with occurrence count 4");
        assertTrue(words.stream().anyMatch(w -> w.word().equals("world") && w.occurrenceCount() == 4),
                   "Expected \"world\" with occurrence count 4");
    }

    @Test
    public void findWords_shouldPreserveApostrophes() {
        // GIVEN notes where words contain apostrophes:
        Note note1 = new Note();
        note1.setText("isn't that amazing isn't that amazing");
        Note note2 = new Note();
        note2.setText("isn't that amazing");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN apostrophes should be preserved so "isn't" is treated as one word:
        List<Word> words = result.getWords();
        assertTrue(words.stream().anyMatch(w -> w.word().equals("isn't")),
                   "Expected \"isn't\" to be present as a single word");
        assertFalse(words.stream().anyMatch(w -> w.word().equals("isn") || w.word().equals("t")),
                    "Apostrophe should not cause \"isn't\" to be split into \"isn\" and \"t\"");
    }

    @Test
    public void findWords_shouldReturnWordsInDecreasingOrderOfOccurrence() {
        // GIVEN notes where "hello" appears more often than "world" and "world" more than "amazing":
        Note note1 = new Note();
        note1.setText("hello hello hello world world amazing");
        Note note2 = new Note();
        note2.setText("hello hello world amazing");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN words should be returned in decreasing order of occurrence count:
        List<Word> words = result.getWords();
        assertFalse(words.isEmpty());
        for (int i = 0; i < words.size() - 1; i++) {
            assertTrue(words.get(i).occurrenceCount() >= words.get(i + 1).occurrenceCount(),
                       "Words should be in decreasing order of occurrence count");
        }
        assertEquals("hello", words.get(0).word());
    }

    @Test
    public void findWords_shouldLimitResultsToN() {
        // GIVEN a note with several recurring long words:
        Note note1 = new Note();
        note1.setText("alpha bravo charlie delta echo foxtrot");
        Note note2 = new Note();
        note2.setText("alpha bravo charlie delta echo foxtrot");

        // WHEN we call findWords with N = 3:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 3);

        // THEN at most 3 words should be returned:
        assertTrue(result.getWords().size() <= 3,
                   "Result should contain at most N=3 words");
    }

    @Test
    public void findWords_withNGreaterThanAvailableWords_shouldReturnAllAvailableWords() {
        // GIVEN a note where only 2 long words recur:
        Note note1 = new Note();
        note1.setText("hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // WHEN we call findWords requesting more than the number of available recurring words:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 100);

        // THEN we should get back only the 2 available recurring words (not an error):
        assertEquals(2, result.getWords().size());
    }

    @Test
    public void findWords_shouldCountAcrossMultipleNotes() {
        // GIVEN the same word spread across multiple notes:
        Note note1 = new Note();
        note1.setText("hello there");
        Note note2 = new Note();
        note2.setText("hello again");
        Note note3 = new Note();
        note3.setText("hello everyone");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2, note3), 10);

        // THEN "hello" should be counted 3 times (once per note):
        List<Word> words = result.getWords();
        assertTrue(words.stream().anyMatch(w -> w.word().equals("hello") && w.occurrenceCount() == 3),
                   "Expected \"hello\" with occurrence count 3 across all notes");
    }

    // -----------------------------------------------------------------------------------------------------------------
    // findWords - unique word count tests
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void findWords_withNullNotes_shouldReturnNoDataForUniqueWordCount() {
        // GIVEN a null note list:
        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(null, 10);

        // THEN uniqueWordCount should be NO_DATA because no scanning was performed:
        assertEquals(WordList.NO_DATA, result.getUniqueWordCount());
    }

    @Test
    public void findWords_withEmptyNotes_shouldReturnNoDataForUniqueWordCount() {
        // GIVEN an empty note list:
        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(), 10);

        // THEN uniqueWordCount should be NO_DATA because no scanning was performed:
        assertEquals(WordList.NO_DATA, result.getUniqueWordCount());
    }

    @Test
    public void findWords_withZeroN_shouldReturnNoDataForUniqueWordCount() {
        // GIVEN a note with text:
        Note note = new Note();
        note.setText("hello world hello world");

        // WHEN we call findWords with N = 0:
        WordList result = StatisticsUtil.findWords(List.of(note), 0);

        // THEN uniqueWordCount should be NO_DATA because we bailed out early:
        assertEquals(WordList.NO_DATA, result.getUniqueWordCount());
    }

    @Test
    public void findWords_shouldTrackUniqueWordCount() {
        // GIVEN a note with three distinct words:
        Note note = new Note();
        note.setText("alpha bravo charlie");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note), 10);

        // THEN the unique word count should reflect all three distinct words seen:
        assertEquals(3, result.getUniqueWordCount());
    }

    @Test
    public void findWords_shouldIncludeShortWordsInUniqueWordCount() {
        // GIVEN a note where some words are shorter than MIN_WORD_LENGTH ("the", "cat"):
        Note note = new Note();
        note.setText("the cat sits");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note), 10);

        // THEN short words should NOT appear in the returned word list (they are filtered),
        // but they SHOULD still be counted in the unique word count:
        assertTrue(result.getWords().stream().noneMatch(w -> w.word().equals("the") || w.word().equals("cat")),
                   "Short words should be excluded from the returned word list");
        assertEquals(3, result.getUniqueWordCount(),
                     "Short words should still be counted in the unique word count");
    }

    @Test
    public void findWords_shouldIncludeSingleOccurrenceWordsInUniqueWordCount() {
        // GIVEN two notes where all long words appear only once (and are therefore pruned from results):
        Note note1 = new Note();
        note1.setText("alpha bravo charlie");
        Note note2 = new Note();
        note2.setText("delta echo foxtrot");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN no words should be returned (all single-occurrence), but the unique word
        // count should still reflect all 6 distinct words that were scanned:
        assertTrue(result.getWords().isEmpty(),
                   "All single-occurrence words should be pruned from results");
        assertEquals(6, result.getUniqueWordCount(),
                     "All scanned words should be counted in unique word count even if pruned from results");
    }

    @Test
    public void findWords_shouldDeduplicateUniqueWordsAcrossNotes() {
        // GIVEN the same word appearing in two separate notes:
        Note note1 = new Note();
        note1.setText("hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // WHEN we call findWords:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN the unique word count should be 2 (not 4), since "hello" and "world"
        // are the same two unique words regardless of how many notes contain them:
        assertEquals(2, result.getUniqueWordCount(),
                     "Unique word count should deduplicate the same word seen in multiple notes");
    }

    // -----------------------------------------------------------------------------------------------------------------
    // findWords(List<Note>, Set<String>, int) - specific-words overload tests
    // -----------------------------------------------------------------------------------------------------------------

    @Test
    public void findWords_withSpecificWords_shouldCountOnlyWordsInTheSet() {
        // GIVEN notes that contain several recurring words:
        Note note1 = new Note();
        note1.setText("hello world today hello world today");
        Note note2 = new Note();
        note2.setText("hello world today");

        // AND we only want to count "hello":
        Set<String> specificWords = Set.of("hello");

        // WHEN we call the specific-words overload:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), specificWords, 10);

        // THEN only "hello" should be present in the result:
        List<Word> words = result.getWords();
        assertEquals(1, words.size(), "Only the specified word should be returned");
        assertEquals("hello", words.get(0).word());
        assertEquals(3, words.get(0).occurrenceCount());
    }

    @Test
    public void findWords_withSpecificWords_shouldIgnoreMinWordLength() {
        // GIVEN notes with a short recurring word that would normally be filtered by MIN_WORD_LENGTH:
        Note note1 = new Note();
        note1.setText("the cat sat");
        Note note2 = new Note();
        note2.setText("the cat sat");

        // AND we explicitly ask for "the" (which is shorter than MIN_WORD_LENGTH):
        Set<String> specificWords = Set.of("the");

        // WHEN we call the specific-words overload:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), specificWords, 10);

        // THEN "the" should be counted even though it is shorter than MIN_WORD_LENGTH:
        List<Word> words = result.getWords();
        assertTrue(words.stream().anyMatch(w -> w.word().equals("the") && w.occurrenceCount() == 2),
                   "Words shorter than MIN_WORD_LENGTH should be counted when listed in specificWords");
    }

    @Test
    public void findWords_withSpecificWords_shouldIgnoreWordsNotInTheSet() {
        // GIVEN notes containing both "hello" and "world", recurring multiple times:
        Note note1 = new Note();
        note1.setText("hello world hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // AND we only ask for "hello":
        Set<String> specificWords = Set.of("hello");

        // WHEN we call the specific-words overload:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), specificWords, 10);

        // THEN "world" should NOT appear in the results, even though it recurs:
        List<Word> words = result.getWords();
        assertTrue(words.stream().noneMatch(w -> w.word().equals("world")),
                   "Words not in the specificWords set should be excluded from results");
    }

    @Test
    public void findWords_withNullSpecificWords_shouldBehaveLikeGeneralOverload() {
        // GIVEN a note with recurring long words:
        Note note1 = new Note();
        note1.setText("hello world hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // WHEN we call the specific-words overload with null:
        WordList withNull = StatisticsUtil.findWords(List.of(note1, note2), null, 10);

        // AND we call the general overload for comparison:
        WordList withoutSet = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN both should return the same words and counts:
        assertEquals(withoutSet.getWords().size(), withNull.getWords().size());
        for (int i = 0; i < withoutSet.getWords().size(); i++) {
            assertEquals(withoutSet.getWords().get(i).word(), withNull.getWords().get(i).word());
            assertEquals(withoutSet.getWords().get(i).occurrenceCount(), withNull.getWords().get(i).occurrenceCount());
        }
    }

    @Test
    public void findWords_withEmptySpecificWords_shouldBehaveLikeGeneralOverload() {
        // GIVEN a note with recurring long words:
        Note note1 = new Note();
        note1.setText("hello world hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // WHEN we call the specific-words overload with an empty set:
        WordList withEmpty = StatisticsUtil.findWords(List.of(note1, note2), Set.of(), 10);

        // AND we call the general overload for comparison:
        WordList withoutSet = StatisticsUtil.findWords(List.of(note1, note2), 10);

        // THEN both should return the same words and counts:
        assertEquals(withoutSet.getWords().size(), withEmpty.getWords().size());
        for (int i = 0; i < withoutSet.getWords().size(); i++) {
            assertEquals(withoutSet.getWords().get(i).word(), withEmpty.getWords().get(i).word());
            assertEquals(withoutSet.getWords().get(i).occurrenceCount(), withEmpty.getWords().get(i).occurrenceCount());
        }
    }

    @Test
    public void findWords_withSpecificWords_shouldStillTrackUniqueWordCount() {
        // GIVEN notes with several distinct words:
        Note note1 = new Note();
        note1.setText("hello world today");
        Note note2 = new Note();
        note2.setText("hello world tomorrow");

        // AND we only ask for "hello":
        Set<String> specificWords = Set.of("hello");

        // WHEN we call the specific-words overload:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), specificWords, 10);

        // THEN the unique word count should reflect ALL distinct words scanned (4),
        // not just the words in the specific set (1):
        assertEquals(4, result.getUniqueWordCount(),
                     "Unique word count should track all scanned words, even when specificWords is used");
    }

    @Test
    public void findWords_withSpecificWordNotPresentInNotes_shouldReturnEmptyWordList() {
        // GIVEN notes that do not contain the requested word at all:
        Note note1 = new Note();
        note1.setText("hello world hello world");
        Note note2 = new Note();
        note2.setText("hello world");

        // AND we ask for a word that does not appear in any note:
        Set<String> specificWords = Set.of("elephant");

        // WHEN we call the specific-words overload:
        WordList result = StatisticsUtil.findWords(List.of(note1, note2), specificWords, 10);

        // THEN the returned word list should be empty:
        assertTrue(result.getWords().isEmpty(),
                   "Word list should be empty when the specified word does not appear in any note");
    }
}