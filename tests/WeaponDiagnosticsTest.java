package kayn.diagnostics;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.*;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class WeaponDiagnosticsTest {
 static int assertions;
 static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 static final class Fixture {
  boolean firing,disabled,overloaded,venting,forcedOff,aiTarget=true,wants=true,beam,grouped=true,autofire=true,selected,obstructed,beamContact,missileTarget,shipAI,autopilot,projectileContact;
  float cooldown,currentFlux,fluxCost=10,arcDistance,range=500;
  int ammo=3;
  Vector2f muzzle=new Vector2f();
  final ShipAPI ship,target;
  final WeaponAPI weapon;
  final WeaponDiagnostics diagnostic;
  final MissileAPI missile=DiagnosticsIntegrationTest.fake(MissileAPI.class,(n,a)->switch(n){case "getLocation"->new Vector2f(50,0);case "getCollisionRadius"->2f;default->null;});
  final DamagingProjectileAPI projectile=DiagnosticsIntegrationTest.fake(DamagingProjectileAPI.class,(n,a)->switch(n){case "getLocation"->new Vector2f(50,0);case "getCollisionRadius"->2f;default->null;});
  Fixture(){
   target=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){case "getLocation"->new Vector2f(300,0);case "getId"->"target";case "getCollisionRadius"->10f;case "getOwner"->1;case "isAlive"->true;default->null;});
   AutofireAIPlugin ai=DiagnosticsIntegrationTest.fake(AutofireAIPlugin.class,(n,a)->switch(n){case "getTargetShip"->aiTarget&&!missileTarget?target:null;case "getTargetMissile"->aiTarget&&missileTarget?missile:null;case "getTarget"->aiTarget?(missileTarget?missile.getLocation():target.getLocation()):null;case "shouldFire"->wants;default->null;});
   WeaponGroupAPI group=DiagnosticsIntegrationTest.fake(WeaponGroupAPI.class,(n,a)->switch(n){case "getAutofirePlugin"->ai;case "isAutofiring"->autofire;default->null;});
   FluxTrackerAPI flux=DiagnosticsIntegrationTest.fake(FluxTrackerAPI.class,(n,a)->switch(n){case "getMaxFlux"->1000f;case "getCurrFlux"->currentFlux;case "isOverloaded"->overloaded;case "isVenting"->venting;default->null;});
   ShipAIPlugin pilot=DiagnosticsIntegrationTest.fake(ShipAIPlugin.class,(n,a)->null);
   ship=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){case "getShipAI"->shipAI?pilot:null;case "getShipTarget"->shipAI||autopilot?target:null;case "getMouseTarget"->target.getLocation();case "getFluxTracker"->flux;case "getWeaponGroupFor"->grouped?group:null;case "getSelectedGroupAPI"->selected?group:null;case "getWeaponGroupsCopy"->grouped?List.of(group):List.of();case "getLocation"->new Vector2f();default->null;});
   WeaponSpecAPI spec=DiagnosticsIntegrationTest.fake(WeaponSpecAPI.class,(n,a)->n.equals("getWeaponId")?"fixture_weapon":null);
   weapon=DiagnosticsIntegrationTest.fake(WeaponAPI.class,(n,a)->switch(n){case "getShip"->ship;case "getSpec"->spec;case "getDisplayName"->"Fixture gun";case "getLocation"->muzzle;case "getRange"->range;case "distanceFromArc"->arcDistance;case "isFiring"->firing;case "isDisabled"->disabled;case "isForceNoFireOneFrame"->forcedOff;case "usesAmmo"->true;case "getAmmo"->ammo;case "getCooldownRemaining"->cooldown;case "getFluxCostToFire"->fluxCost;case "isBeam"->beam;default->null;});
   diagnostic=new WeaponDiagnostics("mount-a",weapon);
  }
  Map<String,String> read(){
   ShipAPI blocker=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){case "getLocation"->new Vector2f(50,0);case "getCollisionRadius"->10f;case "getId"->"blocker";case "getCollisionClass"->CollisionClass.SHIP;default->null;});
   BeamAPI contact=DiagnosticsIntegrationTest.fake(BeamAPI.class,(n,a)->switch(n){case "getWeapon"->weapon;case "didDamageThisFrame"->true;case "getDamageTarget"->projectileContact?projectile:missile;case "getTo"->missile.getLocation();default->null;});
   CombatEngineAPI engine=DiagnosticsIntegrationTest.fake(CombatEngineAPI.class,(n,a)->switch(n){case "getPlayerShip"->ship;case "isUIAutopilotOn"->autopilot;case "getShips"->obstructed?List.of(ship,target,blocker):List.of(ship,target);case "getBeams"->beamContact?List.of(contact):List.of();default->null;});
   diagnostic.refresh(ship,engine);
   String[] header=WeaponDiagnostics.HEADER.stripTrailing().split("\t"),cells=diagnostic.line().stripTrailing().split("\t");
   check(header.length==cells.length,"TSV columns remain aligned");
   Map<String,String> result=new HashMap<>();for(int i=0;i<header.length;i++)result.put(header[i],cells[i]);return result;
  }
 }
 static void readiness(){
  Fixture f=new Fixture();Map<String,String> row=f.read();
  check(row.get("state").equals("READY_OBSERVED")&&row.get("readyObserved").equals("true"),"readiness is an observation when known checks pass");
  f.cooldown=1;row=f.read();check(row.get("state").equals("COOLDOWN")&&row.get("readyObserved").equals("false"),"cooldown must not be labeled ready");
  f.cooldown=0;f.forcedOff=true;row=f.read();check(row.get("state").equals("FORCED_NO_FIRE")&&row.get("readyObserved").equals("false"),"forced-off flag explains a held weapon");
  f.forcedOff=false;f.currentFlux=995;row=f.read();check(row.get("state").equals("FLUX_HEADROOM_LOW")&&row.get("readyObserved").equals("false"),"low measured flux headroom is labeled without promising future behavior");
  f.currentFlux=0;f.ammo=0;check(f.read().get("state").equals("NO_AMMO"),"empty ammunition is visible");
  f.ammo=3;f.disabled=true;check(f.read().get("state").equals("DISABLED"),"disabled weapon is visible");
  f.disabled=false;f.venting=true;check(f.read().get("state").equals("SHIP_BLOCKED"),"venting blocks readiness");
  f.venting=false;f.overloaded=true;check(f.read().get("readyObserved").equals("false"),"overload prevents readiness");
  f.overloaded=false;f.aiTarget=false;row=f.read();check(row.get("state").equals("NO_TARGET")&&row.get("readyObserved").equals("false"),"nearest enemy is not invented as an AI firing target");
  f.aiTarget=true;f.arcDistance=1;check(f.read().get("state").equals("OUT_OF_ARC"),"arc observation is preserved");
  f.arcDistance=0;f.wants=false;check(f.read().get("state").equals("AI_HOLD"),"false AI request is not claimed as readiness");
  f.wants=true;f.range=100;row=f.read();check(row.get("state").equals("OUT_OF_RANGE")&&row.get("readyObserved").equals("false"),"out-of-range observation is preserved");
  f.range=500;check(f.read().get("controlMode").equals("AUTO"),"autofiring group is labeled AUTO");
  f.selected=true;row=f.read();check(row.get("controlMode").equals("SELECTED")&&row.get("selectedGroup").equals("true"),"selected group remains independent of autofire mode");
  f.selected=false;f.autofire=false;check(f.read().get("controlMode").equals("MANUAL"),"non-autofiring unselected group is labeled MANUAL");
  f.grouped=false;row=f.read();check(row.get("controlMode").equals("UNGROUPED")&&row.get("aiShouldFire").equals("NA"),"ungrouped weapon does not invent an AI request");
 }
 static void counters(){
  Fixture f=new Fixture();f.firing=true;f.wants=false;f.aiTarget=false;
  f.diagnostic.advance(.5f);f.diagnostic.recordProjectile();f.diagnostic.recordProjectile();f.diagnostic.recordImpact();f.diagnostic.recordBeamDamageFrame();
  Map<String,String> row=f.read();
  check(row.get("state").equals("FIRING")&&row.get("aiShouldFire").equals("false"),"actual firing is independent of individual autofire request");
  check(row.get("firingSeconds").equals("0.50")&&row.get("projectilesObserved").equals("2")&&row.get("impactsObserved").equals("1")&&row.get("beamDamageFrames").equals("1"),"firing/projectile/impact/beam counters survive the refactor");
  f.firing=false;f.diagnostic.advance(1);
  StringBuilder hud=new StringBuilder();f.diagnostic.appendPanel(hud);
  check(hud.toString().contains("FIRING"),"HUD retains the same captured observation as the TSV until refresh");
  row=f.read();check(row.get("firingSeconds").equals("0.50"),"idle time does not add firing time");
 }
 static String panel(Fixture f){f.read();StringBuilder text=new StringBuilder();f.diagnostic.appendPanel(text);return text.toString();}
 static void targetAccuracy(){
  Fixture f=new Fixture();f.aiTarget=false;f.range=100;f.obstructed=true;
  Map<String,String> row=f.read();
  check(row.get("targetId").equals("NA")&&row.get("rangeStatus").equals("UNKNOWN"),"nearby enemy must not become the weapon's target or range reference");
  check(!panel(f).contains("Range OUT")&&!panel(f).startsWith("!"),"unknown target cannot produce yellow range or red obstruction warnings");
  check(f.read().get("restrictions").contains("NoTarget"),"no known target is an explicit restriction");
  f.venting=true;check(panel(f).contains(WeaponRestriction.FLUXING.markup())&&panel(f).contains(WeaponRestriction.NO_TARGET.markup()),"venting and missing target retain separate colours together");f.venting=false;
  f.aiTarget=true;f.autofire=false;row=f.read();
  check(row.get("state").equals("TARGET_UNKNOWN")&&row.get("aiShouldFire").equals("NA"),"inactive autofire data cannot describe manual weapon intent");
  f.autofire=true;f.selected=true;f.obstructed=false;row=f.read();
  check(row.get("targetId").equals("NA")&&row.get("state").equals("TARGET_UNKNOWN"),"selected group does not inherit a stale autofire target");
  f.shipAI=true;row=f.read();check(row.get("targetSource").equals("SHIP_AI_SELECTED_TARGET")&&row.get("targetId").equals("target")&&panel(f).contains("TARGET SHIP"),"AI-controlled selected group uses the ship AI's explicit target and aim point");check(row.get("state").equals("OUT_OF_RANGE")&&panel(f).contains(WeaponRestriction.DISTANCE.markup()),"explicit ship AI target outside weapon range is yellow");f.shipAI=false;
  f.firing=true;row=f.read();check(row.get("state").equals("FIRING")&&row.get("rangeStatus").equals("UNKNOWN"),"manual firing without target evidence stays FIRING with unknown range");
  f.autopilot=true;row=f.read();check(row.get("targetSource").equals("SHIP_AI_SELECTED_TARGET"),"player autopilot can supply selected-group intent without a ShipAIPlugin");f.autopilot=false;
  f.firing=false;f.beam=true;f.beamContact=true;row=f.read();
  check(row.get("targetSource").equals("BEAM_CONTACT")&&row.get("targetId").equals("MISSILE")&&row.get("rangeStatus").equals("MET")&&row.get("state").equals("FIRING"),"current beam missile contact overrides missing or stale autofire information");
  check(panel(f).contains("TARGET MISSILE")&&!panel(f).contains("NO_TARGET"),"observed missile target changes NO_TARGET to TARGET");
  f.projectileContact=true;row=f.read();check(row.get("targetId").equals("PROJECTILE")&&panel(f).contains("TARGET PROJECTILE"),"non-missile beam contact is identified as a projectile, not a missile");f.projectileContact=false;
  f.beamContact=false;row=f.read();check(row.get("rangeStatus").equals("UNKNOWN"),"previous beam contacts are not retained as current targets");
  f.selected=false;f.missileTarget=true;f.obstructed=false;row=f.read();
  check(row.get("targetId").equals("MISSILE")&&row.get("rangeStatus").equals("MET"),"autofire missile target is measured instead of distant enemy ship");
  f.missileTarget=false;check(panel(f).contains("TARGET SHIP"),"observed ship target is explicitly labeled TARGET");
  f.aiTarget=false;String absent=panel(f);
  check(!absent.contains("NO_TARGET")&&!absent.contains("TARGET_UNKNOWN")&&absent.split("NoTarget",-1).length==2,"missing target appears once as the coloured restriction");
 }
 static void presentation(){
  Fixture f=new Fixture();f.range=100;
  check(panel(f).contains(WeaponRestriction.DISTANCE.markup()),"distance restriction has its own yellow colour");
  f.venting=true;
  check(panel(f).contains(WeaponRestriction.FLUXING.markup())&&panel(f).contains(WeaponRestriction.DISTANCE.markup()),"venting does not hide distance");
  f.obstructed=true;
  check(panel(f).contains(WeaponRestriction.BLOCKED.markup())&&panel(f).contains(WeaponRestriction.FLUXING.markup())&&panel(f).contains(WeaponRestriction.DISTANCE.markup()),"blocked does not hide other restrictions");
  f.obstructed=false;
  f.venting=false;f.range=500;f.beam=true;f.firing=true;
  f.diagnostic.advance(.5f);f.diagnostic.recordBeamDamageFrame();f.diagnostic.recordBeamDamageFrame();
  check(panel(f).contains("Beam 0.50s | Hit frames 2"),"beam firing duration and observed damage frames are visible together");
  java.util.List<DiagnosticPanel.Span> spans=DiagnosticPanel.spans("{#ffa040}Blocked{#ffffff} | {#40e8ff}Fluxing");
  check(spans.size()==3&&spans.get(0).text().equals("Blocked")&&spans.get(0).color().equals(WeaponRestriction.BLOCKED.color),"inline blocked colour and visible text decoded");
  check(spans.get(2).text().equals("Fluxing")&&spans.get(2).color().equals(WeaponRestriction.FLUXING.color),"second restriction keeps its independent colour");
  f.beam=false;f.firing=false;f.disabled=true;f.ammo=0;f.cooldown=1;f.currentFlux=995;f.forcedOff=true;f.range=100;f.arcDistance=1;f.obstructed=true;
  Map<String,String> all=f.read();
  for(String label:List.of("Blocked","Distance","Fluxing","Damaged","Cooldown","NoAmmo","Hold","Arc"))
   check(all.get("restrictions").contains(label)&&panel(f).contains(label),"simultaneous restriction survives TSV and HUD: "+label);
  check(panel(f).lines().filter(l->l.contains("{#")).allMatch(l->l.chars().filter(c->c=='|').count()<4),"short labels wrap in groups of four");

 }
 public static void main(String[] args){if(args.length==0||args[0].equals("target"))targetAccuracy();if(args.length==0||args[0].equals("presentation"))presentation();if(args.length==0||args[0].equals("readiness"))readiness();if(args.length==0||args[0].equals("counters"))counters();System.out.println("PASS: "+assertions+" weapon diagnostics assertions");}
}
