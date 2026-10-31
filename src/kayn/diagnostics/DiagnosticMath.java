package kayn.diagnostics;

/** Geometric observations, not a reconstruction of the firing AI. */
public final class DiagnosticMath {
    private DiagnosticMath() {}
    public static float surfaceDistance(float centre, float radius) { return Math.max(0, centre-radius); }
    public static boolean inRange(float distance, float range) { return Float.isFinite(distance) && distance <= range; }
    public static boolean ready(boolean target, boolean range, boolean arc, boolean disabled, boolean blocked,
                                boolean forcedOff, float cooldown, int ammo, float fluxCost, float headroom) {
        return target && range && arc && !disabled && !blocked && !forcedOff && cooldown <= 0
            && ammo > 0 && fluxCost <= headroom;
    }
    public static String state(boolean firing, boolean disabled, boolean blocked, boolean target,
                               boolean range, boolean arc, boolean wantsFire) {
        if (firing) return "FIRING";
        if (disabled) return "DISABLED";
        if (blocked) return "SHIP_BLOCKED";
        if (!target) return "NO_AI_TARGET";
        if (!range) return "OUT_OF_RANGE";
        if (!arc) return "OUT_OF_ARC";
        return wantsFire ? "READY_OBSERVED" : "AI_HOLD";
    }
}
