package kayn.diagnostics;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.*;
import com.fs.starfarer.api.loading.*;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

public class RecordingReliabilityTest {
 static int assertions;
 static void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
 static final class Fixture {
  boolean failLive,failArchive,failSnapshot,failMessage;
  final List<String> attempts=new ArrayList<>(),messages=new ArrayList<>();
  final Map<String,String> files=new HashMap<>();
  final List<DamageListener> listeners=new ArrayList<>();
  final CombatDiagnostics diagnostic=new CombatDiagnostics("recording failure fixture");
  Fixture(){
   SettingsAPI settings=(SettingsAPI)Proxy.newProxyInstance(SettingsAPI.class.getClassLoader(),new Class[]{SettingsAPI.class},(p,m,a)->{
    if(m.getName().equals("writeTextFileToCommon")){
     String filename=(String)a[0];attempts.add(filename);
     if(filename.equals(CombatDiagnostics.FILE)?failLive:failArchive)throw new IOException("controlled write failure");
     files.put(filename,(String)a[1]);
    }
    return null;
   });Global.setSettings(settings);
   ShipHullSpecAPI hull=DiagnosticsIntegrationTest.fake(ShipHullSpecAPI.class,(n,a)->switch(n){case "getHullId","getHullName"->"fixture";default->null;});
   FluxTrackerAPI flux=DiagnosticsIntegrationTest.fake(FluxTrackerAPI.class,(n,a)->n.equals("getMaxFlux")?1000f:null);
   ShipAPI ship=DiagnosticsIntegrationTest.fake(ShipAPI.class,(n,a)->{
    if(n.equals("getHullSpec")){if(failSnapshot)throw new IllegalStateException("controlled snapshot failure");return hull;}
    if(n.equals("addListener"))listeners.add((DamageListener)a[0]);
    if(n.equals("removeListener"))listeners.remove(a[0]);
    return switch(n){case "getFluxTracker"->flux;case "getId"->"fixture-ship";case "getLocation"->new Vector2f();case "isAlive"->true;default->null;};
   });
   CombatUIAPI ui=DiagnosticsIntegrationTest.fake(CombatUIAPI.class,(n,a)->{if(n.equals("addMessage")){if(failMessage)throw new IllegalStateException("controlled notification failure");messages.add(Arrays.deepToString(a));}return null;});
   CombatEngineAPI engine=DiagnosticsIntegrationTest.fake(CombatEngineAPI.class,(n,a)->switch(n){case "getCustomData"->new HashMap<>();case "getPlayerShip"->ship;case "getShips"->List.of(ship);case "getCombatUI"->ui;case "isUIShowingHUD","isEntityInPlay"->true;default->null;});
   diagnostic.init(engine);
  }
  void enable(){DiagnosticsIntegrationTest.press(diagnostic,org.lwjgl.input.Keyboard.KEY_F8);diagnostic.advance(.5f,List.of());}
  void disable(){DiagnosticsIntegrationTest.press(diagnostic,org.lwjgl.input.Keyboard.KEY_F8);}
  long liveAttempts(){return attempts.stream().filter(CombatDiagnostics.FILE::equals).count();}
 }
 static void archiveFailure(){
  Fixture f=new Fixture();f.enable();f.failArchive=true;f.disable();
  check(f.messages.stream().anyMatch(s->s.contains("archive")&&s.contains("failed")),"archive failure must be visible even after disabling the overlay");
  check(f.messages.stream().anyMatch(s->s.contains("kayn_combat_diagnostics_")&&s.contains(".tsv")),"failure message identifies the archive file");
  check(f.listeners.isEmpty(),"archive failure does not retain listeners");
  check(f.diagnostic.snapshot("").contains("ended=true"),"failed archive preserves finalized in-memory snapshot");
 }
 static void liveFailure(){
  Fixture f=new Fixture();f.failLive=true;f.enable();f.diagnostic.advance(2,List.of());
  check(f.liveAttempts()==1,"live failure does not retry every frame");
  f.disable();
  check(f.files.keySet().stream().anyMatch(s->!s.equals(CombatDiagnostics.FILE)),"live-file failure must not suppress the final archive attempt");
  check(f.listeners.isEmpty(),"live failure does not prevent listener cleanup");
  check(f.messages.stream().anyMatch(s->s.contains("live")&&s.contains("failed")),"live recording failure is visible");
  f.failLive=false;f.enable();
  check(f.files.containsKey(CombatDiagnostics.FILE),"starting a fresh measurement retries live recording");
  f.disable();
 }
 static void snapshotFailure(){
  Fixture f=new Fixture();f.enable();f.failSnapshot=true;
  try{f.disable();throw new AssertionError("expected controlled snapshot failure");}catch(IllegalStateException expected){}
  check(f.listeners.isEmpty(),"finalization must detach listeners even if snapshot generation throws");
 }
 static void notificationFailure(){
  Fixture f=new Fixture();f.failLive=true;f.failMessage=true;f.enable();f.disable();
  check(f.files.keySet().stream().anyMatch(s->!s.equals(CombatDiagnostics.FILE)),"a broken UI notification must not suppress the archive");
  check(f.listeners.isEmpty(),"notification failure does not retain listeners");
 }
 static void recordingStatus(){
  boolean[] fail={false,false};
  DiagnosticRecorder recorder=new DiagnosticRecorder((name,text)->{if(name.equals(CombatDiagnostics.FILE)?fail[0]:fail[1])throw new IOException("controlled");},(operation,name,error)->{});
  recorder.beginSession();check(recorder.status().contains("live"),"healthy recording status");
  fail[1]=true;recorder.archive("first","snapshot");check(recorder.status().contains("ARCHIVE FAILED"),"archive failure is represented in panel status");
  recorder.beginSession();check(recorder.status().contains("ARCHIVE FAILED"),"a new session must not hide a previous archive failure");
  fail[1]=false;recorder.archive("second","snapshot");check(recorder.status().equals("Recording: archived"),"successful archive clears the warning");
  recorder.beginSession();fail[0]=true;recorder.writeLive("snapshot");check(recorder.status().contains("LIVE FAILED"),"live-file failure is represented in panel status");
  recorder.archive("third","snapshot");check(recorder.status().contains("archived")&&recorder.status().contains("live file failed"),"saved archive is distinguished from a failed live file");
  fail[0]=false;recorder.beginSession();recorder.writeLive("snapshot");check(!recorder.status().contains("failed")&&!recorder.status().contains("FAILED"),"new live recording can recover");
 }
 static void archiveRetry() throws IOException {
  java.nio.file.Path folder=java.nio.file.Files.createTempDirectory("kayn-archive-retry-");
  boolean[] fail={true};
  String snapshot="session=retry-fixture\nended=true\n";
  DiagnosticRecorder recorder=new DiagnosticRecorder((name,text)->{
   if(fail[0])throw new IOException("controlled first-attempt failure");
   java.nio.file.Files.writeString(folder.resolve(name),text);
  },(operation,name,error)->{});
  try {
   recorder.archive("retry-fixture",snapshot);
   check(recorder.status().contains("ARCHIVE FAILED"),"first archive attempt fails visibly");
   fail[0]=false;
   recorder.archive("retry-fixture",snapshot);
   check(java.nio.file.Files.readString(folder.resolve("kayn_combat_diagnostics_retry-fixture.tsv")).equals(snapshot),"retry writes the exact same session snapshot to disk");
   check(recorder.status().equals("Recording: archived"),"successful same-session retry clears the failure");
  } finally {
   java.nio.file.Files.deleteIfExists(folder.resolve("kayn_combat_diagnostics_retry-fixture.tsv"));
   java.nio.file.Files.delete(folder);
  }
 }
 public static void main(String[] args) throws IOException {
  if(args.length==0||args[0].equals("retry"))archiveRetry();
  if(args.length==0||args[0].equals("archive"))archiveFailure();
  if(args.length==0||args[0].equals("live"))liveFailure();
  if(args.length==0||args[0].equals("snapshot"))snapshotFailure();
  if(args.length==0||args[0].equals("notification"))notificationFailure();
  if(args.length==0||args[0].equals("status"))recordingStatus();
  System.out.println("PASS: "+assertions+" recording reliability assertions");
 }
}
