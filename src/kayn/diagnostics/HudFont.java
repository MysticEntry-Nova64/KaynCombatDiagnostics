package kayn.diagnostics;

import com.fs.starfarer.api.Global;
import java.awt.Color;
import org.lwjgl.opengl.GL11;

/** Draws the installed game's atlas without changing SpriteAPI objects or owning its texture. */
final class HudFont {
    private final BitmapFontMetrics metrics;
    private final int texture;
    private HudFont(BitmapFontMetrics metrics,int texture){this.metrics=metrics;this.texture=texture;}
    static HudFont load() throws java.io.IOException {
        String directory="graphics/fonts/";
        BitmapFontMetrics metrics=BitmapFontMetrics.parse(Global.getSettings().loadText(directory+"arial16bold.fnt"));
        Global.getSettings().loadTexture(directory+metrics.page);
        int texture=Global.getSettings().getSprite(directory+metrics.page).getTextureId();
        if(texture<=0)throw new IllegalStateException("Game font atlas not available");
        return new HudFont(metrics,texture);
    }
    float width(String value,float size){return metrics.width(value,size/metrics.size);}
    float lineHeight(float size){return metrics.lineHeight*size/metrics.size;}
    void draw(String value,float x,float top,float size,Color color) {
        float scale=size/metrics.size,cursor=x;int previous=-1;
        GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        GL11.glColor4f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,color.getAlpha()/255f);
        GL11.glBegin(GL11.GL_QUADS);
        try {
            for(int index=0;index<value.length();) {
                int point=value.codePointAt(index);index+=Character.charCount(point);
                BitmapFontMetrics.Glyph glyph=metrics.glyph(point);if(glyph==null)continue;
                cursor+=metrics.kerning(previous,point)*scale;
                float left=cursor+glyph.offsetX()*scale,right=left+glyph.width()*scale;
                float y=top-glyph.offsetY()*scale,bottom=y-glyph.height()*scale;
                float u0=glyph.x()/(float)metrics.atlasWidth,u1=(glyph.x()+glyph.width())/(float)metrics.atlasWidth;
                // BMFont uses a top-left origin; the game's texture loader uses bottom-left.
                float v0=1-glyph.y()/(float)metrics.atlasHeight,v1=1-(glyph.y()+glyph.height())/(float)metrics.atlasHeight;
                GL11.glTexCoord2f(u0,v0);GL11.glVertex2f(left,y);
                GL11.glTexCoord2f(u0,v1);GL11.glVertex2f(left,bottom);
                GL11.glTexCoord2f(u1,v1);GL11.glVertex2f(right,bottom);
                GL11.glTexCoord2f(u1,v0);GL11.glVertex2f(right,y);
                cursor+=glyph.advance()*scale;previous=point;
            }
        } finally {GL11.glEnd();}
    }
}
