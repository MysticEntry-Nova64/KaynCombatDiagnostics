package kayn.diagnostics;

/** Dependency-free font contract checks used by hosted CI as well as the local game build. */
public class FontMetricsPortableTest {
    static final String DESCRIPTOR="""
        info face="Fixture" size=-16
        common lineHeight=19 base=15 scaleW=64 scaleH=64 pages=1
        page id=0 file="fixture.png"
        char id=32 x=0 y=0 width=1 height=1 xoffset=0 yoffset=15 xadvance=4 page=0
        char id=63 x=0 y=0 width=7 height=12 xoffset=0 yoffset=2 xadvance=8 page=0
        char id=65 x=8 y=0 width=8 height=12 xoffset=1 yoffset=2 xadvance=10 page=0
        char id=86 x=16 y=0 width=8 height=12 xoffset=1 yoffset=2 xadvance=10 page=0
        kerning first=65 second=86 amount=-2
        """;
    static int count;
    static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        BitmapFontMetrics metrics=BitmapFontMetrics.parse(DESCRIPTOR);
        check(metrics.size==16&&metrics.lineHeight==19,"negative-size stock font convention");
        check(metrics.width("AV",1)==18&&metrics.width("AV",2)==36,"scaled kerning width");
        check(metrics.width("AV\nA",1)==18,"multiline longest width");
        check(metrics.width("\ud83d\ude80",1)==8,"one fallback per Unicode codepoint");
        check(metrics.width("",1)==0,"empty text has no width");
        for(String bad:java.util.List.of(DESCRIPTOR.replace("pages=1","pages=2"),DESCRIPTOR.replace("fixture.png","../bad.png"),DESCRIPTOR.replace("x=16","x=63"))) {
            boolean rejected=false;try{BitmapFontMetrics.parse(bad);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"invalid font descriptor rejected");
        }
        if(args.length>0)check(BitmapFontMetrics.parse(java.nio.file.Files.readString(java.nio.file.Path.of(args[0]))).glyph('A')!=null,"actual installed font descriptor");
        System.out.println("PASS: "+count+" portable font assertions");
    }
}
