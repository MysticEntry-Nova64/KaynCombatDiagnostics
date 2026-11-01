package kayn.diagnostics;

import java.util.Locale;

/** Presentation status is calculated separately from the frozen evidence. */
final class ObservationFreshness {
    enum State {
        LIVE("LIVE"), PAUSED("PAUSED"), STALE("STALE"), FINAL("FINAL / STALE"), FAILED("STALE / FAILED");
        final String label;
        State(String label){this.label=label;}
    }
    static State state(boolean paused,boolean finished,boolean failed,float age) {
        return failed?State.FAILED:finished?State.FINAL:paused?State.PAUSED:age>.5f?State.STALE:State.LIVE;
    }
    static String label(boolean paused, boolean finished, boolean failed, float age) {
        return String.format(Locale.ROOT,"%s | Evidence age %.2fs",state(paused,finished,failed,age).label,Math.max(0,age));
    }
}
