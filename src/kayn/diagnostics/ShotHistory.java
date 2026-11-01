package kayn.diagnostics;

import java.awt.Color;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Five detached world-space initial projectile directions, newest first. */
final class ShotHistory {
    static final int CAPACITY=5;
    static final float LENGTH=90;
    private static final int[] GREEN={255,180,100,40,0};
    record Line(float x, float y, float endX, float endY) {}
    private final Deque<Line> lines=new ArrayDeque<>();
    boolean add(Vector2f origin, float facing) {
        if(!FiringLine.finite(origin) || !Float.isFinite(facing)) return false;
        double radians=Math.toRadians(facing);
        Line line=new Line(origin.x,origin.y,origin.x+LENGTH*(float)Math.cos(radians),origin.y+LENGTH*(float)Math.sin(radians));
        if(!Float.isFinite(line.endX()) || !Float.isFinite(line.endY())) return false;
        lines.addFirst(line);
        if(lines.size()>CAPACITY) lines.removeLast();
        return true;
    }
    List<Line> lines() { return List.copyOf(lines); }
    static Color color(int age) { return new Color(0,GREEN[age],0); }
}
