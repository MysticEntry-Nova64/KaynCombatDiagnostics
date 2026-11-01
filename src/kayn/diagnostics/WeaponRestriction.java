package kayn.diagnostics;

import java.awt.Color;
import java.util.*;

/** Independent observations, never commands or inferred private AI decisions. */
enum WeaponRestriction {
    BLOCKED("Blocked", 0xffa040), DISTANCE("Distance", 0xffe060),
    FLUXING("Fluxing", 0x40e8ff), NO_TARGET("NoTarget", 0xb0b0b0),
    DAMAGED("Damaged", 0xff6060), COOLDOWN("Cooldown", 0x80b8ff),
    NO_AMMO("NoAmmo", 0xff6060), HOLD("Hold", 0xd090ff),
    ARC("Arc", 0xffc040), OVERLOAD("Overload", 0xff6060),
    UNKNOWN("Unknown", 0xb0b0b0), AI_HOLD("AIHold", 0xd090ff);

    final String label;
    final Color color;
    WeaponRestriction(String label, int rgb) { this.label=label; this.color=new Color(rgb); }
    String markup() { return String.format(Locale.ROOT,"{#%06x}%s",color.getRGB() & 0xffffff,label); }

    static List<WeaponRestriction> observe(WeaponReadiness r, WeaponTarget target,
            boolean rangeKnown, boolean rangeMet, boolean arcKnown, boolean inArc,
            FiringLine.Result geometry, boolean venting, boolean overloaded) {
        EnumSet<WeaponRestriction> found=EnumSet.noneOf(WeaponRestriction.class);
        if(geometry.blocked()) found.add(BLOCKED);
        if(rangeKnown && !rangeMet) found.add(DISTANCE);
        if(venting || Float.isFinite(r.fluxCost()) && Float.isFinite(r.headroom()) && r.fluxCost()>r.headroom()) found.add(FLUXING);
        if(!target.observed()) found.add(NO_TARGET);
        if(r.disabled()) found.add(DAMAGED);
        if(r.cooldown()>0) found.add(COOLDOWN);
        if(r.ammo()<=0) found.add(NO_AMMO);
        if(r.forcedOff()) found.add(HOLD);
        if(target.individualAiTarget() && !target.wantsFire()) found.add(AI_HOLD);
        if(arcKnown && !inArc) found.add(ARC);
        if(overloaded) found.add(OVERLOAD);
        if(target.observed() && (!rangeKnown || !arcKnown || "UNKNOWN".equals(geometry.status())) ||
                !Float.isFinite(r.cooldown()) || !Float.isFinite(r.fluxCost()) || !Float.isFinite(r.headroom())) found.add(UNKNOWN);
        return List.copyOf(found);
    }
}
