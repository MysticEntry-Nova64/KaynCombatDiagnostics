package kayn.diagnostics;

import java.util.*;
import java.util.function.Function;
import org.lwjgl.input.Keyboard;

/** Unique configurable Ctrl shortcuts; unrelated or consumed input is never intercepted. */
final class DiagnosticKeys {
    enum Action {
        TOGGLE("F8"), SUBJECT("F9"), PAGE("F10"), HELP("F7"), UP("UP"), DOWN("DOWN");
        final String defaultName;
        Action(String name){defaultName=name;}
        String setting(){return "kayn_diagnostics_"+name().toLowerCase(Locale.ROOT)+"_key";}
    }
    private static final Map<String,Integer> CODES=Map.ofEntries(
        Map.entry("F1",Keyboard.KEY_F1),Map.entry("F2",Keyboard.KEY_F2),Map.entry("F3",Keyboard.KEY_F3),
        Map.entry("F4",Keyboard.KEY_F4),Map.entry("F5",Keyboard.KEY_F5),Map.entry("F6",Keyboard.KEY_F6),
        Map.entry("F7",Keyboard.KEY_F7),Map.entry("F8",Keyboard.KEY_F8),Map.entry("F9",Keyboard.KEY_F9),
        Map.entry("F10",Keyboard.KEY_F10),Map.entry("F11",Keyboard.KEY_F11),Map.entry("F12",Keyboard.KEY_F12),
        Map.entry("UP",Keyboard.KEY_UP),Map.entry("DOWN",Keyboard.KEY_DOWN));
    static final DiagnosticKeys DEFAULT=load(key->null);
    private final Map<Action,String> names;
    private DiagnosticKeys(Map<Action,String> names){this.names=Map.copyOf(names);}
    static DiagnosticKeys load(Function<String,String> settings) {
        Map<Action,String> names=new EnumMap<>(Action.class);Set<String> seen=new HashSet<>();
        for(Action action:Action.values()) {
            String value=settings.apply(action.setting());
            String name=value==null?action.defaultName:value.trim().toUpperCase(Locale.ROOT);
            if(!CODES.containsKey(name)||!seen.add(name))throw new IllegalArgumentException("Invalid or duplicate diagnostic key: "+name);
            names.put(action,name);
        }
        return new DiagnosticKeys(names);
    }
    int code(Action action){return CODES.get(names.get(action));}
    String name(Action action){return names.get(action);}
    String footer(){return "Ctrl+"+name(Action.HELP)+" help | Ctrl+"+name(Action.UP)+"/"+name(Action.DOWN)+" scroll";}
}
