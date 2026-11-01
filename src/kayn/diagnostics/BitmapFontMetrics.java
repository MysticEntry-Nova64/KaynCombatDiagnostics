package kayn.diagnostics;

import java.util.*;
import java.util.regex.*;

/** Read-only BMFont metrics. No textures, game objects, or rendering state. */
final class BitmapFontMetrics {
    record Glyph(int x,int y,int width,int height,int offsetX,int offsetY,int advance) {}
    private static final Pattern FIELD=Pattern.compile("(\\w+)=(\"[^\"]*\"|[^\\s]+)");
    final int size,lineHeight,atlasWidth,atlasHeight;
    final String page;
    private final Map<Integer,Glyph> glyphs;
    private final Map<Long,Integer> kernings;

    private BitmapFontMetrics(int size,int lineHeight,int width,int height,String page,
            Map<Integer,Glyph> glyphs,Map<Long,Integer> kernings) {
        this.size=size;this.lineHeight=lineHeight;atlasWidth=width;atlasHeight=height;
        this.page=page;this.glyphs=Map.copyOf(glyphs);this.kernings=Map.copyOf(kernings);
    }
    static BitmapFontMetrics parse(String descriptor) {
        int size=0,lineHeight=0,width=0,height=0;String page=null;
        Map<Integer,Glyph> glyphs=new HashMap<>();Map<Long,Integer> kernings=new HashMap<>();
        for(String line:descriptor.split("\\R")) {
            Map<String,String> fields=new HashMap<>();Matcher matcher=FIELD.matcher(line);
            while(matcher.find())fields.put(matcher.group(1),matcher.group(2).replace("\"",""));
            if(line.startsWith("info "))size=Math.abs(number(fields,"size"));
            else if(line.startsWith("common ")) {
                lineHeight=number(fields,"lineHeight");width=number(fields,"scaleW");height=number(fields,"scaleH");
                if(number(fields,"pages")!=1)throw new IllegalArgumentException("Expected one font atlas");
            } else if(line.startsWith("page ")) {
                if(number(fields,"id")!=0)throw new IllegalArgumentException("Unexpected font page");
                page=fields.get("file");
            } else if(line.startsWith("char ")) {
                if(number(fields,"page")!=0)throw new IllegalArgumentException("Unexpected glyph page");
                glyphs.put(number(fields,"id"),new Glyph(number(fields,"x"),number(fields,"y"),
                    number(fields,"width"),number(fields,"height"),number(fields,"xoffset"),
                    number(fields,"yoffset"),number(fields,"xadvance")));
            } else if(line.startsWith("kerning "))kernings.put(pair(number(fields,"first"),number(fields,"second")),number(fields,"amount"));
        }
        if(size<=0||lineHeight<=0||width<=0||height<=0||page==null||glyphs.isEmpty()||
                page.contains("/")||page.contains("\\")||page.contains(".."))
            throw new IllegalArgumentException("Invalid font atlas metadata");
        for(Glyph glyph:glyphs.values())if(glyph.x()<0||glyph.y()<0||glyph.width()<0||glyph.height()<0||
                (long)glyph.x()+glyph.width()>width||(long)glyph.y()+glyph.height()>height||glyph.advance()<0)
            throw new IllegalArgumentException("Glyph outside font atlas");
        return new BitmapFontMetrics(size,lineHeight,width,height,page,glyphs,kernings);
    }
    private static int number(Map<String,String> fields,String name) {
        if(!fields.containsKey(name))throw new IllegalArgumentException("Missing font field: "+name);
        return Integer.parseInt(fields.get(name));
    }
    private static long pair(int a,int b){return ((long)a<<32)|(b&0xffffffffL);}
    Glyph glyph(int point){return glyphs.getOrDefault(point,glyphs.get((int)'?'));}
    int kerning(int previous,int point){return kernings.getOrDefault(pair(previous,point),0);}
    float width(String value,float scale) {
        float current=0,max=0;int previous=-1;
        for(int index=0;index<value.length();) {
            int point=value.codePointAt(index);index+=Character.charCount(point);
            if(point=='\n'){max=Math.max(max,current);current=0;previous=-1;continue;}
            Glyph glyph=glyph(point);if(glyph==null)continue;
            current+=(kerning(previous,point)+glyph.advance())*scale;previous=point;
        }
        return Math.max(current,max);
    }
}
