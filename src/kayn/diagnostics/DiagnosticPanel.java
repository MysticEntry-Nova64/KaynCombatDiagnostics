package kayn.diagnostics;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.Display;

/** Compact screen-space panel, with OpenGL state restored after each draw. */
public final class DiagnosticPanel {
    private static final Color NORMAL = Color.WHITE;
    private static final Color BLOCKED = new Color(255,90,90);
    static Color lineColor(String line) {
        return line.startsWith("!")?BLOCKED:line.startsWith("^")?Color.ORANGE:line.startsWith("~")?Color.YELLOW:NORMAL;
    }
    static String visibleLine(String line) {
        return line.startsWith("!")||line.startsWith("^")||line.startsWith("~")?line.substring(1):line;
    }
    private LazyFont.DrawableString text;
    private boolean failed;
    public void draw(String value, boolean paused) {
        if (failed) return;
        if (text == null) {
            try { text = LazyFont.loadFont("graphics/fonts/arial16bold.fnt").createText(); }
            catch (Exception e) { failed=true; Global.getLogger(DiagnosticPanel.class).error("KAYN diagnostics panel font failed",e); return; }
        }
        float scale = Global.getSettings().getScreenScaleMult();
        float width = Display.getWidth(), height = Display.getHeight();
        float x=20*scale, top=height-110*scale, w=Math.min(520*scale,width-40*scale), h=Math.min(450*scale,height-130*scale);
        int matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION); GL11.glPushMatrix(); GL11.glLoadIdentity();
        GL11.glOrtho(0,width,0,height,-1,1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW); GL11.glPushMatrix(); GL11.glLoadIdentity();
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST); GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(.015f,.025f,.02f,.96f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(x,top);GL11.glVertex2f(x+w,top);GL11.glVertex2f(x+w,top-h);GL11.glVertex2f(x,top-h);
            GL11.glEnd();
            GL11.glColor4f(.40f,.70f,.48f,.9f);GL11.glLineWidth(scale);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            GL11.glVertex2f(x,top);GL11.glVertex2f(x+w,top);GL11.glVertex2f(x+w,top-h);GL11.glVertex2f(x,top-h);
            GL11.glEnd();
            text.setFontSize(16*scale);text.setMaxWidth(w-24*scale);text.setMaxHeight(h-20*scale);
            float y=top-12*scale;
            for(String line:value.split("\n",-1)){
                if(line.isEmpty()){y-=17*scale;continue;}
                text.setBaseColor(lineColor(line));
                text.setText(visibleLine(line));text.draw(x+12*scale,y);
                y-=17*scale;
            }
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();
            GL11.glPopAttrib();GL11.glMatrixMode(matrix);
        }
    }
}
