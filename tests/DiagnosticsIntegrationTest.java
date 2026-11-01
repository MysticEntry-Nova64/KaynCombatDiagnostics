package kayn.diagnostics;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.loading.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import org.lwjgl.util.vector.Vector2f;

public class DiagnosticsIntegrationTest {
 static int assertions, writes;
 static String file;
 static boolean paused, ended, hit;
 static List<DamageListener> listeners = new ArrayList<>();
 static List<DamagingProjectileAPI> projectiles = new ArrayList<>();
 static Map<String,Object> custom = new HashMap<>();
 static <T> T fake(Class<T> type, BiFunction<String,Object[],Object> fn) {
  return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->{
   if(m.getName().equals("hashCode")) return System.identityHashCode(p);
   if(m.getName().equals("equals")) return p==a[0];
   if(type.getPackageName().startsWith("com.fs.starfarer.api.combat") &&
       m.getName().matches("(?:set|force|giveCommand|disable|repair|apply|spawn|endCombat|removeEntity|modify).*"))
       throw new AssertionError("Observer attempted to mutate combat API: "+type.getSimpleName()+"."+m.getName());
   Object r=fn.apply(m.getName(),a); if(r!=null)return r;
   Class<?> c=m.getReturnType(); if(c==boolean.class)return false;
   if(c==float.class)return 0f; if(c==int.class)return 0; if(c==long.class)return 0L;
   if(List.class.isAssignableFrom(c))return Collections.emptyList(); return null;
  }));
 }
 static void check(boolean ok,String message){ assertions++;if(!ok)throw new AssertionError(message); }
 static ShipAPI player,target;
 static int key;static boolean consumed;
 static com.fs.starfarer.api.input.InputEventAPI event(int k){key=k;consumed=false;return fake(com.fs.starfarer.api.input.InputEventAPI.class,(n,a)->switch(n){case "isKeyDownEvent","isCtrlDown"->true;case "getEventValue"->key;case "isConsumed"->consumed;case "consume"->{consumed=true;yield null;}default->null;});}
 static void press(CombatDiagnostics d,int k){d.processInputPreCoreControls(0,List.of(event(k)));}
 public static void main(String[] args) {
  Global.setSettings(fake(SettingsAPI.class,(n,a)->{if(n.equals("writeTextFileToCommon")){writes++;file=(String)a[1];}if(n.equals("readTextFileFromCommon"))return file;return null;}));
  ShipHullSpecAPI hull=fake(ShipHullSpecAPI.class,(n,a)->switch(n){case "getHullId"->"ordinary_ship";case "getHullName"->"Ordinary ship";default->null;});
  FluxTrackerAPI flux=fake(FluxTrackerAPI.class,(n,a)->n.equals("getMaxFlux")?1000f:null);
  List<WeaponAPI> weapons=new ArrayList<>();
  ShipAPI hunter=fake(ShipAPI.class,(n,a)->switch(n){
   case "getHullSpec"->hull;case "getAllWeapons"->weapons;case "getFluxTracker"->flux;
   case "isAlive"->true;case "getLocation"->new Vector2f();case "getShipTarget"->target;case "getId"->"ordinary-1";default->null;
  });player=hunter;
  for(String id:List.of("gun-a","gun-b")){
   WeaponSlotAPI slot=fake(WeaponSlotAPI.class,(n,a)->n.equals("getId")?id:null);
   WeaponSpecAPI spec=fake(WeaponSpecAPI.class,(n,a)->n.equals("getWeaponId")?"test_weapon":null);
   weapons.add(fake(WeaponAPI.class,(n,a)->switch(n){case "getShip"->hunter;case "getSlot"->slot;case "getSpec"->spec;case "isFiring"->true;case "getRange"->500f;case "getLocation"->new Vector2f();case "getDisplayName"->"Test gun";default->null;}));
  }
  ShipAPI enemy=fake(ShipAPI.class,(n,a)->{
   if(n.equals("addListener"))listeners.add((DamageListener)a[0]);if(n.equals("removeListener"))listeners.remove(a[0]);
   return switch(n){case "getHullSpec"->hull;case "getFluxTracker"->flux;case "getId"->"enemy-2";case "getOwner"->1;case "isAlive"->true;case "getLocation"->new Vector2f(300,0);default->null;};
  });target=enemy;
  CombatEngineAPI engine=fake(CombatEngineAPI.class,(n,a)->switch(n){case "getCustomData"->custom;case "getPlayerShip"->player;case "getShips"->List.of(hunter,enemy);case "getProjectiles"->projectiles;case "isEntityInPlay","isUIShowingHUD"->true;case "isPaused"->paused;case "isCombatOver"->ended;default->null;});
  CombatDiagnostics d=new CombatDiagnostics("fixture");d.init(engine);d.advance(1,List.of());
  check(writes==0&&listeners.isEmpty(),"off by default does not collect");
  press(d,org.lwjgl.input.Keyboard.KEY_F8);d.advance(.5f,List.of());
  check(d.snapshot("").contains("gun-a"),"toggle discovers arbitrary hull and slot names");
  check(d.hudText().contains("s | Recording\n")&&!d.hudText().contains("archive on stop")&&!d.hudText().contains("Observed firing, impacts and readiness")&&!d.hudText().contains("Page"),"actual HUD assembles the compact one-page layout without redundant lines");
  press(d,org.lwjgl.input.Keyboard.KEY_F7);check(d.hudText().contains("All shortcuts use Ctrl")&&d.hudText().contains("Reports archive on stop"),"help exposes removed explanations on demand");
  press(d,org.lwjgl.input.Keyboard.KEY_F7);check(!d.hudText().contains("All shortcuts use Ctrl"),"help closes without disabling recording");
  check(consumed,"recognized key consumed");
  check(listeners.size()==1,"one listener per enemy");
  DamagingProjectileAPI projectile=fake(DamagingProjectileAPI.class,(n,a)->switch(n){case "getSpawnLocation"->new Vector2f(10,20);case "getFacing"->30f;case "getSource"->hunter;case "getWeapon"->weapons.get(0);case "didDamage"->hit;case "getDamageTarget"->enemy;default->null;});
  projectiles.add(projectile);d.advance(.5f,List.of());hit=true;d.advance(.5f,List.of());d.advance(.5f,List.of());
  check(d.snapshot("gun-a").contains("\t1\t1\t0\t"),"projectile/impact deduplication");
  check(d.shotLines("gun-a").size()==1&&d.shotLines("gun-b").isEmpty(),"new projectile adds exactly one vector only to its originating weapon");
  check(d.shotLines("gun-a").get(0).x()==10&&d.shotLines("gun-a").get(0).y()==20,"engine observation preserves projectile spawn origin");
  check(d.snapshot("gun-a").contains("FIRING"),"actual firing overrides no individual AI request");
  boolean[] finalHit={false};
  DamagingProjectileAPI finalFrame=fake(DamagingProjectileAPI.class,(n,a)->switch(n){case "getSource"->hunter;case "getWeapon"->weapons.get(0);case "didDamage"->finalHit[0];case "getDamageTarget"->enemy;default->null;});
  projectiles.add(finalFrame);d.advance(.5f,List.of());
  check(d.snapshot("gun-a").contains("\t2\t1\t0\t"),"a new projectile is not counted as a hit before impact");
  finalHit[0]=true;projectiles.remove(finalFrame);d.advance(.5f,List.of());
  check(d.snapshot("gun-a").contains("\t2\t2\t0\t"),"a disappearing projectile's final impact is counted");
  finalHit[0]=false;projectiles.add(finalFrame);d.advance(.5f,List.of());projectiles.remove(finalFrame);d.advance(.5f,List.of());
  check(d.snapshot("gun-a").contains("\t3\t2\t0\t"),"a removed projectile with no damage is not counted as a hit");
  int before=writes;String state=d.snapshot("");paused=true;d.advance(5,List.of());check(writes==before&&state.equals(d.snapshot("")),"pause freezes collection");
  check(d.hudText().contains("PAUSED | Evidence age")&&d.hudText().contains("s | Recording"),"pause remains visible independently of recording status");
  check(d.shotLines("gun-a").size()==1,"pause preserves rather than ages or duplicates vectors");
  press(d,org.lwjgl.input.Keyboard.KEY_F8);check(listeners.isEmpty(),"disable while paused removes listeners");before=writes;paused=false;d.advance(5,List.of());check(writes==before,"disabled stops writes");
  press(d,org.lwjgl.input.Keyboard.KEY_F8);d.advance(.5f,List.of());check(d.snapshot("gun-a").contains("\t0\t0\t0\t"),"fresh session excludes pre-existing projectile");
  check(d.shotLines("gun-a").isEmpty(),"new measurement resets vectors and excludes already airborne projectiles");
  press(d,org.lwjgl.input.Keyboard.KEY_F9);d.advance(.5f,List.of());check(d.snapshot("").contains("enemy-2")&&!d.snapshot("").contains("gun-a\t"),"target mode switches subject and resets weapon rows");
  press(d,org.lwjgl.input.Keyboard.KEY_F9);d.advance(.5f,List.of());check(d.snapshot("").contains("ordinary-1"),"return to player");
  player=enemy;d.advance(.5f,List.of());check(d.snapshot("").contains("enemy-2"),"automatic player ship transfer");
  before=writes;ended=true;player=hunter;d.advance(.1f,List.of());
  check(file.contains("ended=true")&&listeners.isEmpty(),"combat end finalizes and detaches");
  check(writes==before+2&&d.snapshot("").contains("enemy-2"),
      "combat end with simultaneous subject change only finalizes the existing session");
  before=writes;d.advance(5,List.of());check(writes==before,"no writes after end");
  state=d.snapshot("");player=hunter;d.advance(.5f,List.of());
  check(writes==before&&state.equals(d.snapshot(""))&&listeners.isEmpty(),
      "subject change after combat end cannot start another measurement");
  press(d,org.lwjgl.input.Keyboard.KEY_F9);d.advance(.5f,List.of());
  check(writes==before&&state.equals(d.snapshot("")),"target toggle after end preserves final snapshot");
  press(d,org.lwjgl.input.Keyboard.KEY_F8);press(d,org.lwjgl.input.Keyboard.KEY_F8);d.advance(.5f,List.of());
  check(writes==before&&state.equals(d.snapshot(""))&&listeners.isEmpty(),
      "off/on after end cannot restart collection");
  CombatDiagnostics late=new CombatDiagnostics("already ended");late.init(engine);
  press(late,org.lwjgl.input.Keyboard.KEY_F8);late.advance(.5f,List.of());
  check(writes==before&&listeners.isEmpty(),"first enable after combat end writes no empty session");
  ended=false;d.advance(.5f,List.of());late.advance(.5f,List.of());
  check(writes==before&&state.equals(d.snapshot(""))&&listeners.isEmpty(),
      "combat completion remains terminal if the engine flag later clears");
  System.out.println("PASS: "+assertions+" standalone lifecycle assertions (API fixtures)");
 }
}
