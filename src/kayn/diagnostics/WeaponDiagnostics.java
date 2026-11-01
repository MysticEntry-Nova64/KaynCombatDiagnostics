package kayn.diagnostics;

import com.fs.starfarer.api.combat.*;
import java.util.Locale;
import org.lwjgl.util.vector.Vector2f;

/** One weapon's counters, readiness observations and synchronized TSV/HUD readout. */
final class WeaponDiagnostics {
    static final String HEADER = "slot\tweapon\tstate\tfiring\tfiringSeconds\tprojectilesObserved\timpactsObserved\tbeamDamageFrames\taiShouldFire\tautofire\tselectedGroup\ttargetSource\ttargetId\tcentreDistance\tsurfaceDistanceApprox\trange\tpointInArc\tcooldown\tammo\tdisabled\tforceNoFire\tshieldDamageDirect\tarmorDamageDirect\thullDamageDirect\tempDamageDirect\trangeStatus\treadyObserved\tfireLineGeometry\tblockerId\tcontrolMode\tgroupNumber\n";
    private final String slot;
    private final WeaponAPI weapon;
    private long projectiles, impacts, beamFrames;
    private float firingSeconds;
    // Retained for file compatibility; weapon damage attribution is outside this module's purpose.
    private final double[] damage = new double[4];
    private String line = "", state = "WAITING", rangeStatus = "-";
    private String displayName = "", mode = "UNGROUPED";
    private String targetLabel = "UNKNOWN";
    private int groupNumber;
    private boolean firing, venting, observedTarget;
    private FiringLine.Result fireLine = new FiringLine.Result("NO_TARGET", "-");

    WeaponDiagnostics(String slot, WeaponAPI weapon) { this.slot=slot; this.weapon=weapon; }
    void advance(float seconds) { if(weapon.isFiring()) firingSeconds+=seconds; }
    void recordProjectile() { projectiles++; }
    void recordImpact() { impacts++; }
    void recordBeamDamageFrame() { beamFrames++; }
    void recordDamage(double[] values) { for(int i=0;i<damage.length;i++)damage[i]+=values[i]; }
    String line() { return line; }

    void refresh(ShipAPI hunter, CombatEngineAPI engine) {
        WeaponAPI w=weapon;
        WeaponTarget evidence = WeaponTarget.observe(w, hunter, engine);
        WeaponGroupAPI group = evidence.group();
        AutofireAIPlugin ai = evidence.ai();
        CombatEntityAPI target = evidence.entity();
        Vector2f point = evidence.point();
        String targetSource = evidence.source();
        boolean beamContact = evidence.beamContact();
        targetLabel = evidence.label();
        observedTarget = point!=null;
        fireLine = point==null ? new FiringLine.Result("UNKNOWN","-") : FiringLine.trace(w, target, point, engine);
        // Entity range is a collision-radius approximation; beam contact uses the actual contact point.
        Vector2f rangePoint = !beamContact && target!=null ? target.getLocation() : point;
        float centre = rangePoint == null ? Float.NaN : Vector2f.sub(rangePoint, w.getLocation(), null).length();
        float surface = DiagnosticMath.surfaceDistance(centre, beamContact||target==null ? 0 : target.getCollisionRadius());
        boolean arc = point != null && w.distanceFromArc(point) <= 0;
        boolean disabled = w.isDisabled() || w.isForceDisabled();
        venting = hunter.getFluxTracker().isVenting();
        boolean blocked = hunter.getFluxTracker().isOverloaded() || venting;
        boolean wants = evidence.wantsFire();
        boolean rangeMet = DiagnosticMath.inRange(surface, w.getRange());
        rangeStatus = point == null ? "UNKNOWN" : rangeMet ? "MET" : "OUT";
        firing = w.isFiring() || beamContact;
        boolean forcedOff = w.isForceNoFireOneFrame();
        boolean usesAmmo = w.usesAmmo();
        int ammo = usesAmmo ? w.getAmmo() : 1;
        float cooldown = w.getCooldownRemaining();
        float fluxCost = w.getFluxCostToFire();
        float headroom = hunter.getFluxTracker().getMaxFlux()-hunter.getFluxTracker().getCurrFlux();
        WeaponReadiness readiness = new WeaponReadiness(disabled, blocked, forcedOff,
            ammo, cooldown, fluxCost, headroom);
        boolean ready = readiness.ready(evidence, rangeMet, arc);
        state = readiness.state(evidence, firing, rangeMet, arc);
        displayName = w.getDisplayName();
        groupNumber = group==null ? 0 : hunter.getWeaponGroupsCopy().indexOf(group)+1;
        mode = controlMode(hunter, group);
        line = String.join("\t",
            slot, safe(w.getSpec().getWeaponId()), state, text(firing), n(firingSeconds),
            text(projectiles), text(impacts), text(beamFrames), ai == null ? "NA" : text(wants),
            text(group != null && group.isAutofiring()), text(group != null && group == hunter.getSelectedGroupAPI()),
            targetSource, safe(evidence.id()), n(centre), n(surface), n(w.getRange()),
            point == null ? "NA" : text(arc), n(cooldown), usesAmmo ? text(ammo) : "unlimited",
            text(disabled), text(forcedOff), text(damage[0]), text(damage[1]), text(damage[2]), text(damage[3]),
            rangeStatus, text(ready), fireLine.status(), safe(fireLine.blocker()), mode, text(groupNumber)) + "\n";
    }

    void appendPanel(StringBuilder hud) {
        String mark = colorMarker();
        hud.append(mark).append(shortText(slot, 14)).append(" ")
            .append(shortText(displayName, 25)).append(" G").append(groupNumber).append('\n');
        hud.append(mark).append(observedTarget ? "TARGET " + targetLabel : "NO_TARGET")
            .append(" | ").append(activity()).append(" | ").append(mode).append('\n');
        hud.append(mark).append("Range ").append(unknownAsQuestion(rangeStatus))
            .append(" | Line ").append(unknownAsQuestion(fireLine.status())).append(" | ");
        if (weapon.isBeam()) {
            hud.append("Beam ").append(n(firingSeconds)).append("s | Hit frames ").append(beamFrames);
        } else {
            hud.append("Shots/hits ").append(projectiles).append('/').append(impacts);
        }
        hud.append('\n');
    }

    private String colorMarker() {
        if (fireLine.blocked()) return "!";
        if (venting && !firing) return "^";
        if (observedTarget && "OUT".equals(rangeStatus)) return "~";
        return "";
    }

    private String activity() {
        if (firing) return "FIRING";
        if ("NO_TARGET".equals(state) || "TARGET_UNKNOWN".equals(state)) return "IDLE";
        return state;
    }

    private static String unknownAsQuestion(String value) {
        return "UNKNOWN".equals(value) ? "?" : value;
    }

    private static String text(Object value) { return String.valueOf(value); }

    private static String controlMode(ShipAPI ship, WeaponGroupAPI group) {
        if (group == null) return "UNGROUPED";
        if (group == ship.getSelectedGroupAPI()) return "SELECTED";
        return group.isAutofiring() ? "AUTO" : "MANUAL";
    }

    private static String n(float value) { return Float.isFinite(value)?String.format(Locale.ROOT,"%.2f",value):"NA"; }
    private static String safe(String value) { return value==null?"NA":value.replace('\t',' ').replace('\n',' '); }
    private static String shortText(String text,int max) { String s=safe(text);return s.length()>max?s.substring(0,max-1)+"~":s; }
}
