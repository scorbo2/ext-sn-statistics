package ca.corbett.snotes.extensions.statistics;

import ca.corbett.extras.progress.SimpleProgressWorker;
import ca.corbett.snotes.model.Note;

import java.util.List;
import java.util.Set;

/**
 * A very simple thread to wrap a call to findWords() in StatisticsUtil with a given
 * list of specific search words. Cancellation events are ignored by this thread!
 * There's literally only one step, so by the time we receive a cancel message
 * from our dialog, our work would already be complete. It's therefore safe
 * to invoke getResults() even if the user hit cancel.
 * <p>
 * Note that the callback fires on this worker thread! Take care when updating the UI.
 * </p>
 *
 * @author <a href="https://github.com/scorbo2">scorbo2</a>
 * @since Snotes 2.0
 */
public class WordSearchThread extends SimpleProgressWorker {

    private final List<Note> notesToSearch;
    private final Set<String> searchWords;
    private WordList results;

    public WordSearchThread(List<Note> allNotes, Set<String> searchWords) {
        this.notesToSearch = allNotes;
        this.searchWords = searchWords;
    }

    /**
     * Will return our search results, or null if the search has not yet been performed
     * or has not yet completed.
     */
    public WordList getResults() {
        return results;
    }

    @Override
    public void run() {
        this.results = null;
        fireProgressBegins(2);
        fireProgressUpdate(1, "Searching for words...");
        try {
            this.results = StatisticsUtil.findWords(notesToSearch, searchWords, StatisticsUtil.TOP_N_WORDS);
        }
        finally {
            fireProgressComplete(); // even if canceled, doesn't matter, just fire complete.
        }
    }
}
