package kayn.diagnostics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.*;

/** Test-only display examples. No reflection, API proxies or live combat setters. */
public final class MediaLab extends BaseEveryFrameCombatPlugin {
    private CombatEngineAPI engine;
    private final DiagnosticPanel panel=new DiagnosticPanel();
    private String command="actual";
    private long lastRead;
    private String readout="";
    private final Set<String> subjects=new LinkedHashSet<>();
    public void init(CombatEngineAPI engine){this.engine=engine;}
    public void advance(float amount,List<InputEventAPI> events) {
        if(System.nanoTime()-lastRead<250_000_000L)return;lastRead=System.nanoTime();
        try {
            command=Global.getSettings().readTextFileFromCommon("kayn_diagnostics_media_command.txt").trim();
            Object value=engine.getCustomData().get(CombatDiagnostics.KEY);
            if(!(value instanceof CombatDiagnostics diagnostic))return;
            diagnostic.overlay=command.equals("actual"); // Only the diagnostic's own display switch.
            if(engine.getPlayerShip()!=null)subjects.add(engine.getPlayerShip().getHullSpec().getHullId());
            Global.getSettings().writeTextFileToCommon("kayn_diagnostics_lab_observation.txt",
                "subjects="+subjects+"\ncommand="+command+"\n"+diagnostic.snapshot(""));
            if(command.equals("actual"))return;
            int phase=(int)((System.nanoTime()/800_000_000L)%3);
            boolean active=phase!=0;
            StringBuilder body=new StringBuilder("WS 001 Display example | "+(active?"FIRING":"IDLE")+"\nAUTO\n");
            int column=0;
            for(WeaponRestriction restriction:WeaponRestriction.values()) {
                if(active&&(command.equals(restriction.label)||command.equals("Multiple"))) {
                    if(column>0)body.append("{#ffffff} | ");
                    body.append(restriction.markup());
                    if(++column==4){body.append('\n');column=0;}
                }
            }
            body.append("{#ffffff}\n");
            if(command.equals("Beam"))body.append("Beam ").append(phase*.25f).append("s | Hit frames ").append(phase*4).append('\n');
            else body.append("Projectiles/impacts ").append(phase*3).append('/').append(phase).append('\n');
            String recording=command.equals("Recording")&&active?"Recording warning (fixture)":"Recording";
            readout="CONTROLLED DISPLAY FIXTURE | "+command+"\n"+
                "Illustration only: not live ship/AI measurements\n"+
                DiagnosticHud.title(phase,recording)+
                ObservationFreshness.label(command.equals("Freshness")&&active,false,false,phase*.25f)+"\n"+
                (command.equals("Help")?DiagnosticHud.help(DiagnosticKeys.DEFAULT):"")+body;
        } catch(Exception error){Global.getLogger(MediaLab.class).error("KAYN_LAB test helper failure",error);}
    }
    public void renderInUICoords(ViewportAPI viewport) {
        if(!command.equals("actual")&&!readout.isEmpty())panel.draw(readout,engine.isPaused(),0);
    }
}
