package kayn.diagnostics;

import com.fs.starfarer.api.combat.*;
import java.util.Locale;
import org.lwjgl.util.vector.Vector2f;

/** One weapon's counters, readiness observations and synchronized TSV/HUD readout. */
final class WeaponDiagnostics {
    static final String HEADER = "slot\tweapon\tstate\tfiring\tfiringSeconds\tprojectilesObserved\timpactsObserved\tbeamDamageFrames\taiShouldFire\tautofire\tselectedGroup\ttargetSource\ttargetId\tcentreDistance\tsurfaceDistanceApprox\trange\tpointInArc\tcooldown\tammo\tdisabled\tforceNoFire\tshieldDamageDirect\tarmorDamageDirect\thullDamageDirect\tempDamageDirect\trangeStatus\treadyObserved\tfireLineGeometry\tblockerId\tcontrolMode\tgroupNumber\trestrictions\n";
    private final ShotHistory shots=new ShotHistory();
    private java.util.List<WeaponRestriction> restrictions=java.util.List.of();
    private String targetSourceLabel="UNOBSERVED";
    ShotHistory shots() { return shots; }
    void recordProjectile(DamagingProjectileAPI projectile) {
        recordProjectile();
        // Spawn location is more precise than an estimated barrel index. Never use a moved projectile position.
        shots.add(projectile.getSpawnLocation(),projectile.getFacing());
    }
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
        targetSourceLabel=targetSource;
        boolean beamContact = evidence.beamContact();
        targetLabel = evidence.label();
        observedTarget = point!=null;
        fireLine = point==null ? new FiringLine.Result("UNKNOWN","-") : FiringLine.trace(w, target, point, engine);
        // Entity range is a collision-radius approximation; beam contact uses the actual contact point.
        Vector2f rangePoint = !beamContact && target!=null ? target.getLocation() : point;
        float centre = !FiringLine.finite(rangePoint) || !FiringLine.finite(w.getLocation()) ? Float.NaN : Vector2f.sub(rangePoint, w.getLocation(), null).length();
        float surface = DiagnosticMath.surfaceDistance(centre, beamContact||target==null ? 0 : target.getCollisionRadius());
        float arcDistance=FiringLine.finite(point)&&FiringLine.finite(w.getLocation())?w.distanceFromArc(point):Float.NaN;
        boolean arcKnown=Float.isFinite(arcDistance);
        boolean arc = arcKnown && arcDistance <= 0;
        boolean disabled = w.isDisabled() || w.isForceDisabled();
        venting = hunter.getFluxTracker().isVenting();
        boolean blocked = hunter.getFluxTracker().isOverloaded() || venting;
        boolean wants = evidence.wantsFire();
        boolean rangeMet = DiagnosticMath.inRange(surface, w.getRange());
        boolean rangeKnown=FiringLine.finite(point)&&Float.isFinite(surface)&&Float.isFinite(w.getRange())&&w.getRange()>0;
        rangeStatus = !rangeKnown ? "UNKNOWN" : rangeMet ? "MET" : "OUT";
        firing = w.isFiring() || beamContact;
        boolean forcedOff = w.isForceNoFireOneFrame();
        boolean usesAmmo = w.usesAmmo();
        int ammo = usesAmmo ? w.getAmmo() : 1;
        float cooldown = w.getCooldownRemaining();
        float fluxCost = w.getFluxCostToFire();
        float headroom = hunter.getFluxTracker().getMaxFlux()-hunter.getFluxTracker().getCurrFlux();
        WeaponReadiness readiness = new WeaponReadiness(disabled, blocked, forcedOff,
            ammo, cooldown, fluxCost, headroom);
        restrictions=WeaponRestriction.observe(readiness,evidence,rangeKnown,rangeMet,arcKnown,arc,fireLine,
            venting,hunter.getFluxTracker().isOverloaded());
        boolean ready = rangeKnown && arcKnown && !restrictions.contains(WeaponRestriction.UNKNOWN) && readiness.ready(evidence, rangeMet, arc);
        state = readiness.state(evidence, firing, rangeMet, arc);
        if(!firing && restrictions.contains(WeaponRestriction.UNKNOWN) &&
                ("OUT_OF_RANGE".equals(state)||"OUT_OF_ARC".equals(state)||"READY_OBSERVED".equals(state))) state="UNKNOWN";
        displayName = w.getDisplayName();
        groupNumber = group==null ? 0 : hunter.getWeaponGroupsCopy().indexOf(group)+1;
        mode = controlMode(hunter, group);
        line = String.join("\t",
            slot, safe(w.getSpec().getWeaponId()), state, text(firing), n(firingSeconds),
            text(projectiles), text(impacts), text(beamFrames), ai == null ? "NA" : text(wants),
            text(group != null && group.isAutofiring()), text(group != null && group == hunter.getSelectedGroupAPI()),
            targetSource, safe(evidence.id()), n(centre), n(surface), n(w.getRange()),
            !arcKnown ? "NA" : text(arc), n(cooldown), usesAmmo ? text(ammo) : "unlimited",
            text(disabled), text(forcedOff), text(damage[0]), text(damage[1]), text(damage[2]), text(damage[3]),
            rangeStatus, text(ready), fireLine.status(), safe(fireLine.blocker()), mode, text(groupNumber), restrictions.isEmpty()?"-":restrictions.stream().map(r->r.label).collect(java.util.stream.Collectors.joining("|"))) + "\n";
    }

    void appendPanel(StringBuilder hud) {
        hud.append(shortText(slot,14)).append(" ").append(shortText(displayName,25))
            .append(" G").append(groupNumber).append(" | ").append(activity()).append('\n');
        if(observedTarget) hud.append("TARGET ").append(targetLabel).append(" | ").append(mode)
            .append(" | ").append(targetSourceLabel).append('\n');
        else hud.append(mode).append('\n');
        if(restrictions.isEmpty()) hud.append("Observed restrictions: none\n");
        else {
            int column=0;
            for(WeaponRestriction restriction:restrictions) {
                if(column>0)hud.append("{#ffffff} | ");
                hud.append(restriction.markup());
                // At most four short labels per physical row; panel can scroll on small screens.
                if(++column==4){hud.append('\n');column=0;}
            }
            if(column!=0)hud.append('\n');
        }
        if(weapon.isBeam()) hud.append("Beam ").append(n(firingSeconds)).append("s | Hit frames ").append(beamFrames);
        else hud.append("Projectiles/impacts ").append(projectiles).append('/').append(impacts);
        hud.append('\n');
    }

    private String activity() {
        return firing ? "FIRING" : "IDLE";
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
