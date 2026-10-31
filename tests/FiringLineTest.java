package kayn.diagnostics;
import org.lwjgl.util.vector.Vector2f;
public class FiringLineTest {
 static int count; static void check(boolean b,String s){count++;if(!b)throw new AssertionError(s);}
 static Vector2f p(float x,float y){return new Vector2f(x,y);}
 public static void main(String[] a){
 check(Math.abs(FiringLine.circleHit(p(0,0),p(100,0),p(50,0),10)-.4f)<.001,"intervening circle");
 check(!Float.isFinite(FiringLine.circleHit(p(0,0),p(100,0),p(150,0),10)),"behind target");
 check(!Float.isFinite(FiringLine.circleHit(p(0,0),p(100,0),p(50,20),10)),"off path");
 check(FiringLine.circleHit(p(0,0),p(100,0),p(0,0),10)==0,"origin inside obstacle");
 check(Math.abs(FiringLine.segmentHit(p(0,0),p(100,0),p(40,-10),p(40,10))-.4f)<.001,"hull crossing");
 check(!Float.isFinite(FiringLine.segmentHit(p(0,0),p(100,0),p(40,10),p(60,10))),"parallel clear");
 check(!Float.isFinite(FiringLine.circleHit(p(0,0),p(0,0),p(50,0),10)),"zero length");
 System.out.println("PASS: "+count+" firing-line geometry assertions");
 }
}
