package kayn.diagnostics;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
public class FiringLineIntegrationTest {
 static Vector2f obstaclePos=new Vector2f(50,0);static boolean phased;
 static boolean shieldOn=true;
 static int shieldArc=1;
 static ShipAPI ship(String id,Vector2f loc){return DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){
  case "getLocation"->loc;case "getCollisionRadius"->10f;case "getCollisionClass"->CollisionClass.SHIP;case "getId"->id;case "isPhased"->id.equals("blocker")&&phased;default->null;
 });}
 static int assertions;
 static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  ShipAPI source=ship("self",new Vector2f()),target=ship("target",new Vector2f(100,0)),obstacle=ship("blocker",obstaclePos);
  List<ShipAPI> ships=new ArrayList<>(List.of(source,target,obstacle));
  CombatEngineAPI engine=DiagnosticsIntegrationTest.fake(CombatEngineAPI.class,(n,a)->n.equals("getShips")?ships:null);
  WeaponAPI w=DiagnosticsIntegrationTest.fake(WeaponAPI.class,(n,a)->switch(n){case "getShip"->source;case "getLocation"->source.getLocation();case "getRange"->100f;default->null;});
  check(FiringLine.trace(w,target,null,engine).status().equals("NO_TARGET"),"no target is not clear");
  check(FiringLine.trace(w,target,target.getLocation(),engine).blocker().equals("blocker"),"obstruction identified");
  obstaclePos.y=30;check(!FiringLine.trace(w,target,target.getLocation(),engine).blocked(),"moving aside clears line");
  obstaclePos.y=0;phased=true;check(!FiringLine.trace(w,target,target.getLocation(),engine).blocked(),"phased ship ignored");
  phased=false;obstaclePos.x=120;check(!FiringLine.trace(w,target,target.getLocation(),engine).blocked(),"behind target ignored");
  ships.remove(obstacle);check(FiringLine.trace(w,target,target.getLocation(),engine).status().equals("CLEAR"),"own hull and intended target excluded");
  // The ray misses the hull but crosses the shield circle at x=44 and x=56.
  ShieldAPI shield=DiagnosticsIntegrationTest.fake(ShieldAPI.class,(n,a)->switch(n){
   case "isOn"->shieldOn;case "getLocation"->new Vector2f(50,8);case "getRadius"->10f;
   case "isWithinArc"->shieldArc==0 || (shieldArc>0 && ((Vector2f)a[0]).x>50);default->null;
  });
  ShipAPI shielded=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){
   case "getLocation"->new Vector2f(50,8);case "getCollisionRadius"->1f;
   case "getShield"->shield;case "getId"->"shielded";case "getCollisionClass"->CollisionClass.SHIP;default->null;
  });
  check(Math.abs(FiringLine.hit(new Vector2f(),new Vector2f(100,0),shielded)-.56f)<.0001f,
      "inactive entry still detects active shield exit");
  check(Math.abs(FiringLine.hit(new Vector2f(100,0),new Vector2f(),shielded)-.44f)<.0001f,
      "active shield entry is the first hit");
  check(Math.abs(FiringLine.hit(new Vector2f(50,0),new Vector2f(100,0),shielded)-.12f)<.0001f,
      "ray starting inside shield reaches active boundary, not its origin");
  check(!Float.isFinite(FiringLine.hit(new Vector2f(),new Vector2f(50,0),shielded)),
      "active boundary beyond segment is ignored");
  check(!Float.isFinite(FiringLine.hit(new Vector2f(50,0),new Vector2f(),shielded)),
      "inactive exit is clear");
  check(!Float.isFinite(FiringLine.hit(new Vector2f(50,0),new Vector2f(50,0),shielded)),
      "zero-length segment does not hit a shield boundary");
  ships.add(shielded);
  check(FiringLine.trace(w,target,target.getLocation(),engine).blocker().equals("shielded"),
      "shield exit produces a blocked firing line");
  shieldArc=0;
  check(Math.abs(FiringLine.hit(new Vector2f(),new Vector2f(100,0),shielded)-.44f)<.0001f,
      "full shield chooses earliest boundary");
  check(Math.abs(FiringLine.hit(new Vector2f(0,-2),new Vector2f(100,-2),shielded)-.5f)<.0001f,
      "active tangent counts as a hit");
  shieldArc=-1;
  check(!Float.isFinite(FiringLine.hit(new Vector2f(),new Vector2f(100,0),shielded)),
      "both inactive intersections are clear");
  shieldOn=false;shieldArc=0;
  check(!Float.isFinite(FiringLine.hit(new Vector2f(),new Vector2f(100,0),shielded)),
      "disabled shield does not obstruct");
  System.out.println("PASS: "+assertions+" firing-line transition/API fixture assertions");
 }
}
