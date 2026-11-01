package kayn.diagnostics;

import java.util.*;

public class FontAndLayoutTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static String descriptor="""
        info face="Fixture" size=-16
        common lineHeight=19 base=15 scaleW=64 scaleH=64 pages=1
        page id=0 file="fixture.png"
        char id=32 x=0 y=0 width=1 height=1 xoffset=0 yoffset=15 xadvance=4 page=0
        char id=63 x=0 y=0 width=7 height=12 xoffset=0 yoffset=2 xadvance=8 page=0
        char id=65 x=8 y=0 width=8 height=12 xoffset=1 yoffset=2 xadvance=10 page=0
        char id=86 x=16 y=0 width=8 height=12 xoffset=1 yoffset=2 xadvance=10 page=0
        kerning first=65 second=86 amount=-2
        """;
    public static void main(String[] args) throws Exception {
        BitmapFontMetrics metrics=BitmapFontMetrics.parse(descriptor);
        check(metrics.size==16&&metrics.lineHeight==19&&metrics.page.equals("fixture.png"),"game-style negative font size and single atlas parsed");
        check(metrics.width("AV",1)==18&&metrics.width("AV",2)==36,"width includes kerning and scale");
        check(metrics.width("AV\nA",1)==18,"multiline width uses longest row");
        check(metrics.width("x",1)==8&&metrics.width("\ud83d\ude80",1)==8,"missing code point uses one fallback glyph, including non-BMP input");
        check(metrics.glyph('A').x()==8&&metrics.glyph('A').offsetX()==1,"glyph atlas and offset preserved");
        DiagnosticKeys custom=DiagnosticKeys.load(name->name.equals("kayn_diagnostics_toggle_key")?"F6":null);
        check(custom.code(DiagnosticKeys.Action.TOGGLE)==org.lwjgl.input.Keyboard.KEY_F6&&custom.code(DiagnosticKeys.Action.HELP)==org.lwjgl.input.Keyboard.KEY_F7,"one shortcut can be rebound without changing the others");
        check(DiagnosticHud.help(custom).contains("F6: recording on/off"),"help uses configured shortcuts");
        for(String invalid:List.of("F7","NOT_A_KEY")) {
            boolean rejected=false;try{DiagnosticKeys.load(name->name.equals("kayn_diagnostics_toggle_key")?invalid:null);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"duplicate or unsupported shortcut rejected");
        }
        for(String bad:List.of(descriptor.replace("pages=1","pages=2"),descriptor.replace("file=\"fixture.png\"","file=\"../bad.png\""),descriptor.replace("x=16","x=63"))) {
            boolean rejected=false;try{BitmapFontMetrics.parse(bad);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"invalid atlas metadata rejected");
        }
        List<List<DiagnosticPanel.Span>> wrapped=DiagnosticPanel.wrap("{#ffa040}AV AV{#80b8ff}AV",24,s->metrics.width(s,1));
        String joined=wrapped.stream().flatMap(Collection::stream).map(DiagnosticPanel.Span::text).collect(java.util.stream.Collectors.joining());
        check(joined.equals("AV AVAV")&&wrapped.size()>1,"wrapping preserves all evidence text");
        for(List<DiagnosticPanel.Span> row:wrapped)check(row.stream().mapToDouble(s->metrics.width(s.text(),1)).sum()<=24,"wrapped row fits measured budget");
        check(wrapped.get(0).get(0).color().equals(WeaponRestriction.BLOCKED.color),"wrapped label retains original colour");
        PanelLayout.Board shortBoard=PanelLayout.fit(1280,720,1,380,6,18),longBoard=PanelLayout.fit(1280,720,1,380,40,18);
        check(shortBoard.height()<longBoard.height(),"small readout produces a shorter board");
        check(longBoard.rows()<40&&longBoard.height()<=longBoard.top()-130,"large readout stays above bottom HUD and scrolls");
        for(int width:new int[]{800,1280,2560}) {
            PanelLayout.Board board=PanelLayout.fit(width,720,1,2000,25,18);
            check(board.x()+board.width()<=width-16&&board.width()<=620,"board respects screen and maximum width");
        }
        if(args.length>0) {
            String stock=java.nio.file.Files.readString(java.nio.file.Path.of(args[0]));
            BitmapFontMetrics actual=BitmapFontMetrics.parse(stock);
            check(actual.width("COMBAT DIAGNOSTICS | Recording",1)>0&&actual.glyph('A')!=null,"installed game descriptor parses without external font library");
        }
        System.out.println("PASS: "+checks+" font and board layout assertions");
    }
}
