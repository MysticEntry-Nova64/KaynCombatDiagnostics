package kayn.diagnostics;

import com.fs.starfarer.api.combat.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class WeaponEvidenceTest {
    static int assertions;
    static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static void history(){
        ShotHistory history=new ShotHistory();Vector2f origin=new Vector2f(12,34);
        check(history.add(origin,0),"finite shot accepted");origin.x=900;
        check(history.lines().get(0).x()==12&&history.lines().get(0).endX()==102,"spawn coordinate detached from mutable vector");
        for(int i=2;i<=5;i++)history.add(new Vector2f(i,0),90);
        check(history.lines().size()==5&&history.lines().get(4).x()==12,"five shots retained");
        check(ShotHistory.color(0).equals(java.awt.Color.GREEN)&&ShotHistory.color(4).equals(java.awt.Color.BLACK),"bright green to black endpoints");
        for(int i=1;i<5;i++)check(ShotHistory.color(i).getGreen()<ShotHistory.color(i-1).getGreen(),"strict darkening by shot age");
        history.add(new Vector2f(6,0),180);
        check(history.lines().size()==5&&history.lines().get(4).x()==2&&history.lines().get(0).x()==6,"sixth removes oldest and becomes newest");
        check(Math.abs(history.lines().get(0).endX()+84)<.001,"line follows facing and fixed short length");
        List<ShotHistory.Line> before=history.lines();
        check(!history.add(new Vector2f(Float.NaN,0),0)&&!history.add(new Vector2f(),Float.POSITIVE_INFINITY),"invalid shot rejected");
        check(history.lines().equals(before),"invalid shot does not age or evict valid history");
        boolean immutable=false;try{before.clear();}catch(UnsupportedOperationException expected){immutable=true;}
        check(immutable,"published lines cannot mutate history");
        WeaponDiagnosticsTest.Fixture one=new WeaponDiagnosticsTest.Fixture(),two=new WeaponDiagnosticsTest.Fixture();
        DamagingProjectileAPI projectile=DiagnosticsIntegrationTest.fake(DamagingProjectileAPI.class,(n,a)->switch(n){
            case "getSpawnLocation"->new Vector2f(4,8);case "getLocation"->new Vector2f(500,800);case "getFacing"->90f;default->null;});
        one.diagnostic.recordProjectile(projectile);
        check(one.diagnostic.shots().lines().size()==1&&two.diagnostic.shots().lines().isEmpty(),"weapon histories are independent");
        check(one.diagnostic.shots().lines().get(0).x()==4&&one.diagnostic.shots().lines().get(0).y()==8,"projectile uses spawn rather than moved location");
        check(one.read().get("projectilesObserved").equals("1"),"vector capture preserves projectile counter");
    }
    static void geometry(){
        ShipAPI ship=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->n.equals("getLocation")?new Vector2f():null);
        CombatEngineAPI engine=DiagnosticsIntegrationTest.fake(CombatEngineAPI.class,(n,a)->null);
        float[] range={500};Vector2f origin=new Vector2f();
        WeaponAPI weapon=DiagnosticsIntegrationTest.fake(WeaponAPI.class,(n,a)->switch(n){case "getShip"->ship;case "getLocation"->origin;case "getRange"->range[0];default->null;});
        for(float bad:new float[]{Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY}){
            check(FiringLine.trace(weapon,null,new Vector2f(bad,0),engine).status().equals("UNKNOWN"),"nonfinite aim is never clear");
            range[0]=bad;check(FiringLine.trace(weapon,null,new Vector2f(100,0),engine).status().equals("UNKNOWN"),"nonfinite range is unknown");range[0]=500;
            origin.x=bad;check(FiringLine.trace(weapon,null,new Vector2f(100,0),engine).status().equals("UNKNOWN"),"nonfinite muzzle is unknown");origin.x=0;
        }
        range[0]=-1;check(FiringLine.trace(weapon,null,new Vector2f(100,0),engine).status().equals("UNKNOWN"),"negative range is unknown");
        range[0]=0;check(FiringLine.trace(weapon,null,new Vector2f(100,0),engine).status().equals("UNKNOWN"),"zero range is unknown");range[0]=500;
        check(FiringLine.trace(weapon,null,new Vector2f(),engine).status().equals("UNKNOWN"),"zero-length aim is unknown");
        check(FiringLine.trace(weapon,null,new Vector2f(100,0),engine).status().equals("CLEAR"),"ordinary valid ray remains clear");
        check(Math.abs(FiringLine.segmentHit(new Vector2f(),new Vector2f(10,0),new Vector2f(4,0),new Vector2f(8,0))-.4f)<.0001,"collinear overlap counts at first contact");
        check(FiringLine.segmentHit(new Vector2f(),new Vector2f(10,0),new Vector2f(10,0),new Vector2f(12,0))==1,"touching endpoints count");
        check(!Float.isFinite(FiringLine.segmentHit(new Vector2f(),new Vector2f(10,0),new Vector2f(11,0),new Vector2f(12,0))),"disjoint collinear segments remain clear");
        check(!Float.isFinite(FiringLine.segmentHit(new Vector2f(),new Vector2f(10,0),new Vector2f(4,1),new Vector2f(8,1))),"parallel offset segments remain clear");
        WeaponDiagnosticsTest.Fixture f=new WeaponDiagnosticsTest.Fixture();f.range=Float.NaN;
        Map<String,String> row=f.read();
        check(row.get("restrictions").contains("Unknown")&&!row.get("restrictions").contains("Distance")&&row.get("readyObserved").equals("false")&&row.get("state").equals("UNKNOWN"),"invalid range cannot masquerade as distance or readiness in either readout");
        f.range=500;f.muzzle=null;row=f.read();
        check(row.get("restrictions").contains("Unknown")&&row.get("readyObserved").equals("false"),"missing muzzle produces unknown rather than stopping the observer");
        f.muzzle=new Vector2f(Float.NaN,0);row=f.read();
        check(row.get("pointInArc").equals("NA")&&row.get("fireLineGeometry").equals("UNKNOWN"),"nonfinite muzzle is unknown in both arc and geometry evidence");
    }
    static void freshness(){
        check(ObservationFreshness.label(false,false,false,.1f).startsWith("LIVE"),"recent evidence live");
        check(ObservationFreshness.label(true,false,false,0).startsWith("PAUSED | Evidence age"),"paused cached firing visibly identified even at age zero");
        check(ObservationFreshness.label(false,false,false,.6f).startsWith("STALE"),"delayed refresh stale");
        check(ObservationFreshness.label(false,true,false,0).startsWith("FINAL / STALE"),"finished session is not live");
        check(ObservationFreshness.label(true,false,true,0).startsWith("STALE / FAILED"),"failure takes precedence over pause");
        check(DiagnosticPanel.footer(ObservationFreshness.State.PAUSED).startsWith("PAUSED"),"scrolling cannot hide paused freshness cue");
        check(DiagnosticPanel.footer(ObservationFreshness.State.FINAL).startsWith("FINAL / STALE"),"scrolling cannot hide final freshness cue");
        check(DiagnosticPanel.footer(ObservationFreshness.state(true,false,true,0)).startsWith("STALE / FAILED"),"fixed failure cue takes priority over pause");
    }
    static void compactHud(){
        String single=DiagnosticHud.ship("Mule (P)",true,5498,0,6400,6,0,1);
        check(!single.contains("Page")&&single.contains("Hull 5498 | Flux 0 / 6400 | Weapons 6"),"single-page HUD keeps ship metrics together and hides redundant pagination");
        check(DiagnosticHud.ship("Lasher",false,2100,50,3500,7,0,2).contains("Page 1/2"),"multiple pages remain discoverable");
        check(DiagnosticHud.ship("Lasher",false,2100,50,3500,7,1,2).contains("Page 2/2"),"current page is reported accurately");
        check(!DiagnosticHud.ship("No weapons",false,100,0,100,0,0,1).contains("Page"),"zero weapons does not invent another page");
        check(DiagnosticHud.title(49,"Recording").strip().equals("COMBAT DIAGNOSTICS | 49s | Recording"),"agreed title keeps elapsed time and recording together");
        DiagnosticRecorder recorder=new DiagnosticRecorder((n,c)->{},(o,n,e)->{});recorder.beginSession();
        check(recorder.summary().equals("Recording"),"healthy recording is concise");
        recorder.archive("example","report");check(recorder.summary().equals("Archived"),"finished recording does not keep claiming active recording");
    }
    public static void main(String[] args){history();geometry();freshness();compactHud();System.out.println("PASS: "+assertions+" new weapon evidence assertions");}
}
