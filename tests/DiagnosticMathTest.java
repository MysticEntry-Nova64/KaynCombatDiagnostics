package kayn.diagnostics;
public class DiagnosticMathTest {
 public static void main(String[] args) {
  check(DiagnosticMath.surfaceDistance(600, 100)==500,"surface distance");
  check(DiagnosticMath.surfaceDistance(50, 100)==0,"inside radius");
  check(DiagnosticMath.inRange(500,500),"range boundary");
  check(!DiagnosticMath.inRange(501,500),"outside range");
  check(!DiagnosticMath.inRange(Float.NaN,500),"missing target");
  check(DiagnosticMath.state(true,false,false,false,false,false,false)=="FIRING","observed firing priority");
  check(DiagnosticMath.state(false,true,false,false,false,false,false)=="DISABLED","disabled");
  check(DiagnosticMath.state(false,false,true,false,false,false,false)=="SHIP_BLOCKED","flux restriction");
  check(DiagnosticMath.state(false,false,false,false,true,true,true)=="NO_TARGET","no invented target");
  check(DiagnosticMath.state(false,false,false,true,false,true,true)=="OUT_OF_RANGE","range");
  check(DiagnosticMath.state(false,false,false,true,true,false,true)=="OUT_OF_ARC","arc");
  check(DiagnosticMath.state(false,false,false,true,true,true,false)=="AI_HOLD","AI hold");
  check(DiagnosticMath.state(false,false,false,true,true,true,true)=="READY_OBSERVED","not claiming will fire");
  check(DiagnosticMath.ready(true,true,true,false,false,false,0,1,10,20),"ready observed");
  check(!DiagnosticMath.ready(false,true,true,false,false,false,0,1,10,20),"candidate is not AI target");
  check(!DiagnosticMath.ready(true,true,true,false,false,false,1,1,10,20),"cooldown prevents ready");
  check(!DiagnosticMath.ready(true,true,true,false,false,false,0,0,10,20),"no ammo");
  check(!DiagnosticMath.ready(true,true,true,false,false,false,0,1,30,20),"insufficient flux headroom");
  check(!DiagnosticMath.ready(true,true,true,false,false,true,0,1,10,20),"forced no fire");
  System.out.println("PASS: 19 geometry/state assertions");
 }
 static void check(boolean ok,String name) {if(!ok)throw new AssertionError(name);}
}
