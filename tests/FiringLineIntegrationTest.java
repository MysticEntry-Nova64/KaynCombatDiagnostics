package kayn.diagnostics;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;
import java.util.*;
public class FiringLineIntegrationTest {
 static Vector2f obstaclePos=new Vector2f(50,0);static boolean phased;
 static ShipAPI ship(String id,Vector2f loc){return DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->switch(n){
  case "getLocation"->loc;case "getCollisionRadius"->10f;case "getCollisionClass"->CollisionClass.SHIP;case "getId"->id;case "isPhased"->id.equals("blocker")&&phased;default->null;
 });}
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
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
  System.out.println("PASS: 6 firing-line transition/API fixture assertions");
 }
}
