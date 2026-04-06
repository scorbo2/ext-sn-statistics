package ca.corbett.snotes.extensions.statistics;

import ca.corbett.snotes.model.Note;

import java.time.DayOfWeek;
import java.time.Month;
import java.util.List;
import java.util.Map;

/**
 * Holds all gathered statistics in one convenient model object. This
 * will be loaded up by the StatisticsLoaderThread and then used by
 * all charts in the StatisticsDialog.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class Statistics {

    private final int totalNoteCount;
    private final int totalWordCount;
    private final List<Note> allNotes; // all notes we analyzed, in case any chart needs to drill down into them
    private final List<Integer> uniqueYears;
    private final Map<Integer, Integer> wordCountByYear; // year -> word count for that year
    private final Map<Integer, Integer> noteCountByYear; // year -> note count for that year
    private final Map<Integer, Integer> allMonthsWordCount; // month (1-12) -> total words in that month (all years)
    private final Map<Integer, Integer> allMonthsNoteCount; // month (1-12) -> total notes in that month (all years)
    private final Map<Integer, Map<Integer, Integer>> wordCountByYearAndMonth; // year -> month -> word count
    private final Map<Integer, Map<Integer, Integer>> noteCountByYearAndMonth; // year -> month -> note count
    private final Map<Integer, Integer> wordCountByDayOfWeek; // day of week (1-7) -> total words on that day (all years)
    private final Map<Integer, Integer> noteCountByDayOfWeek; // day of week (1-7) -> total notes on that day (all years)
    private final PhraseList allPhrases; // all phrases of length 2-10 across all notes, sorted by frequency
    private final Map<Integer, List<Phrase>> phrasesByLength; // phrase length -> list of phrases of that length

    public Statistics(List<Note> allNotes, int totalNoteCount, int totalWordCount, List<Integer> uniqueYears,
                      Map<Integer, Integer> wordCountByYear, Map<Integer, Integer> noteCountByYear,
                      Map<Integer, Integer> allMonthsWordCount, Map<Integer, Integer> allMonthsNoteCount,
                      Map<Integer, Map<Integer, Integer>> wordCountByYearAndMonth,
                      Map<Integer, Map<Integer, Integer>> noteCountByYearAndMonth,
                      Map<Integer, Integer> wordCountByDayOfWeek, Map<Integer, Integer> noteCountByDayOfWeek,
                      PhraseList allPhrases, Map<Integer, List<Phrase>> phrasesByLength) {
        this.allNotes = allNotes;
        this.totalNoteCount = totalNoteCount;
        this.totalWordCount = totalWordCount;
        this.uniqueYears = uniqueYears;
        this.wordCountByYear = wordCountByYear;
        this.noteCountByYear = noteCountByYear;
        this.allMonthsWordCount = allMonthsWordCount;
        this.allMonthsNoteCount = allMonthsNoteCount;
        this.wordCountByYearAndMonth = wordCountByYearAndMonth;
        this.noteCountByYearAndMonth = noteCountByYearAndMonth;
        this.wordCountByDayOfWeek = wordCountByDayOfWeek;
        this.noteCountByDayOfWeek = noteCountByDayOfWeek;
        this.allPhrases = allPhrases;
        this.phrasesByLength = phrasesByLength;
    }

    public List<Note> getAllNotes() {
        return allNotes;
    }

    public int getTotalNoteCount() {
        return totalNoteCount;
    }

    public int getTotalWordCount() {
        return totalWordCount;
    }

    public List<Integer> getUniqueYears() {
        return uniqueYears;
    }

    public Map<Integer, Integer> getWordCountByYear() {
        return wordCountByYear;
    }

    public Map<Integer, Integer> getNoteCountByYear() {
        return noteCountByYear;
    }

    public Map<Integer, Integer> getAllMonthsWordCount() {
        return allMonthsWordCount;
    }

    public Map<Integer, Integer> getAllMonthsNoteCount() {
        return allMonthsNoteCount;
    }

    public Map<Integer, Map<Integer, Integer>> getWordCountByYearAndMonth() {
        return wordCountByYearAndMonth;
    }

    public Map<Integer, Map<Integer, Integer>> getNoteCountByYearAndMonth() {
        return noteCountByYearAndMonth;
    }

    public Map<Integer, Integer> getWordCountByDayOfWeek() {
        return wordCountByDayOfWeek;
    }

    public Map<Integer, Integer> getNoteCountByDayOfWeek() {
        return noteCountByDayOfWeek;
    }

    /**
     * Returns the DayOfWeek where the most words were written (across all years and months).
     * If no data is present, will return null.
     */
    public DayOfWeek getMostActiveDayByWords() {
        int mostWords = 0;
        int mostActiveDay = -1; // possible if we have no data
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            int wordCount = wordCountByDayOfWeek.getOrDefault(dayOfWeek, 0);
            if (wordCount > mostWords) {
                mostWords = wordCount;
                mostActiveDay = dayOfWeek;
            }
        }
        return mostActiveDay == -1 ? null : StatisticsUtil.DAYS_OF_WEEK[mostActiveDay - 1];
    }

    /**
     * Returns the DayOfWeek where the most notes were written (across all years and months).
     * If no data is present, will return null.
     */
    public DayOfWeek getMostActiveDayByNotes() {
        int mostNotes = 0;
        int mostActiveDay = -1; // possible if we have no data
        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            int noteCount = noteCountByDayOfWeek.getOrDefault(dayOfWeek, 0);
            if (noteCount > mostNotes) {
                mostNotes = noteCount;
                mostActiveDay = dayOfWeek;
            }
        }
        return mostActiveDay == -1 ? null : StatisticsUtil.DAYS_OF_WEEK[mostActiveDay - 1];
    }

    /**
     * Returns the Month where the most words were written (across all years).
     * If no data is present, will return null.
     */
    public Month getMostActiveMonthByWords() {
        int mostWords = 0;
        int mostActiveMonth = -1; // possible if we have no data
        for (int month = 1; month <= 12; month++) {
            int wordCount = allMonthsWordCount.getOrDefault(month, 0);
            if (wordCount > mostWords) {
                mostWords = wordCount;
                mostActiveMonth = month;
            }
        }
        return mostActiveMonth == -1 ? null : Month.of(mostActiveMonth);
    }

    /**
     * Returns the Month where the most notes were written (across all years).
     * If no data is present, will return null.
     */
    public Month getMostActiveMonthByNotes() {
        int mostNotes = 0;
        int mostActiveMonth = -1; // possible if we have no data
        for (int month = 1; month <= 12; month++) {
            int noteCount = allMonthsNoteCount.getOrDefault(month, 0);
            if (noteCount > mostNotes) {
                mostNotes = noteCount;
                mostActiveMonth = month;
            }
        }
        return mostActiveMonth == -1 ? null : Month.of(mostActiveMonth);
    }
    
    public PhraseList getAllPhrases() {
        return allPhrases;
    }

    public Map<Integer, List<Phrase>> getPhrasesByLength() {
        return phrasesByLength;
    }
}
