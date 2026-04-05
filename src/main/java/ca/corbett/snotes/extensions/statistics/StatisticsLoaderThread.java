package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.snotes.io.DataManager;
import ca.corbett.snotes.model.Note;
import ca.corbett.snotes.model.Query;
import ca.corbett.snotes.model.QueryFactory;
import ca.corbett.snotes.model.filter.BooleanFilterType;
import ca.corbett.snotes.model.filter.DateFilterType;
import ca.corbett.snotes.model.filter.DayOfWeekFilter;
import ca.corbett.snotes.model.filter.YearFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Even though DataManager caches all notes in-memory, gathering statistics
 * from them is surprisingly CPU expensive, especially for the phrase analysis.
 * So, we'll have a loader thread to do all the heavy lifting, and then
 * we'll populate a Statistics object that can be passed around all our
 * charts without having to re-query anything after the initial load.
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class StatisticsLoaderThread extends SimpleProgressWorker {

    private static final String MSG = "This may take some time!";
    private final DataManager dataManager;
    private volatile int currentStep;
    private volatile int totalSteps;
    private volatile boolean wasCanceled;

    private int totalNoteCount;
    private int totalWordCount;
    private List<Integer> uniqueYears;
    private Map<Integer, Integer> wordCountByYear; // year -> word count for that year
    private Map<Integer, Integer> noteCountByYear; // year -> note count for that year
    private Map<Integer, Integer> allMonthsWordCount; // month (1-12) -> total words in that month (all years)
    private Map<Integer, Integer> allMonthsNoteCount; // month (1-12) -> total notes in that month (all years)
    private Map<Integer, Map<Integer, Integer>> wordCountByYearAndMonth; // year -> month -> word count
    private Map<Integer, Map<Integer, Integer>> noteCountByYearAndMonth; // year -> month -> note count
    private Map<Integer, Integer> wordCountByDayOfWeek; // day of week (1-7) -> total words on that day (all years)
    private Map<Integer, Integer> noteCountByDayOfWeek; // day of week (1-7) -> total notes on that day (all years)
    private PhraseList allPhrases; // all phrases of length 2-10 across all notes, sorted by frequency
    private Map<Integer, List<Phrase>> phrasesByLength; // phrase length -> list of phrases of that length

    public StatisticsLoaderThread(DataManager dataManager) {
        this.dataManager = dataManager;
        this.currentStep = 0;
    }

    public boolean wasCanceled() {
        return wasCanceled;
    }

    /**
     * Creates and returns a Statistics instance containing all the statistics we've gathered.
     * This should only be called after the thread has completed.
     */
    public Statistics getStatistics() {
        return new Statistics(dataManager.getNotes(),
                              totalNoteCount,
                              totalWordCount,
                              uniqueYears,
                              wordCountByYear,
                              noteCountByYear,
                              allMonthsWordCount,
                              allMonthsNoteCount,
                              wordCountByYearAndMonth,
                              noteCountByYearAndMonth,
                              wordCountByDayOfWeek,
                              noteCountByDayOfWeek,
                              allPhrases,
                              phrasesByLength);
    }

    @Override
    public void run() {
        wasCanceled = false;
        List<Runnable> workers = new ArrayList<>();
        workers.add(this::loadGeneralStats);
        workers.add(this::loadYearData);
        workers.add(this::loadAllMonthsData);
        workers.add(this::loadMonthData);
        workers.add(this::loadWeekData);
        workers.add(this::loadPhrases);

        // we do one pass to find all phrases, then one pass per phrase length to filter them:
        int phraseSteps = (StatisticsUtil.MAX_PHRASE_LENGTH - StatisticsUtil.MIN_PHRASE_LENGTH);

        // Fire them all off and let them run in parallel.
        // Each worker will call chunkComplete() when it finishes.
        totalSteps = workers.size() + phraseSteps;
        fireProgressBegins(totalSteps + 1);
        ExecutorService executor = Executors.newFixedThreadPool(workers.size());
        try {
            List<Future<?>> futures = workers.stream()
                                             .map(executor::submit)
                                             .collect(Collectors.toList());
            for (Future<?> f : futures) {
                f.get(); // wait for all
            }
        }
        catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Statistics worker failed", e);

            // If any worker threw an exception, we'll end up here. In that case, we'll just cancel the whole operation.
            wasCanceled = true;
            fireProgressCanceled();
        }
        finally {
            executor.shutdown();
        }
    }

    /**
     * Fired when one of our worker threads complete.
     * We'll track progress, update our progress bar, and
     * check for user cancellation. When the last chunk
     * completes, we'll fire the completion event.
     */
    private synchronized void chunkComplete() {
        // If a previous worker errored out or was canceled,
        // then we have already fired the canceled event.
        // So, we do nothing here:
        if (wasCanceled) {
            return;
        }

        currentStep++;
        if (!fireProgressUpdate(currentStep, MSG)) {
            wasCanceled = true;
            fireProgressCanceled();
            return;
        }

        if (currentStep > totalSteps) {
            fireProgressComplete();
        }
    }

    /**
     * Loads the high level general stuff.
     */
    void loadGeneralStats() {
        List<Note> allNotes = dataManager.getNotes();
        totalNoteCount = allNotes.size();
        totalWordCount = StatisticsUtil.countWords(allNotes);
        chunkComplete();
    }

    /**
     * Loads data per year, for all unique years in the dataManager.
     */
    void loadYearData() {
        uniqueYears = dataManager.getUniqueYears();
        wordCountByYear = new HashMap<>();
        noteCountByYear = new HashMap<>();
        for (int year : uniqueYears) {
            List<Note> notesForYear = QueryFactory.year(year).execute(dataManager.getNotes());
            wordCountByYear.put(year, StatisticsUtil.countWords(notesForYear));
            noteCountByYear.put(year, notesForYear.size());
        }
        chunkComplete();
    }

    /**
     * Sums data for each calendar month without regard to year.
     */
    void loadAllMonthsData() {
        allMonthsWordCount = new HashMap<>();
        allMonthsNoteCount = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            List<Note> notesForMonth = QueryFactory.month(month).execute(dataManager.getNotes());
            allMonthsWordCount.put(month, StatisticsUtil.countWords(notesForMonth));
            allMonthsNoteCount.put(month, notesForMonth.size());
        }
        chunkComplete();
    }

    /**
     * Loads data per month, for all unique years and all 12 months in each year.
     */
    void loadMonthData() {
        wordCountByYearAndMonth = new HashMap<>();
        noteCountByYearAndMonth = new HashMap<>();
        for (int year : dataManager.getUniqueYears()) {
            for (int month = 1; month <= 12; month++) {
                Query query = QueryFactory.month(month).addFilter(new YearFilter(year, DateFilterType.ON));
                List<Note> notesForMonth = query.execute(dataManager.getNotes());
                wordCountByYearAndMonth.computeIfAbsent(year, y -> new HashMap<>())
                                       .put(month, StatisticsUtil.countWords(notesForMonth));
                noteCountByYearAndMonth.computeIfAbsent(year, y -> new HashMap<>()).put(month, notesForMonth.size());
            }
        }
        chunkComplete();
    }

    /**
     * Loads data for each day of the week, without regard to year.
     * We'll use 1-7 for Monday-Sunday, to match Java's DayOfWeek enum.
     */
    void loadWeekData() {
        wordCountByDayOfWeek = new HashMap<>();
        noteCountByDayOfWeek = new HashMap<>();

        for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
            Query weekQuery = new Query();
            weekQuery.addFilter(new DayOfWeekFilter(StatisticsUtil.DAYS_OF_WEEK[dayOfWeek - 1], BooleanFilterType.IS));
            List<Note> notesForDay = weekQuery.execute(dataManager.getNotes());
            wordCountByDayOfWeek.put(dayOfWeek, StatisticsUtil.countWords(notesForDay));
            noteCountByDayOfWeek.put(dayOfWeek, notesForDay.size());
        }
        chunkComplete();
    }

    /**
     * Finds all phrases of length 2-10 across all notes, and organizes them by length for easy access.
     * This operation is surprisingly CPU expensive - more than all the other operations combined in my testing.
     * For that reason, this is the only worker method that invoked chunkComplete() multiple times -
     * once after doing the initial phrase scan, then once again for each phrase length filter.
     * This is accounted for in the totalSteps calculation in run(), so that our progress bar can reflect
     * this extra work.
     */
    void loadPhrases() {
        List<Note> allNotes = dataManager.getNotes();
        allPhrases = StatisticsUtil.findPhrases(allNotes, null);
        phrasesByLength = new HashMap<>();
        chunkComplete();
        for (int length = StatisticsUtil.MIN_PHRASE_LENGTH; length <= StatisticsUtil.MAX_PHRASE_LENGTH; length++) {
            if (wasCanceled) {
                return; // no point in continuing
            }
            phrasesByLength.put(length, allPhrases.filter(10, length));
            chunkComplete();
        }
    }
}
