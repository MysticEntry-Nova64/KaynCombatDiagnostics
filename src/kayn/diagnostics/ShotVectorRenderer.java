package kayn.diagnostics;

import java.awt.Color;
import java.util.Collection;
import com.fs.starfarer.api.combat.ViewportAPI;
import org.lwjgl.opengl.GL11;

/** Drawing only; no game entity references are stored in shot history. */
final class ShotVectorRenderer {
    static void draw(Collection<WeaponDiagnostics> rows, ViewportAPI viewport) {
        try(RenderState ignored=new RenderState()) {
            GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glLineWidth(2f);
            GL11.glBegin(GL11.GL_LINES);
            try {
                for(WeaponDiagnostics row:rows) {
                    java.util.List<ShotHistory.Line> lines=row.shots().lines();
                    // Draw oldest first so a coincident latest shot remains bright.
                    for(int age=lines.size()-1;age>=0;age--) {
                        Color color=ShotHistory.color(age); ShotHistory.Line line=lines.get(age);
                        GL11.glColor4f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,1);
                        GL11.glVertex2f(line.x(),line.y()); GL11.glVertex2f(line.endX(),line.endY());
                    }
                }
            } finally { GL11.glEnd(); }
        }
    }
}
