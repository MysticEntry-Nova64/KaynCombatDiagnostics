package kayn.diagnostics;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/** Instantaneous straight-path geometry, not the game's AI fire-permission predicate. */
final class FiringLine {
 static final float INF=Float.POSITIVE_INFINITY;
 record Result(String status,String blocker) { boolean blocked(){return status.equals("BLOCKED");} }
 static float circleHit(Vector2f a,Vector2f b,Vector2f c,float r){
  float dx=b.x-a.x,dy=b.y-a.y,ox=a.x-c.x,oy=a.y-c.y;
  float aa=dx*dx+dy*dy;if(aa<.000001f)return INF;
  float cc=ox*ox+oy*oy-r*r;if(cc<=0)return 0;
  float bb=2*(ox*dx+oy*dy),disc=bb*bb-4*aa*cc;if(disc<0)return INF;
  float t=(-bb-(float)Math.sqrt(disc))/(2*aa);return t>=0&&t<=1?t:INF;
 }
 static float segmentHit(Vector2f a,Vector2f b,Vector2f c,Vector2f d){
  float rx=b.x-a.x,ry=b.y-a.y,sx=d.x-c.x,sy=d.y-c.y,den=rx*sy-ry*sx;
  if(Math.abs(den)<.000001f)return INF;
  float qx=c.x-a.x,qy=c.y-a.y,t=(qx*sy-qy*sx)/den,u=(qx*ry-qy*rx)/den;
  return t>=0&&t<=1&&u>=0&&u<=1?t:INF;
 }
 static Vector2f world(Vector2f p,CombatEntityAPI e){
  double angle=Math.toRadians(e.getFacing());float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
  return new Vector2f(e.getLocation().x+p.x*c-p.y*s,e.getLocation().y+p.x*s+p.y*c);
 }
 static float hit(Vector2f a,Vector2f b,CombatEntityAPI e){
  float best=INF;
  BoundsAPI bounds=e.getExactBounds();
  if(bounds!=null&&!bounds.getOrigSegments().isEmpty()){
   if(e.isPointInBounds(a))best=0;
   for(BoundsAPI.SegmentAPI s:bounds.getOrigSegments())best=Math.min(best,segmentHit(a,b,world(s.getP1(),e),world(s.getP2(),e)));
  }else best=circleHit(a,b,e.getLocation(),e.getCollisionRadius());
  ShieldAPI shield=e.getShield();
  if(shield!=null&&shield.isOn()){
   float t=circleHit(a,b,shield.getLocation(),shield.getRadius());
   if(Float.isFinite(t)&&shield.isWithinArc(new Vector2f(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t)))best=Math.min(best,t);
  }
  return best;
 }
 static Result trace(WeaponAPI w,CombatEntityAPI target,Vector2f point,CombatEngineAPI engine){
  if(point==null)return new Result("NO_TARGET","-");
  Vector2f a=w.getLocation();float length=Vector2f.sub(point,a,null).length();
  if(length<.001f)return new Result("UNKNOWN","-");
  float fraction=Math.min(1,w.getRange()/length);
  Vector2f b=new Vector2f(a.x+(point.x-a.x)*fraction,a.y+(point.y-a.y)*fraction);
  float limit=target==null?1:Math.min(1,hit(a,b,target));
  float nearest=limit;CombatEntityAPI obstacle=null;
  for(ShipAPI s:engine.getShips()){
   if(s==w.getShip()||s==target||s.isPhased()||s.getCollisionClass()==CollisionClass.NONE||s.isExpired())continue;
   float t=hit(a,b,s);if(t<nearest-.0001f){nearest=t;obstacle=s;}
  }
  for(CombatEntityAPI e:engine.getAsteroids()){
   if(e==target||e.getCollisionClass()==CollisionClass.NONE||e.isExpired())continue;
   float t=hit(a,b,e);if(t<nearest-.0001f){nearest=t;obstacle=e;}
  }
  return obstacle==null?new Result("CLEAR","-"):new Result("BLOCKED",obstacle instanceof ShipAPI?((ShipAPI)obstacle).getId():"ASTEROID");
 }
}
