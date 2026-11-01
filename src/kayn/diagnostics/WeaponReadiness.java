package kayn.diagnostics;

/** Measured restrictions and their explicit display priority, separate from target discovery. */
record WeaponReadiness(boolean disabled, boolean shipBlocked, boolean forcedOff,
                       int ammo, float cooldown, float fluxCost, float headroom) {
    boolean ready(WeaponTarget target, boolean rangeMet, boolean inArc) {
        return DiagnosticMath.ready(target.individualAiTarget(), rangeMet, inArc,
            disabled, shipBlocked, forcedOff, cooldown, ammo, fluxCost, headroom);
    }

    String state(WeaponTarget target, boolean firing, boolean rangeMet, boolean inArc) {
        if (firing) return "FIRING";
        if (disabled) return "DISABLED";
        if (shipBlocked) return "SHIP_BLOCKED";
        if (forcedOff) return "FORCED_NO_FIRE";
        if (ammo <= 0) return "NO_AMMO";
        if (cooldown > 0) return "COOLDOWN";
        if (fluxCost > headroom) return "FLUX_HEADROOM_LOW";
        if (!target.observed()) return target.ai() == null ? "TARGET_UNKNOWN" : "NO_TARGET";
        if (!rangeMet) return "OUT_OF_RANGE";
        if (!inArc) return "OUT_OF_ARC";
        if (!target.individualAiTarget()) return "TARGET_OBSERVED";
        return target.wantsFire() ? "READY_OBSERVED" : "AI_HOLD";
    }
}
