package kayn.diagnostics;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Combat-thread recording; pending reports retain text only, never game entities. */
final class DiagnosticRecorder {
    static final String LIVE_FILE = "kayn_combat_diagnostics_live.tsv";
    static final int MAX_PENDING = 8;
    static final int MAX_PENDING_CHARS = 524288; // At most 1 MiB of UTF-16 character payload.
    static final int MAX_ATTEMPTS = 5; // Initial archive write plus four automatic retries.
    static final float RETRY_SECONDS = 5;

    @FunctionalInterface interface TextFiles {
        void write(String filename, String contents) throws IOException;
    }
    @FunctionalInterface interface FailureReporter {
        void report(String operation, String filename, Exception error);
    }
    private static final class Pending {
        final String snapshot;
        int attempts = 1;
        Pending(String snapshot) { this.snapshot = snapshot; }
    }

    private final TextFiles files;
    private final FailureReporter failures;
    private final Map<String, Pending> pending = new LinkedHashMap<>();
    private int pendingChars;
    private float retryElapsed;
    private boolean liveFailed, finalized, unqueuedFailure;

    DiagnosticRecorder(TextFiles files, FailureReporter failures) {
        this.files = files;
        this.failures = failures;
    }

    void beginSession() {
        liveFailed = false;
        finalized = false;
        // Pending archives and overflow warnings belong to this combat, not this subject.
    }

    /** An explicit user re-enable grants exhausted reports another bounded retry cycle. */
    void retryPendingArchives() {
        for (Pending report : pending.values()) {
            if (report.attempts >= MAX_ATTEMPTS) report.attempts = 0;
        }
    }

    void writeLive(String snapshot) {
        if (liveFailed) return;
        try {
            files.write(LIVE_FILE, snapshot);
        } catch (IOException | RuntimeException error) {
            liveFailed = true;
            report("live snapshot", LIVE_FILE, error);
        }
    }

    void archive(String session, String snapshot) {
        finalized = true;
        String filename = "kayn_combat_diagnostics_" + session + ".tsv";
        Pending existing = pending.get(filename);
        // Never replace a queued session's preserved report with newer subject data.
        String contents = existing == null ? snapshot : existing.snapshot;
        try {
            files.write(filename, contents);
            remove(filename);
        } catch (IOException | RuntimeException error) {
            if (existing == null) {
                if (pending.size() < MAX_PENDING && contents.length() <= MAX_PENDING_CHARS - pendingChars) {
                    pending.put(filename, new Pending(contents));
                    pendingChars += contents.length();
                    report("archive (queued for retry)", filename, error);
                } else {
                    unqueuedFailure = true;
                    report("archive (retry queue full; report not queued)", filename, error);
                }
            } else {
                report("archive", filename, error);
            }
        }
    }

    /** Caller supplies unpaused simulation time, including while collection is off. */
    void advance(float seconds) {
        if (pending.isEmpty() || !Float.isFinite(seconds) || seconds <= 0) return;
        retryElapsed += seconds;
        if (retryElapsed < RETRY_SECONDS) return;
        retryElapsed = 0;
        // One write per interval; rotate failures so one bad file cannot starve others.
        String filename = null;
        for (Map.Entry<String, Pending> entry : pending.entrySet()) {
            if (entry.getValue().attempts < MAX_ATTEMPTS) { filename = entry.getKey(); break; }
        }
        if (filename == null) return;
        Pending report = pending.get(filename);
        report.attempts++;
        try {
            files.write(filename, report.snapshot);
            remove(filename);
        } catch (IOException | RuntimeException error) {
            pending.remove(filename);
            pending.put(filename, report);
            if (report.attempts == MAX_ATTEMPTS) report("archive (retries exhausted; retained in memory)", filename, error);
        }
    }

    private void remove(String filename) {
        Pending removed = pending.remove(filename);
        if (removed != null) pendingChars -= removed.snapshot.length();
        if (pending.isEmpty()) retryElapsed = 0;
    }

    private void report(String operation, String filename, Exception error) {
        // Notification failure must not interrupt archiving; status remains observable.
        try { failures.report(operation, filename, error); } catch (RuntimeException ignored) { }
    }

    String status() {
        if (unqueuedFailure) return "Recording: ARCHIVE FAILED (queue limit; see log)";
        if (!pending.isEmpty()) {
            long exhausted = pending.values().stream().filter(p -> p.attempts >= MAX_ATTEMPTS).count();
            return "Recording: ARCHIVE FAILED (" + pending.size() + " pending, " + exhausted + " exhausted)";
        }
        if (finalized) return liveFailed ? "Recording: archived; live file failed" : "Recording: archived";
        if (liveFailed) return "Recording: LIVE FAILED (in memory)";
        return "Recording: live; archive on stop";
    }
}
