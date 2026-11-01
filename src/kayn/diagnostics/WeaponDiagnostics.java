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
    private String displayName = "", mode = "UNGROUPED", aiRequest = "-";
    private String targetLabel = "UNKNOWN";
    private int groupNumber;
    private boolean ready, firing, venting, observedTarget;
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
        WeaponGroupAPI group = hunter.getWeaponGroupFor(w);
        boolean activeAutofire = group != null && group.isAutofiring() && group != hunter.getSelectedGroupAPI();
        AutofireAIPlugin ai = activeAutofire ? group.getAutofirePlugin(w) : null;
        CombatEntityAPI target = ai == null ? null : ai.getTargetShip();
        if (target == null && ai != null) target = ai.getTargetMissile();
        Vector2f point = ai == null ? null : ai.getTarget();
        String targetSource = point == null ? (ai==null?"UNOBSERVED":"NONE") : "AUTOFIRE_AI";
        if (point == null && target != null) { point = target.getLocation(); targetSource = "AI_ENTITY_CENTRE"; }
        boolean hasAiTarget = point != null;
        if(point==null && group!=null && group==hunter.getSelectedGroupAPI()
                && (hunter.getShipAI()!=null || hunter==engine.getPlayerShip()&&engine.isUIAutopilotOn())
                && hunter.getShipTarget()!=null) {
            target=hunter.getShipTarget();point=hunter.getMouseTarget();
            if(point==null)point=target.getLocation();
            targetSource="SHIP_AI_SELECTED_TARGET";
        }
        boolean beamContact = false;
        if (w.isBeam()) for (BeamAPI beam : engine.getBeams()) {
            if (beam.getWeapon()==w && beam.didDamageThisFrame() && beam.getDamageTarget()!=null) {
                target=beam.getDamageTarget();point=beam.getTo();targetSource="BEAM_CONTACT";beamContact=true;break;
            }
        }
        targetLabel = target instanceof MissileAPI?"MISSILE":target instanceof DamagingProjectileAPI?"PROJECTILE":target instanceof ShipAPI?"SHIP":point!=null?"POINT":ai==null?"UNKNOWN":"NONE";
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
        boolean wants = ai != null && ai.shouldFire();
        boolean rangeMet = DiagnosticMath.inRange(surface, w.getRange());
        rangeStatus = point == null ? "UNKNOWN" : rangeMet ? "MET" : "OUT";
        firing = w.isFiring() || beamContact;
        boolean forcedOff = w.isForceNoFireOneFrame();
        boolean usesAmmo = w.usesAmmo();
        int ammo = usesAmmo ? w.getAmmo() : 1;
        float cooldown = w.getCooldownRemaining();
        float fluxCost = w.getFluxCostToFire();
        float headroom = hunter.getFluxTracker().getMaxFlux()-hunter.getFluxTracker().getCurrFlux();
        ready = DiagnosticMath.ready(hasAiTarget,rangeMet,arc,disabled,blocked,forcedOff,
            cooldown,ammo,fluxCost,headroom);
        state = DiagnosticMath.state(firing,disabled,blocked,hasAiTarget,rangeMet,arc,wants);
        if(!firing && !disabled && !blocked && point!=null && !hasAiTarget)
            state=!rangeMet?"OUT_OF_RANGE":!arc?"OUT_OF_ARC":"TARGET_OBSERVED";
        if(!firing && !disabled && !blocked && point==null && ai==null)state="TARGET_UNKNOWN";
        if (!firing && !disabled && !blocked) {
            if (forcedOff) state="FORCED_NO_FIRE";
            else if (usesAmmo && ammo<=0) state="NO_AMMO";
            else if (cooldown>0) state="COOLDOWN";
            else if (fluxCost>headroom) state="FLUX_HEADROOM_LOW";
        }
        displayName = w.getDisplayName();
        groupNumber = group==null ? 0 : hunter.getWeaponGroupsCopy().indexOf(group)+1;
        mode = controlMode(hunter, group);
        aiRequest = ai==null?"-":wants?"YES":"NO";
        line = slot+"\t"+safe(w.getSpec().getWeaponId())+"\t"+state+"\t"+firing+"\t"+n(firingSeconds)
            +"\t"+projectiles+"\t"+impacts+"\t"+beamFrames+"\t"+(ai==null?"NA":wants)
            +"\t"+(group!=null&&group.isAutofiring())+"\t"+(group!=null&&group==hunter.getSelectedGroupAPI())
            +"\t"+targetSource+"\t"+(target==null?"NA":safe(target instanceof ShipAPI ? ((ShipAPI)target).getId() : target instanceof MissileAPI?"MISSILE":target instanceof DamagingProjectileAPI?"PROJECTILE":"ENTITY"))
            +"\t"+n(centre)+"\t"+n(surface)+"\t"+n(w.getRange())+"\t"+(point==null?"NA":arc)
            +"\t"+n(cooldown)+"\t"+(usesAmmo?ammo:"unlimited")+"\t"+disabled+"\t"+forcedOff
            +"\t"+damage[0]+"\t"+damage[1]+"\t"+damage[2]+"\t"+damage[3]+"\t"+rangeStatus+"\t"+ready+"\t"+fireLine.status()+"\t"+safe(fireLine.blocker())+"\t"+mode+"\t"+groupNumber+"\n";
    }

    void appendPanel(StringBuilder hud) {
        String mark=fireLine.blocked()?"!":venting&&!firing?"^":observedTarget&&"OUT".equals(rangeStatus)?"~":"";
        hud.append(mark).append(shortText(slot,14)).append(" ").append(shortText(displayName,25)).append(" G").append(groupNumber).append('\n');
        hud.append(mark).append(observedTarget?"TARGET "+targetLabel:"NO_TARGET");
        String activity = firing?"FIRING":state.equals("NO_TARGET")||state.equals("TARGET_UNKNOWN")?"IDLE":state;
        hud.append(" | ").append(activity).append(" | ").append(mode).append('\n');
        hud.append(mark).append("Range ").append("UNKNOWN".equals(rangeStatus)?"?":rangeStatus).append(" | Line ").append("UNKNOWN".equals(fireLine.status())?"?":fireLine.status()).append(" | ");
        if(weapon.isBeam())hud.append("Beam ").append(n(firingSeconds)).append("s | Hit frames ").append(beamFrames);
        else hud.append("Shots/hits ").append(projectiles).append('/').append(impacts);
        hud.append('\n');
    }

    private static String controlMode(ShipAPI hunter, WeaponGroupAPI group) {
        return group==null?"UNGROUPED":group==hunter.getSelectedGroupAPI()?"SELECTED":group.isAutofiring()?"AUTO":"MANUAL";
    }
    private static String n(float value) { return Float.isFinite(value)?String.format(Locale.ROOT,"%.2f",value):"NA"; }
    private static String safe(String value) { return value==null?"NA":value.replace('\t',' ').replace('\n',' '); }
    private static String shortText(String text,int max) { String s=safe(text);return s.length()>max?s.substring(0,max-1)+"~":s; }
}
