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
    private float capturedAt;
    private int panelScroll;
    private boolean showHelp;
    private DiagnosticKeys keys=DiagnosticKeys.DEFAULT;
    private String stopReason = "";
    private String observationWarning = "";
    private final Map<WeaponAPI, WeaponDiagnostics> weaponRows = new IdentityHashMap<>();
    private final Map<String, String> publishedRows = new HashMap<>();
    public CombatDiagnostics() { this("combat"); }
    public CombatDiagnostics(String scenario) { this.scenario = scenario; }
    List<ShotHistory.Line> shotLines(String slot) {
        WeaponDiagnostics row=rows.get(slot);
        return row==null?List.of():row.shots().lines();
    }
    private static <T> Set<T> identitySet() { return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>()); }
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
        try {keys=DiagnosticKeys.load(name->Global.getSettings().getString(name));}
        catch(RuntimeException error){reportFailure("key configuration","diagnostic settings; using defaults",error);}
        recording = new DiagnosticRecorder(
            (filename, contents) -> {
                Global.getSettings().writeTextFileToCommon(filename, contents);
                // The game may log a failed rename without throwing. Verify completed archives.
                if(!DiagnosticRecorder.LIVE_FILE.equals(filename) &&
                        !contents.equals(Global.getSettings().readTextFileFromCommon(filename))) {
                    throw new java.io.IOException("Archive readback differs after write: "+filename);
                }
            },
            this::reportFailure);
        engine.getCustomData().put(KEY, this);
    }
    public String snapshot(String slot) {
        if (slot == null || slot.isEmpty()) return snapshot;
        String row = publishedRows.get(slot);
        return row == null ? "Unknown or not deployed slot: " + slot :
            (observationWarning.isEmpty()?"":"# snapshotStale=true\n") + WeaponDiagnostics.HEADER + row;
    }
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        try { processInput(events); }
        catch (RuntimeException error) { stopAfterFailure("input snapshot", error); }
    }
    private void processInput(List<InputEventAPI> events) {
        if(engine==null || combatEnded || engine.isCombatOver() || engine.isUIShowingDialog() || !engine.isUIShowingHUD()) return;
        for(InputEventAPI e:events) {
            if(e.isConsumed() || !e.isKeyDownEvent() || e.isRepeat() || !e.isCtrlDown() || e.isAltDown() || e.isShiftDown()) continue;
            int key=e.getEventValue();
            if(enabled && key==keys.code(DiagnosticKeys.Action.HELP)) {
                e.consume();showHelp=!showHelp;panelScroll=0;continue;
            }
            if(enabled && (key==keys.code(DiagnosticKeys.Action.UP) || key==keys.code(DiagnosticKeys.Action.DOWN))) {
                e.consume();panelScroll=Math.max(0,panelScroll+(key==keys.code(DiagnosticKeys.Action.DOWN)?1:-1));continue;
            }
            if(key==keys.code(DiagnosticKeys.Action.TOGGLE)) {
                e.consume();
                enabled=!enabled;
                if(!enabled) { finish("disabled"); hunter=null; pinned=null; targetMode=false; }
                else { finished=false; stopReason=""; panel=null; recording.retryPendingArchives(); }
            } else if(enabled && key==keys.code(DiagnosticKeys.Action.SUBJECT)) {
                e.consume();
                ShipAPI player=engine.getPlayerShip();
                if(targetMode) { targetMode=false; pinned=null; }
                else if(player!=null && player.getShipTarget()!=null) { targetMode=true; pinned=player.getShipTarget(); }
            } else if(enabled && key==keys.code(DiagnosticKeys.Action.PAGE)) {
                e.consume();
                panelScroll=0;page=(page+1)%Math.max(1,(rows.size()+5)/6); if(hunter!=null) buildSnapshot();
            }
        }
    }
    private void start(ShipAPI ship) {
        if(hunter!=null) finish("ship changed");
        hunter=ship;finished=false;recording.beginSession();stopReason="";elapsed=refresh=disk=0;capturedAt=0;page=0;panelScroll=0;
        session=UUID.randomUUID().toString();rows.clear();weaponRows.clear();
        publishedRows.clear();
        observationWarning="";
        panelHeader="New session; no complete snapshot\n";
        panelRows="";
        snapshot="# Kayn combat diagnostics v1\n# session="+session+" noCompleteSnapshot=true\n"+WeaponDiagnostics.HEADER;
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
            try { buildSnapshot(); }
            catch (RuntimeException error) { preserveFailedSnapshot("final snapshot", error); }
            writeSnapshot();
            recording.archive(session, snapshot);
        } finally {
            detach();
        }
    }
    public void advance(float amount, List<InputEventAPI> events) {
        try {
            if(engine==null) return;
            if(!engine.isPaused()) recording.advance(amount);
            advanceObservation(amount);
        } catch (RuntimeException error) { stopAfterFailure("observation snapshot", error); }
    }
    private void advanceObservation(float amount) {
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
        try { renderPanel(); }
        catch (RuntimeException error) { stopAfterFailure("panel", error); }
    }
    private void renderPanel() {
        if (!overlay || (!enabled && observationWarning.isEmpty()) || engine == null || !engine.isUIShowingHUD() || engine.isUIShowingDialog()) return;
        if (panel == null) panel = new DiagnosticPanel(keys.footer());
        panel.draw(hudText(),ObservationFreshness.state(engine.isPaused(),finished,!observationWarning.isEmpty(),elapsed-capturedAt),panelScroll);
    }
    String hudText() {
        if(hunter==null && observationWarning.isEmpty()) return "COMBAT DIAGNOSTICS | Waiting for your ship\n";
        String summary=recording.summary();
        if(summary.contains("FAILED") || summary.contains("failed")) summary="{#ff6060}"+summary+"{#ffffff}";
        return DiagnosticHud.title(elapsed,summary)+panelHeader+
            ObservationFreshness.label(engine.isPaused(),finished,!observationWarning.isEmpty(),elapsed-capturedAt)+"\n"+
            observationWarning+(showHelp?DiagnosticHud.help(keys):"")+panelRows;
    }
    public void renderInWorldCoords(ViewportAPI viewport) {
        if(!overlay || !enabled || engine==null || hunter==null || finished || !observationWarning.isEmpty() ||
                !engine.isUIShowingHUD() || engine.isUIShowingDialog())return;
        try { ShotVectorRenderer.draw(rows.values(),viewport); }
        catch(RuntimeException error){stopAfterFailure("shot vectors",error);}
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
            if (row != null && !previous.contains(p)) row.recordProjectile(p);
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
            String observedSession = session;
            DamageListener listener = new DamageListener() {
                public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
                    if (finished || !observedSession.equals(session) || result == null) return;
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
        for (Map.Entry<ShipAPI, DamageListener> e : listeners.entrySet()) {
            try { e.getKey().removeListener(e.getValue()); }
            catch (RuntimeException error) { reportFailure("listener cleanup", "combat listener", error); }
        }
        listeners.clear(); previous.clear(); hitSeen.clear();
    }
    private void preserveFailedSnapshot(String operation, RuntimeException error) {
        observationWarning="Observation: FAILED; last complete data is stale\n";
        snapshot="# snapshotStale=true measurementStopped=true; following data is last complete snapshot\n"+snapshot;
        reportFailure(operation, "session "+session, error);
    }
    private void stopAfterFailure(String operation, RuntimeException error) {
        enabled=false;
        try {
            if(hunter!=null && !finished) {
                finished=true;
                preserveFailedSnapshot(operation, error);
                recording.archive(session, snapshot);
            } else if(observationWarning.isEmpty()) {
                observationWarning="Observation: FAILED; restart diagnostics to retry\n";
                reportFailure(operation, "session "+session, error);
            }
        } finally {
            detach();hunter=null;pinned=null;targetMode=false;
        }
    }
    private void reportFailure(String operation, String filename, Exception error) {
        String message="KAYN diagnostics "+operation+" failed: "+filename;
        try { Global.getLogger(CombatDiagnostics.class).error(message,error); }
        catch (RuntimeException ignored) { /* Keep the observer boundary safe if logging is unavailable. */ }
        try {
            if(engine!=null && engine.getCombatUI()!=null) engine.getCombatUI().addMessage(0,java.awt.Color.ORANGE,message);
        } catch (RuntimeException ignored) { /* File/observation status remains available without the combat UI. */ }
    }
    private static String n(float value) { return Float.isFinite(value) ? String.format(Locale.ROOT, "%.2f", value) : "NA"; }
    private static String safe(String value) { return value == null ? "NA" : value.replace('\t',' ').replace('\n',' '); }
    /** Override in controlled verification subclasses to test failure containment without reflection. */
    protected void buildSnapshot() {
        StringBuilder out = new StringBuilder("# Kayn combat diagnostics v1\n# session=" + session + " scenario=" + scenario
            + " ship=" + safe(hunter.getId()) + " hull=" + safe(hunter.getHullSpec().getHullId()) + " reason=" + stopReason.replace(' ','_') + " seconds=" + n(elapsed) + " ended=" + finished + " capturedUtc=" + java.time.Instant.now() + "\n");
        out.append("# totals shield/armor/hull/EMP=").append(Arrays.toString(damage))
           .append(" unassignedToSlot=").append(Arrays.toString(unknown)).append('\n');
        out.append("# hp=").append(hunter.getHitpoints()).append(" flux=").append(hunter.getFluxLevel())
           .append(" overloaded=").append(hunter.getFluxTracker().isOverloaded()).append(" venting=")
           .append(hunter.getFluxTracker().isVenting()).append('\n');
        out.append("# State labels describe observations, not proven reasons. Range uses collision radius; firing line is instantaneous geometry, not AI permission or missile trajectory.\n");
        out.append(WeaponDiagnostics.HEADER);
        Map<String,String> nextRows = new HashMap<>();
        for (WeaponDiagnostics row : rows.values()) {
            row.refresh(hunter, engine);
            out.append(row.line());
        }
        for (Map.Entry<String,WeaponDiagnostics> entry : rows.entrySet()) nextRows.put(entry.getKey(),entry.getValue().line());
        int pages=Math.max(1,(rows.size()+5)/6);page=Math.min(page,pages-1);
        String nextHeader=DiagnosticHud.ship(shortText(hunter.getHullSpec().getHullName(),34),targetMode,
            Math.round(hunter.getHitpoints()),Math.round(hunter.getFluxTracker().getCurrFlux()),
            Math.round(hunter.getFluxTracker().getMaxFlux()),rows.size(),page,pages);
        StringBuilder hud = new StringBuilder("\n");
        int index=0;
        for(WeaponDiagnostics row:rows.values()) {
            int i=index++;
            if(i>=page*6&&i<(page+1)*6)row.appendPanel(hud);
        }
        // Publish the readout only after both TSV and HUD construction succeeded.
        capturedAt=elapsed;
        snapshot = out.toString();
        publishedRows.clear();publishedRows.putAll(nextRows);
        panelHeader = nextHeader;
        panelRows = hud.toString();
    }
    private static String shortText(String text,int max) {String s=safe(text);return s.length()>max?s.substring(0,max-1)+"~":s;}
    private void writeSnapshot() {
        recording.writeLive(snapshot);
    }
}
