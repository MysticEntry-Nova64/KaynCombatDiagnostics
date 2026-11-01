package kayn.diagnostics;

import java.awt.Color;
import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.regex.*;
import org.lwjgl.opengl.*;

/** Compact content-sized board. Font metrics and layout are independent of GL/game state. */
public final class DiagnosticPanel {
    private static final Pattern COLOR=Pattern.compile("\\{#([0-9a-fA-F]{6})\\}");
    record Span(String text,Color color) {}
    static List<Span> spans(String line) {
        List<Span> result=new ArrayList<>();Matcher matcher=COLOR.matcher(line);
        int start=0;Color color=Color.WHITE;
        while(matcher.find()) {
            if(matcher.start()>start)result.add(new Span(line.substring(start,matcher.start()),color));
            color=new Color(Integer.parseInt(matcher.group(1),16));start=matcher.end();
        }
        if(start<line.length())result.add(new Span(line.substring(start),color));
        return List.copyOf(result);
    }
    static List<List<Span>> wrap(String value,float width,ToDoubleFunction<String> measure) {
        List<List<Span>> lines=new ArrayList<>();
        for(String line:value.split("\n",-1)) {
            List<Span> row=new ArrayList<>();float used=0;
            for(Span span:spans(line)) {
                StringBuilder part=new StringBuilder();
                for(int index=0;index<span.text().length();) {
                    int point=span.text().codePointAt(index);index+=Character.charCount(point);
                    String character=new String(Character.toChars(point));
                    float next=(float)measure.applyAsDouble(part+character);
                    if(used+next>width && (used>0||part.length()>0)) {
                        if(part.length()>0)row.add(new Span(part.toString(),span.color()));
                        lines.add(List.copyOf(row));row=new ArrayList<>();used=0;part.setLength(0);
                    }
                    part.append(character);
                }
                if(part.length()>0){row.add(new Span(part.toString(),span.color()));used+=measure.applyAsDouble(part.toString());}
            }
            lines.add(List.copyOf(row));
        }
        return List.copyOf(lines);
    }
    private HudFont font;
    private boolean failed;
    private final String footerHint;
    public DiagnosticPanel(){this(DiagnosticKeys.DEFAULT.footer());}
    DiagnosticPanel(String footerHint){this.footerHint=footerHint;}
    static String footer(ObservationFreshness.State state) {
        return state.label+" | "+DiagnosticKeys.DEFAULT.footer();
    }
    public void draw(String value,boolean paused,int scroll) {
        draw(value,paused?ObservationFreshness.State.PAUSED:ObservationFreshness.State.LIVE,scroll);
    }
    void draw(String value,ObservationFreshness.State state,int scroll) {
        if(failed)return;
        if(font==null)try{font=HudFont.load();}
        catch(Exception error){failed=true;throw new IllegalStateException("Cannot load installed game font",error);}
        float scale=com.fs.starfarer.api.Global.getSettings().getScreenScaleMult();
        float size=14*scale,lineHeight=Math.max(16*scale,font.lineHeight(size));
        float width=Display.getWidth(),height=Display.getHeight(),textWidth=0;
        for(String line:value.split("\n",-1)) {
            String plain=spans(line).stream().map(Span::text).collect(java.util.stream.Collectors.joining());
            textWidth=Math.max(textWidth,font.width(plain,size));
        }
        PanelLayout.Board board=PanelLayout.fit(width,height,scale,textWidth,1,lineHeight);
        List<List<Span>> lines=wrap(value,Math.max(1,board.width()-24*scale),s->font.width(s,size));
        board=PanelLayout.fit(width,height,scale,textWidth,lines.size(),lineHeight);
        float x=board.x(),top=board.top(),w=board.width(),h=board.height();
        int matrix=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        try(RenderState ignored=new RenderState()) {
            GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,width,0,height,-1,1);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
            try {
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(.018f,.035f,.045f,.94f);rounded(x,top,w,h,7*scale,GL11.GL_POLYGON);
                GL11.glColor4f(.24f,.56f,.58f,.8f);GL11.glLineWidth(scale);rounded(x,top,w,h,7*scale,GL11.GL_LINE_LOOP);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((int)x,Math.max(0,(int)(top-h)),(int)w,(int)h);
                int offset=Math.min(Math.max(0,scroll),Math.max(0,lines.size()-board.rows()));
                float y=top-11*scale;
                for(int index=offset;index<Math.min(lines.size(),offset+board.rows());index++) {
                    float cursor=x+12*scale;
                    for(Span span:lines.get(index)) {
                        Color color=index==0&&span.color().equals(Color.WHITE)?new Color(180,235,230):span.color();
                        font.draw(span.text(),cursor,y,size,color);cursor+=font.width(span.text(),size);
                    }
                    y-=lineHeight;
                }
                font.draw(state.label+" | "+footerHint,x+12*scale,top-h+20*scale,12*scale,new Color(150,180,185));
            } finally {
                GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPopMatrix();
                GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(matrix);
            }
        }
    }
    private static void rounded(float x,float top,float width,float height,float radius,int mode) {
        radius=Math.min(radius,Math.min(width,height)/2);
        GL11.glBegin(mode);
        try {
            float[] cx={x+radius,x+width-radius,x+width-radius,x+radius};
            float[] cy={top-radius,top-radius,top-height+radius,top-height+radius};
            for(int corner=0;corner<4;corner++)for(int step=0;step<=6;step++) {
                double angle=Math.toRadians(180-corner*90-step*15);
                GL11.glVertex2f(cx[corner]+radius*(float)Math.cos(angle),cy[corner]+radius*(float)Math.sin(angle));
            }
        } finally {GL11.glEnd();}
    }
}
