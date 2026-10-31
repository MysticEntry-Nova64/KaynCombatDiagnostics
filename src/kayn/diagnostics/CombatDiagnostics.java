package kayn.diagnostics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Combat-local, opt-in observer. No firing, targeting, damage or ship-state mutations. */
public class CombatDiagnostics extends BaseEveryFrameCombatPlugin {
    public static final String KEY = "kayn.combat.diagnostics.v1";
    public static final String FILE = "kayn_combat_diagnostics_live.tsv";
    private CombatEngineAPI engine;
    private ShipAPI hunter;
    private final String scenario;
    private final Map<String, Row> rows = new TreeMap<String, Row>();
    private final Map<ShipAPI, DamageListener> listeners = new IdentityHashMap<ShipAPI, DamageListener>();
    private Set<DamagingProjectileAPI> previous = identitySet();
    private final Set<DamagingProjectileAPI> hitSeen = identitySet();
    private float elapsed, refresh, disk;
    private boolean finished, ioFailed;
    public boolean overlay = true;
    private String snapshot = "Diagnostics off";
    private String panelText = "Diagnostics off - Ctrl+F8 to enable";
    private DiagnosticPanel panel;
    private final double[] damage = new double[4];
    private final double[] unknown = new double[4];
    private String session = UUID.randomUUID().toString();
    private boolean enabled, targetMode;
    private ShipAPI pinned;
    private int page;
    private String stopReason = "";
    private final Map<WeaponAPI, Row> weaponRows = new IdentityHashMap<>();
    public CombatDiagnostics() { this("combat"); }
    public CombatDiagnostics(String scenario) { this.scenario = scenario; }
    private static <T> Set<T> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>()); }
    static class Row {
        WeaponAPI weapon;
        long projectiles, impacts, beamFrames;
        float firingSeconds;
        final double[] damage = new double[4];
        String line = "", state = "WAITING";
        String rangeStatus = "-";
        boolean ready;
        FiringLine.Result fireLine = new FiringLine.Result("NO_TARGET", "-");
    }
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        engine.getCustomData().put(KEY, this);
    }
    public String snapshot(String slot) {
        if (slot == null || slot.isEmpty()) return snapshot;
        Row row = rows.get(slot);
        return row == null ? "Unknown or not deployed slot: " + slot : header() + row.line;
    }
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        if(engine==null || engine.isUIShowingDialog() || !engine.isUIShowingHUD()) return;
        for(InputEventAPI e:events) {
            if(e.isConsumed() || !e.isKeyDownEvent() || e.isRepeat() || !e.isCtrlDown() || e.isAltDown() || e.isShiftDown()) continue;
            int key=e.getEventValue();
            if(key==org.lwjgl.input.Keyboard.KEY_F8) {
                enabled=!enabled;
                if(!enabled) { finish("disabled"); hunter=null; pinned=null; targetMode=false; }
                else { finished=false; stopReason=""; }
                e.consume();
            } else if(enabled && key==org.lwjgl.input.Keyboard.KEY_F9) {
                ShipAPI player=engine.getPlayerShip();
                if(targetMode) { targetMode=false; pinned=null; }
                else if(player!=null && player.getShipTarget()!=null) { targetMode=true; pinned=player.getShipTarget(); }
                e.consume();
            } else if(enabled && key==org.lwjgl.input.Keyboard.KEY_F10) {
                page=(page+1)%Math.max(1,(rows.size()+5)/6); if(hunter!=null) buildSnapshot();e.consume();
            }
        }
    }
    private void start(ShipAPI ship) {
        if(hunter!=null) finish("ship changed");
        hunter=ship;finished=false;ioFailed=false;stopReason="";elapsed=refresh=disk=0;page=0;
        session=UUID.randomUUID().toString();rows.clear();weaponRows.clear();
        Arrays.fill(damage,0);Arrays.fill(unknown,0);detach();
        for(WeaponAPI w:ship.getAllWeapons()) {
            if(w.isDecorative()) continue;
            String base=w.getSlot()==null?"weapon":safe(w.getSlot().getId()), id=base;
            for(int i=2;rows.containsKey(id);i++)id=base+"#"+i;
            Row r=new Row();r.weapon=w;rows.put(id,r);weaponRows.put(w,r);
        }
        // Shots already in flight predate this measurement session.
        for(DamagingProjectileAPI p:engine.getProjectiles()) if(p.getSource()==hunter) {previous.add(p);hitSeen.add(p);}
        for(DamagingProjectileAPI p:engine.getMissiles()) if(p.getSource()==hunter) {previous.add(p);hitSeen.add(p);}
        buildSnapshot();writeSnapshot();
    }
    private void finish(String reason) {
        if(hunter==null || finished) return;
        finished=true;stopReason=reason;buildSnapshot();writeSnapshot();detach();
        if(!ioFailed)try {Global.getSettings().writeTextFileToCommon("kayn_combat_diagnostics_"+session+".tsv",snapshot);}
        catch(java.io.IOException ex){ioFailed=true;}
    }
    public void advance(float amount, List<InputEventAPI> events) {
        if(engine==null || !enabled) return;
        ShipAPI desired=targetMode?pinned:engine.getPlayerShip();
        if(desired!=hunter) {
            if(desired!=null) start(desired);
            else {finish("no controlled ship");hunter=null;}
        }
        if(hunter==null) return;
        if(!finished && engine.isCombatOver()) {finish("combat ended");return;}
        if(!finished && !engine.isPaused()) {
            if(!engine.isEntityInPlay(hunter) || !hunter.isAlive()) {finish("ship lost");return;}
            elapsed+=amount;refresh+=amount;disk+=amount;updateListeners();observeProjectiles();
            for(Row r:rows.values())if(r.weapon.isFiring())r.firingSeconds+=amount;
            for(BeamAPI beam:engine.getBeams()){Row r=rowFor(beam.getWeapon());if(r!=null&&beam.didDamageThisFrame())r.beamFrames++;}
            if(refresh>=.25f){refresh=0;buildSnapshot();}
            if(disk>=1){disk=0;writeSnapshot();}
        }
    }
    public void renderInUICoords(ViewportAPI viewport) {
        if (!overlay || !enabled || engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog()) return;
        if (panel == null) panel = new DiagnosticPanel();
        panel.draw(hunter==null?"COMBAT DIAGNOSTICS | Waiting for your ship\nCtrl+F8: off":panelText, engine.isPaused());
    }
    private Row rowFor(WeaponAPI weapon) {
        return weaponRows.get(weapon);
    }
    private void observeProjectiles() {
        Set<DamagingProjectileAPI> current = identitySet();
        current.addAll(engine.getProjectiles()); current.addAll(engine.getMissiles());
        current.removeIf(p -> p.getSource() != hunter);
        for (DamagingProjectileAPI p : current) {
            Row row = rowFor(p.getWeapon());
            if (row != null && !previous.contains(p)) row.projectiles++;
            observeImpact(p);
        }
        for (DamagingProjectileAPI p : previous) if (!current.contains(p)) observeImpact(p);
        // Retain only active identities; removed projectiles were checked for a final impact above.
        hitSeen.retainAll(current); previous = current;
    }
    private void observeImpact(DamagingProjectileAPI p) {
        if (!p.didDamage() || p.getDamageTarget() == null || !hitSeen.add(p)) return;
        Row row = rowFor(p.getWeapon()); if (row != null) row.impacts++;
    }
    private void updateListeners() {
        for (Iterator<Map.Entry<ShipAPI, DamageListener>> it = listeners.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ShipAPI, DamageListener> e = it.next();
            if (!engine.isEntityInPlay(e.getKey())) { e.getKey().removeListener(e.getValue()); it.remove(); }
        }
        for (ShipAPI ship : engine.getShips()) {
            if (listeners.containsKey(ship)) continue;
            DamageListener listener = new DamageListener() {
                public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
                    if (finished || result == null) return;
                    WeaponAPI weapon = null;
                    ShipAPI responsible = source instanceof ShipAPI ? (ShipAPI) source : null;
                    if (source instanceof DamagingProjectileAPI) {
                        DamagingProjectileAPI p = (DamagingProjectileAPI) source;
                        responsible = p.getSource(); weapon = p.getWeapon();
                    } else if (source instanceof BeamAPI) {
                        BeamAPI beam = (BeamAPI) source;
                        weapon = beam.getWeapon(); responsible = weapon == null ? null : weapon.getShip();
                    }
                    if (responsible != hunter || target == hunter) return;
                    Row row = rowFor(weapon);
                    double[] bucket = row == null ? unknown : row.damage;
                    double[] values = {result.getDamageToShields(), result.getTotalDamageToArmor(), result.getDamageToHull(), result.getEmpDamage()};
                    for (int i=0; i<4; i++) { damage[i] += values[i]; bucket[i] += values[i]; }
                }
            };
            ship.addListener(listener); listeners.put(ship, listener);
        }
    }
    private void detach() {
        for (Map.Entry<ShipAPI, DamageListener> e : listeners.entrySet()) e.getKey().removeListener(e.getValue());
        listeners.clear(); previous.clear(); hitSeen.clear();
    }
    private String header() {
        return "slot\tweapon\tstate\tfiring\tfiringSeconds\tprojectilesObserved\timpactsObserved\tbeamDamageFrames\taiShouldFire\tautofire\tselectedGroup\ttargetSource\ttargetId\tcentreDistance\tsurfaceDistanceApprox\trange\tpointInArc\tcooldown\tammo\tdisabled\tforceNoFire\tshieldDamageDirect\tarmorDamageDirect\thullDamageDirect\tempDamageDirect\trangeStatus\treadyObserved\tfireLineGeometry\tblockerId\tcontrolMode\tgroupNumber\n";
    }
    private static String n(float value) { return Float.isFinite(value) ? String.format(Locale.ROOT, "%.2f", value) : "NA"; }
    private static String safe(String value) { return value == null ? "NA" : value.replace('\t',' ').replace('\n',' '); }
    private void buildSnapshot() {
        StringBuilder out = new StringBuilder("# Kayn combat diagnostics v1\n# session=" + session + " scenario=" + scenario
            + " ship=" + safe(hunter.getId()) + " hull=" + safe(hunter.getHullSpec().getHullId()) + " reason=" + stopReason.replace(' ','_') + " seconds=" + n(elapsed) + " ended=" + finished + " capturedUtc=" + java.time.Instant.now() + "\n");
        out.append("# totals shield/armor/hull/EMP=").append(Arrays.toString(damage))
           .append(" unassignedToSlot=").append(Arrays.toString(unknown)).append('\n');
        out.append("# hp=").append(hunter.getHitpoints()).append(" flux=").append(hunter.getFluxLevel())
           .append(" overloaded=").append(hunter.getFluxTracker().isOverloaded()).append(" venting=")
           .append(hunter.getFluxTracker().isVenting()).append('\n');
        out.append("# State labels describe observations, not proven reasons. Range uses collision radius; firing line is instantaneous geometry, not AI permission or missile trajectory.\n");
        out.append(header());
        for (Map.Entry<String, Row> entry : rows.entrySet()) {
            Row row = entry.getValue(); WeaponAPI w = row.weapon;
            WeaponGroupAPI group = hunter.getWeaponGroupFor(w);
            AutofireAIPlugin ai = group == null ? null : group.getAutofirePlugin(w);
            CombatEntityAPI target = ai == null ? null : ai.getTargetShip();
            if (target == null && ai != null) target = ai.getTargetMissile();
            Vector2f point = ai == null ? null : ai.getTarget();
            String targetSource = point == null ? "NONE" : "AUTOFIRE_AI";
            if (point == null && target != null) { point = target.getLocation(); targetSource = "AI_ENTITY_CENTRE"; }
            boolean hasAiTarget = point != null;
            if (point == null) {
                float closest = Float.POSITIVE_INFINITY;
                for (ShipAPI candidate : engine.getShips()) {
                    if (candidate.getOwner() == hunter.getOwner() || candidate.getOwner() == 100 || !candidate.isAlive()) continue;
                    float distance = Vector2f.sub(candidate.getLocation(), w.getLocation(), null).lengthSquared();
                    if (distance < closest) { closest = distance; target = candidate; }
                }
                if (target != null) { point = target.getLocation(); targetSource = "NEAREST_ENEMY_CANDIDATE_NOT_AI_TARGET"; }
            }
            row.fireLine = FiringLine.trace(w, target, point, engine);
            float centre = point == null ? Float.NaN : Vector2f.sub(point, w.getLocation(), null).length();
            float surface = DiagnosticMath.surfaceDistance(centre, target == null ? 0 : target.getCollisionRadius());
            boolean arc = point != null && w.distanceFromArc(point) <= 0;
            boolean disabled = w.isDisabled() || w.isForceDisabled();
            boolean blocked = hunter.getFluxTracker().isOverloaded() || hunter.getFluxTracker().isVenting();
            boolean wants = ai != null && ai.shouldFire();
            boolean rangeMet = DiagnosticMath.inRange(surface, w.getRange());
            row.rangeStatus = point == null ? "-" : rangeMet ? "MET" : "OUT";
            row.ready = DiagnosticMath.ready(hasAiTarget,rangeMet,arc,disabled,blocked,w.isForceNoFireOneFrame(),
                w.getCooldownRemaining(),w.usesAmmo()?w.getAmmo():1,w.getFluxCostToFire(),
                hunter.getFluxTracker().getMaxFlux()-hunter.getFluxTracker().getCurrFlux());
            row.state = DiagnosticMath.state(w.isFiring(), disabled, blocked, hasAiTarget,
                DiagnosticMath.inRange(surface,w.getRange()),arc,wants);
            if (!w.isFiring() && !disabled && !blocked && w.usesAmmo() && w.getAmmo()==0) row.state="NO_AMMO";
            row.line = entry.getKey()+"\t"+safe(w.getSpec().getWeaponId())+"\t"+row.state+"\t"+w.isFiring()+"\t"+n(row.firingSeconds)
                +"\t"+row.projectiles+"\t"+row.impacts+"\t"+row.beamFrames+"\t"+(ai==null?"NA":wants)
                +"\t"+(group!=null&&group.isAutofiring())+"\t"+(group!=null&&group==hunter.getSelectedGroupAPI())
                +"\t"+targetSource+"\t"+(target==null?"NA":safe(target instanceof ShipAPI ? ((ShipAPI)target).getId() : "MISSILE"))
                +"\t"+n(centre)+"\t"+n(surface)+"\t"+n(w.getRange())+"\t"+(point==null?"NA":arc)
                +"\t"+n(w.getCooldownRemaining())+"\t"+(w.usesAmmo()?w.getAmmo():"unlimited")+"\t"+disabled+"\t"+w.isForceNoFireOneFrame()
                +"\t"+row.damage[0]+"\t"+row.damage[1]+"\t"+row.damage[2]+"\t"+row.damage[3]+"\t"+row.rangeStatus+"\t"+row.ready+"\t"+row.fireLine.status()+"\t"+safe(row.fireLine.blocker())+"\t"+controlMode(group)+"\t"+(hunter.getWeaponGroupsCopy().indexOf(group)+1)+"\n";
            out.append(row.line);
        }
        snapshot = out.toString();
        StringBuilder hud = new StringBuilder("COMBAT DIAGNOSTICS | "+(finished?"FINAL":"LIVE")+" | "+Math.round(elapsed)+"s\n");
        hud.append(shortText(hunter.getHullSpec().getHullName(),34)).append(targetMode?" [TARGET]":" [YOUR SHIP]").append('\n');
        hud.append("Hull ").append(Math.round(hunter.getHitpoints())).append("  Flux ").append(Math.round(hunter.getFluxTracker().getCurrFlux())).append(" / ").append(Math.round(hunter.getFluxTracker().getMaxFlux())).append('\n');
        hud.append("Damage S/A/H: ").append(Math.round(damage[0])).append('/').append(Math.round(damage[1])).append('/').append(Math.round(damage[2])).append(" (raw)\n");
        hud.append("Ctrl+F8 on/off | F9 target/self | F10 page\n");
        int pages=Math.max(1,(rows.size()+5)/6);page=Math.min(page,pages-1);
        hud.append("Weapons ").append(rows.size()).append(" | page ").append(page+1).append('/').append(pages).append(" (all keys use Ctrl)\n\n");
        int index=0;
        for(Map.Entry<String,Row> e:rows.entrySet()) {
            int i=index++;if(i<page*6||i>=(page+1)*6)continue;
            Row r=e.getValue();WeaponGroupAPI g=hunter.getWeaponGroupFor(r.weapon);
            AutofireAIPlugin ai=g==null?null:g.getAutofirePlugin(r.weapon);
            String mark=r.fireLine.blocked()?"!":"";
            hud.append(mark).append(shortText(e.getKey(),14)).append(" ").append(shortText(r.weapon.getDisplayName(),25)).append(" G").append(hunter.getWeaponGroupsCopy().indexOf(g)+1).append('\n');
            hud.append(mark).append(r.weapon.isFiring()?"FIRING":r.state).append(" | ").append(controlMode(g)).append(" | AI ").append(ai==null?"-":ai.shouldFire()?"YES":"NO").append('\n');
            hud.append(mark).append("Range ").append(r.rangeStatus).append(" | Line ").append(r.fireLine.status()).append(" | ");
            if(r.weapon.isBeam())hud.append("Beam ").append(n(r.firingSeconds)).append("s");
            else hud.append("Shots/hits ").append(r.projectiles).append('/').append(r.impacts);
            hud.append('\n');
        }
        panelText = hud.toString();
    }
    private String controlMode(WeaponGroupAPI group) {
        return group==null?"UNGROUPED":group==hunter.getSelectedGroupAPI()?"SELECTED":group.isAutofiring()?"AUTO":"MANUAL";
    }
    private static String shortText(String text,int max) {String s=safe(text);return s.length()>max?s.substring(0,max-1)+"~":s;}
    private void writeSnapshot() {
        if (ioFailed) return;
        try { Global.getSettings().writeTextFileToCommon(FILE, snapshot); }
        catch (java.io.IOException ex) {
            ioFailed = true;
            Global.getLogger(CombatDiagnostics.class).error("KAYN diagnostics file disabled after write failure", ex);
        }
    }
}
