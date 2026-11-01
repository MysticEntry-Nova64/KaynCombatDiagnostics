package kayn.diagnostics;
import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

/** Instantaneous straight-path geometry, not the game's AI fire-permission predicate. */
final class FiringLine {
 static final float INF=Float.POSITIVE_INFINITY;
 record Result(String status,String blocker) { boolean blocked(){return status.equals("BLOCKED");} }
 static boolean finite(Vector2f p){return p!=null && Float.isFinite(p.x) && Float.isFinite(p.y);}
 static float circleHit(Vector2f a,Vector2f b,Vector2f c,float r){
  if(!finite(a)||!finite(b)||!finite(c))return Float.NaN;
  double dx=(double)b.x-a.x,dy=(double)b.y-a.y,ox=(double)a.x-c.x,oy=(double)a.y-c.y;
  double aa=dx*dx+dy*dy;if(aa<.000001f)return INF;
  if(!Float.isFinite(r)||r<0)return Float.NaN;
  double cc=ox*ox+oy*oy-(double)r*r;if(cc<=0)return 0;
  double bb=2*(ox*dx+oy*dy),disc=bb*bb-4*aa*cc;if(disc<0)return INF;
  double t=(-bb-Math.sqrt(disc))/(2*aa);return t>=0&&t<=1?(float)t:INF;
 }
 static float segmentHit(Vector2f a,Vector2f b,Vector2f c,Vector2f d){
  if(!finite(a)||!finite(b)||!finite(c)||!finite(d))return Float.NaN;
  double rx=(double)b.x-a.x,ry=(double)b.y-a.y,sx=(double)d.x-c.x,sy=(double)d.y-c.y,den=rx*sy-ry*sx;
  if(Math.abs(den)<.000001f){
   double qx=(double)c.x-a.x,qy=(double)c.y-a.y,rr=rx*rx+ry*ry;
   if(rr<.000001f){
    double ss=sx*sx+sy*sy;
    if(ss<.000001f)return qx*qx+qy*qy<.000001f?0:INF;
    double u=((a.x-c.x)*sx+(a.y-c.y)*sy)/ss;
    return Math.abs((a.x-c.x)*sy-(a.y-c.y)*sx)<.000001f&&u>=0&&u<=1?0:INF;
   }
   if(Math.abs(qx*ry-qy*rx)>=.000001f)return INF;
   double t0=(qx*rx+qy*ry)/rr,t1=t0+(sx*rx+sy*ry)/rr;
   double entry=Math.max(0,Math.min(t0,t1)),exit=Math.min(1,Math.max(t0,t1));
   return entry<=exit?(float)entry:INF;
  }
  double qx=(double)c.x-a.x,qy=(double)c.y-a.y,t=(qx*sy-qy*sx)/den,u=(qx*ry-qy*rx)/den;
  return t>=0&&t<=1&&u>=0&&u<=1?(float)t:INF;
 }
 static float shieldHit(Vector2f a,Vector2f b,ShieldAPI shield){
  Vector2f c=shield.getLocation();
  if(!finite(a)||!finite(b)||!finite(c))return Float.NaN;
  double dx=(double)b.x-a.x,dy=(double)b.y-a.y,ox=(double)a.x-c.x,oy=(double)a.y-c.y;
  double aa=dx*dx+dy*dy;if(aa<.000001f)return INF;
  if(!Float.isFinite(shield.getRadius())||shield.getRadius()<0)return Float.NaN;
  double bb=2*(ox*dx+oy*dy),cc=ox*ox+oy*oy-(double)shield.getRadius()*shield.getRadius();
  double disc=bb*bb-4*aa*cc;if(disc<0)return INF;
  double root=Math.sqrt(disc);
  // Shields are arcs on the boundary, not filled circles. Either crossing may hit.
  double entry=(-bb-root)/(2*aa),exit=(-bb+root)/(2*aa);
  if(entry>=0&&entry<=1&&shield.isWithinArc(new Vector2f((float)(a.x+dx*entry),(float)(a.y+dy*entry))))return (float)entry;
  if(exit>=0&&exit<=1&&shield.isWithinArc(new Vector2f((float)(a.x+dx*exit),(float)(a.y+dy*exit))))return (float)exit;
  return INF;
 }
 static Vector2f world(Vector2f p,CombatEntityAPI e){
  double angle=Math.toRadians(e.getFacing());float c=(float)Math.cos(angle),s=(float)Math.sin(angle);
  return new Vector2f(e.getLocation().x+p.x*c-p.y*s,e.getLocation().y+p.x*s+p.y*c);
 }
 static float hit(Vector2f a,Vector2f b,CombatEntityAPI e){
  if(!finite(a)||!finite(b)||!finite(e.getLocation())||!Float.isFinite(e.getFacing())||
      !Float.isFinite(e.getCollisionRadius())||e.getCollisionRadius()<0)return Float.NaN;
  float best=INF;
  BoundsAPI bounds=e.getExactBounds();
  if(bounds!=null&&!bounds.getOrigSegments().isEmpty()){
   if(e.isPointInBounds(a))best=0;
   for(BoundsAPI.SegmentAPI s:bounds.getOrigSegments()) {
    if(!finite(s.getP1())||!finite(s.getP2()))return Float.NaN;
    best=Math.min(best,segmentHit(a,b,world(s.getP1(),e),world(s.getP2(),e)));
   }
  }else best=circleHit(a,b,e.getLocation(),e.getCollisionRadius());
  ShieldAPI shield=e.getShield();
  if(shield!=null&&shield.isOn()){
   if(!finite(shield.getLocation())||!Float.isFinite(shield.getRadius())||shield.getRadius()<0)return Float.NaN;
   best=Math.min(best,shieldHit(a,b,shield));
  }
  return best;
 }
 static Result trace(WeaponAPI w,CombatEntityAPI target,Vector2f point,CombatEngineAPI engine){
  if(point==null)return new Result("NO_TARGET","-");
  Vector2f a=w.getLocation();
  if(!finite(a)||!finite(point)||!Float.isFinite(w.getRange())||w.getRange()<=0)return new Result("UNKNOWN","-");
  float length=Vector2f.sub(point,a,null).length();
  if(!Float.isFinite(length))return new Result("UNKNOWN","-");
  if(length<.001f)return new Result("UNKNOWN","-");
  float fraction=Math.min(1,w.getRange()/length);
  Vector2f b=new Vector2f(a.x+(point.x-a.x)*fraction,a.y+(point.y-a.y)*fraction);
  float limit=target==null?1:Math.min(1,hit(a,b,target));
  if(Float.isNaN(limit))return new Result("UNKNOWN","-");
  float nearest=limit;CombatEntityAPI obstacle=null;
  for(ShipAPI s:engine.getShips()){
   if(s==w.getShip()||s==target||s.isPhased()||s.getCollisionClass()==CollisionClass.NONE||s.isExpired())continue;
   float t=hit(a,b,s);if(Float.isNaN(t))return new Result("UNKNOWN","-");if(t<nearest-.0001f){nearest=t;obstacle=s;}
  }
  for(CombatEntityAPI e:engine.getAsteroids()){
   if(e==target||e.getCollisionClass()==CollisionClass.NONE||e.isExpired())continue;
   float t=hit(a,b,e);if(Float.isNaN(t))return new Result("UNKNOWN","-");if(t<nearest-.0001f){nearest=t;obstacle=e;}
  }
  return obstacle==null?new Result("CLEAR","-"):new Result("BLOCKED",obstacle instanceof ShipAPI?((ShipAPI)obstacle).getId():"ASTEROID");
 }
}
