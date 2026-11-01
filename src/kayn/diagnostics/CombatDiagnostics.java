package kayn.diagnostics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.*;

/** Combat-local, opt-in observer. No firing, targeting, damage or ship-state mutations. */
public class CombatDiagnostics extends BaseEveryFrameCombatPlugin {
    public static final String KEY = "kayn.combat.diagnostics.v1";
    public static final String FILE = DiagnosticRecorder.LIVE_FILE;
    private CombatEngineAPI engine;
    private ShipAPI hunter;
    private final String scenario;
    private final Map<String, WeaponDiagnostics> rows = new TreeMap<String, WeaponDiagnostics>();
    private final Map<ShipAPI, DamageListener> listeners = new IdentityHashMap<ShipAPI, DamageListener>();
    private Set<DamagingProjectileAPI> previous = identitySet();
    private final Set<DamagingProjectileAPI> hitSeen = identitySet();
    private float elapsed, refresh, disk;
    private boolean finished;
    private DiagnosticRecorder recording;
    public boolean overlay = true;
    private String snapshot = "Diagnostics off";
    private String panelHeader = "", panelRows = "";
    private DiagnosticPanel panel;
    private final double[] damage = new double[4];
    private final double[] unknown = new double[4];
    private String session = UUID.randomUUID().toString();
    private boolean enabled, targetMode, combatEnded;
    private ShipAPI pinned;
    private int page;
    private String stopReason = "";
    private final Map<WeaponAPI, WeaponDiagnostics> weaponRows = new IdentityHashMap<>();
    public CombatDiagnostics() { this("combat"); }
    public CombatDiagnostics(String scenario) { this.scenario = scenario; }
    private static <T> Set<T> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>()); }
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        recording = new DiagnosticRecorder(
            (filename, contents) -> Global.getSettings().writeTextFileToCommon(filename, contents),
            (operation, filename, error) -> {
                String message = "KAYN diagnostics " + operation + " failed: " + filename;
                Global.getLogger(CombatDiagnostics.class).error(message, error);
                try {
                    if (engine.getCombatUI() != null) {
                        engine.getCombatUI().addMessage(0, java.awt.Color.ORANGE, message);
                    }
                } catch (RuntimeException notificationError) {
                    Global.getLogger(CombatDiagnostics.class).warn("KAYN diagnostics could not display recording warning", notificationError);
                }
            });
        engine.getCustomData().put(KEY, this);
    }
    public String snapshot(String slot) {
        if (slot == null || slot.isEmpty()) return snapshot;
        WeaponDiagnostics row = rows.get(slot);
        return row == null ? "Unknown or not deployed slot: " + slot : WeaponDiagnostics.HEADER + row.line();
    }
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        if(engine==null || combatEnded || engine.isCombatOver() || engine.isUIShowingDialog() || !engine.isUIShowingHUD()) return;
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
        hunter=ship;finished=false;recording.beginSession();stopReason="";elapsed=refresh=disk=0;page=0;
        session=UUID.randomUUID().toString();rows.clear();weaponRows.clear();
        Arrays.fill(damage,0);Arrays.fill(unknown,0);detach();
        for(WeaponAPI w:ship.getAllWeapons()) {
            if(w.isDecorative()) continue;
            String base=w.getSlot()==null?"weapon":safe(w.getSlot().getId()), id=base;
            for(int i=2;rows.containsKey(id);i++)id=base+"#"+i;
            WeaponDiagnostics r=new WeaponDiagnostics(id,w);rows.put(id,r);weaponRows.put(w,r);
        }
        // Shots already in flight predate this measurement session.
        for(DamagingProjectileAPI p:engine.getProjectiles()) if(p.getSource()==hunter) {previous.add(p);hitSeen.add(p);}
        for(DamagingProjectileAPI p:engine.getMissiles()) if(p.getSource()==hunter) {previous.add(p);hitSeen.add(p);}
        buildSnapshot();writeSnapshot();
    }
    private void finish(String reason) {
        if(hunter==null || finished) return;
        finished=true;stopReason=reason;
        try {
            buildSnapshot();
            writeSnapshot();
            recording.archive(session, snapshot);
        } finally {
            detach();
        }
    }
    public void advance(float amount, List<InputEventAPI> events) {
        if(engine==null || combatEnded) return;
        // Battle completion is terminal, even if the controlled ship changes or collection is off.
        if(engine.isCombatOver()) {combatEnded=true;finish("combat ended");return;}
        if(!enabled) return;
        ShipAPI desired=targetMode?pinned:engine.getPlayerShip();
        if(desired!=hunter) {
            if(desired!=null) start(desired);
            else {finish("no controlled ship");hunter=null;}
        }
        if(hunter==null) return;
        if(!finished && !engine.isPaused()) {
            if(!engine.isEntityInPlay(hunter) || !hunter.isAlive()) {finish("ship lost");return;}
            elapsed+=amount;refresh+=amount;disk+=amount;updateListeners();observeProjectiles();
            for(WeaponDiagnostics row:rows.values())row.advance(amount);
            for(BeamAPI beam:engine.getBeams()){WeaponDiagnostics row=rowFor(beam.getWeapon());if(row!=null&&beam.didDamageThisFrame())row.recordBeamDamageFrame();}
            if(refresh>=.25f){refresh=0;buildSnapshot();}
            if(disk>=1){disk=0;writeSnapshot();}
        }
    }
    public void renderInUICoords(ViewportAPI viewport) {
        if (!overlay || !enabled || engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog()) return;
        if (panel == null) panel = new DiagnosticPanel();
        panel.draw(hunter==null?"COMBAT DIAGNOSTICS | Waiting for your ship\nCtrl+F8: off":panelHeader+recording.status()+panelRows, engine.isPaused());
    }
    private WeaponDiagnostics rowFor(WeaponAPI weapon) {
        return weaponRows.get(weapon);
    }
    private void observeProjectiles() {
        Set<DamagingProjectileAPI> current = identitySet();
        current.addAll(engine.getProjectiles()); current.addAll(engine.getMissiles());
        current.removeIf(p -> p.getSource() != hunter);
        for (DamagingProjectileAPI p : current) {
            WeaponDiagnostics row = rowFor(p.getWeapon());
            if (row != null && !previous.contains(p)) row.recordProjectile();
            observeImpact(p);
        }
        for (DamagingProjectileAPI p : previous) if (!current.contains(p)) observeImpact(p);
        // Retain only active identities; removed projectiles were checked for a final impact above.
        hitSeen.retainAll(current); previous = current;
    }
    private void observeImpact(DamagingProjectileAPI p) {
        if (!p.didDamage() || p.getDamageTarget() == null || !hitSeen.add(p)) return;
        WeaponDiagnostics row = rowFor(p.getWeapon()); if (row != null) row.recordImpact();
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
                    WeaponDiagnostics row = rowFor(weapon);
                    double[] values = {result.getDamageToShields(), result.getTotalDamageToArmor(), result.getDamageToHull(), result.getEmpDamage()};
                    for (int i=0; i<4; i++) { damage[i] += values[i]; if(row==null) unknown[i] += values[i]; }
                    if(row!=null)row.recordDamage(values);
                }
            };
            ship.addListener(listener); listeners.put(ship, listener);
        }
    }
    private void detach() {
        for (Map.Entry<ShipAPI, DamageListener> e : listeners.entrySet()) e.getKey().removeListener(e.getValue());
        listeners.clear(); previous.clear(); hitSeen.clear();
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
        out.append(WeaponDiagnostics.HEADER);
        for (WeaponDiagnostics row : rows.values()) {
            row.refresh(hunter, engine);
            out.append(row.line());
        }
        snapshot = out.toString();
        StringBuilder hud = new StringBuilder("COMBAT DIAGNOSTICS | "+(finished?"FINAL":"LIVE")+" | "+Math.round(elapsed)+"s\n");
        hud.append(shortText(hunter.getHullSpec().getHullName(),34)).append(targetMode?" [TARGET]":" [YOUR SHIP]").append('\n');
        hud.append("Hull ").append(Math.round(hunter.getHitpoints())).append("  Flux ").append(Math.round(hunter.getFluxTracker().getCurrFlux())).append(" / ").append(Math.round(hunter.getFluxTracker().getMaxFlux())).append('\n');
        hud.append("Observed firing, impacts and readiness\n");
        hud.append("Ctrl+F8 on/off | F9 target/self | F10 page\n");
        int pages=Math.max(1,(rows.size()+5)/6);page=Math.min(page,pages-1);
        hud.append("Weapons ").append(rows.size()).append(" | page ").append(page+1).append('/').append(pages).append(" (all keys use Ctrl)\n");
        panelHeader = hud.toString();
        hud = new StringBuilder("\n");
        int index=0;
        for(WeaponDiagnostics row:rows.values()) {
            int i=index++;
            if(i>=page*6&&i<(page+1)*6)row.appendPanel(hud);
        }
        panelRows = hud.toString();
    }
    private static String shortText(String text,int max) {String s=safe(text);return s.length()>max?s.substring(0,max-1)+"~":s;}
    private void writeSnapshot() {
        recording.writeLive(snapshot);
    }
}
