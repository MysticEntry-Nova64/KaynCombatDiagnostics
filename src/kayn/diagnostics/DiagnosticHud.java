package kayn.diagnostics;

/** HUD-only formatting; does not observe or change combat state. */
final class DiagnosticHud {
    static String title(float seconds, String recording) {
        return "COMBAT DIAGNOSTICS | "+Math.round(seconds)+"s | "+recording+"\n";
    }
    static String ship(String name, boolean selected, int hull, int flux, int maxFlux,
            int weapons, int page, int pages) {
        return name+(selected?" | Selected ship":" | Your ship")+"\n"+
            "Hull "+hull+" | Flux "+flux+" / "+maxFlux+" | Weapons "+weapons+
            (pages>1?" | Page "+(page+1)+"/"+pages:"")+"\n";
    }
    static String help(DiagnosticKeys keys) {
        return "All shortcuts use Ctrl\n"+
        keys.name(DiagnosticKeys.Action.TOGGLE)+": recording on/off; "+keys.name(DiagnosticKeys.Action.SUBJECT)+": selected target / controlled ship\n"+
        keys.name(DiagnosticKeys.Action.PAGE)+": next page; "+keys.name(DiagnosticKeys.Action.UP)+"/"+keys.name(DiagnosticKeys.Action.DOWN)+": scroll; "+keys.name(DiagnosticKeys.Action.HELP)+": close help\n"+
        "Reports archive on stop. Timer and evidence age use simulation time.\n"+
        "Paused data is cached. Restrictions are observations, not AI reasons.\n"+
        "Vectors: last 5 observed projectiles per weapon; green to black.\n";
    }
}
