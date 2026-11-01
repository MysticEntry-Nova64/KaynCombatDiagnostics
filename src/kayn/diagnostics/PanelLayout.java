package kayn.diagnostics;

/** Content-driven board dimensions, bounded to the current window. All inputs are pixels. */
final class PanelLayout {
    record Board(float x,float top,float width,float height,int rows) {}
    static Board fit(float screenWidth,float screenHeight,float scale,float textWidth,int count,float lineHeight) {
        float margin=16*scale,padding=12*scale,top=screenHeight-90*scale;
        float width=Math.min(Math.max(310*scale,textWidth+padding*2),Math.min(620*scale,screenWidth-margin*2));
        width=Math.max(40,width);
        float available=Math.max(lineHeight,top-130*scale);
        int rows=Math.max(1,Math.min(count,(int)((available-padding*2-28*scale)/lineHeight)));
        float height=Math.min(available,padding*2+rows*lineHeight+28*scale);
        return new Board(margin,top,width,height,rows);
    }
}
