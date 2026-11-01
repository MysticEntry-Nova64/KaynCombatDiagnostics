package kayn.diagnostics;

import java.io.IOException;

/** Owns recording failures; live snapshots and final archives fail independently. */
final class DiagnosticRecorder {
    static final String LIVE_FILE = "kayn_combat_diagnostics_live.tsv";
    @FunctionalInterface
    interface TextFiles {
        void write(String filename, String contents) throws IOException;
    }

    @FunctionalInterface
    interface FailureReporter {
        void report(String operation, String filename, IOException error);
    }

    private final TextFiles files;
    private final FailureReporter failures;
    private boolean liveFailed;
    private boolean archiveFailed;
    private boolean finalized;

    DiagnosticRecorder(TextFiles files, FailureReporter failures) {
        this.files = files;
        this.failures = failures;
    }

    void beginSession() {
        liveFailed = false;
        finalized = false;
        // Keep a previous archive warning visible until an archive succeeds.
    }

    void writeLive(String snapshot) {
        if (liveFailed) return;
        try {
            files.write(LIVE_FILE, snapshot);
        } catch (IOException error) {
            liveFailed = true;
            failures.report("live snapshot", LIVE_FILE, error);
        }
    }

    void archive(String session, String snapshot) {
        finalized = true;
        String filename = "kayn_combat_diagnostics_" + session + ".tsv";
        try {
            files.write(filename, snapshot);
            archiveFailed = false;
        } catch (IOException error) {
            archiveFailed = true;
            failures.report("archive", filename, error);
        }
    }

    String status() {
        if (archiveFailed) return "Recording: ARCHIVE FAILED (see log)";
        if (finalized) return liveFailed ? "Recording: archived; live file failed" : "Recording: archived";
        if (liveFailed) return "Recording: LIVE FAILED (in memory)";
        return "Recording: live; archive on stop";
    }
}
