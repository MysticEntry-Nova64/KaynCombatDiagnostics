package kayn.diagnostics;

import java.io.IOException;
import java.util.*;

public class ArchiveQueueTest {
    static int assertions;
    static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    static final class Fixture {
        boolean failing = true, badReporter;
        int writes, warnings;
        final Map<String,String> saved = new HashMap<>();
        final DiagnosticRecorder recorder = new DiagnosticRecorder((name,text) -> {
            writes++;
            if (failing) throw new IOException("injected disk failure");
            saved.put(name,text);
        }, (operation,name,error) -> {
            warnings++;
            if (badReporter) throw new IllegalStateException("injected reporter failure");
        });
    }
    static String file(String session) { return "kayn_combat_diagnostics_"+session+".tsv"; }

    static void recovery() {
        Fixture f = new Fixture();
        f.recorder.archive("old","original");
        f.recorder.beginSession();
        f.recorder.archive("new","newer");
        f.failing=false;
        f.recorder.advance(4.99f);
        check(f.writes==2,"no early retry");
        f.recorder.advance(.02f);
        check(f.saved.get(file("old")).equals("original"),"old snapshot survives new measurement");
        check(f.recorder.status().contains("ARCHIVE FAILED"),"second pending report keeps warning");
        f.recorder.advance(100);
        check(f.saved.get(file("new")).equals("newer"),"large delta retries at most one report");
        check(f.recorder.status().equals("Recording: archived"),"all recovered warnings clear");
        f.recorder.advance(100);
        check(f.writes==4,"saved archives are not written again");
    }
    static void limits() {
        Fixture f = new Fixture();
        for(int i=0;i<DiagnosticRecorder.MAX_PENDING+1;i++)f.recorder.archive("s"+i,"text"+i);
        check(f.recorder.status().contains("queue limit"),"capacity loss explicitly reported");
        f.failing=false;
        for(int i=0;i<DiagnosticRecorder.MAX_PENDING;i++)f.recorder.advance(5);
        check(f.saved.size()==DiagnosticRecorder.MAX_PENDING,"queue bounded by report count");
        check(!f.saved.containsKey(file("s"+DiagnosticRecorder.MAX_PENDING)),"overflow does not evict older reports");
        check(f.recorder.status().contains("queue limit"),"successful writes cannot hide unqueued loss");
        Fixture bytes = new Fixture();
        bytes.recorder.archive("oversized","x".repeat(DiagnosticRecorder.MAX_PENDING_CHARS+1));
        bytes.failing=false;bytes.recorder.advance(5);
        check(bytes.saved.isEmpty(),"oversized report is not retained");
        check(bytes.recorder.status().contains("queue limit"),"oversized warning retained");
        Fixture sum = new Fixture();
        sum.recorder.archive("full","x".repeat(DiagnosticRecorder.MAX_PENDING_CHARS));
        sum.recorder.archive("extra","x");
        sum.failing=false;sum.recorder.advance(5);sum.recorder.advance(5);
        check(sum.saved.size()==1&&sum.saved.containsKey(file("full")),"total character budget is bounded");
        sum.recorder.archive("after","x");
        check(sum.saved.containsKey(file("after")),"saving pending data frees its budget");
    }
    static void exhaustion() {
        Fixture f=new Fixture();f.recorder.archive("first","report");
        for(int i=0;i<100;i++)f.recorder.advance(5);
        check(f.writes==DiagnosticRecorder.MAX_ATTEMPTS,"attempts stop at configured limit");
        check(f.warnings==2,"initial and exhausted warnings only");
        check(f.recorder.status().contains("1 exhausted"),"exhausted report stays retained and visible");
        f.failing=false;f.recorder.archive("first","replacement");
        check(f.saved.get(file("first")).equals("report"),"explicit retry keeps original immutable data");
        check(f.recorder.status().equals("Recording: archived"),"explicit recovery clears exhaustion");
        Fixture rearm=new Fixture();rearm.recorder.archive("rearm","preserved");
        for(int i=0;i<10;i++)rearm.recorder.advance(5);
        rearm.failing=false;rearm.recorder.retryPendingArchives();rearm.recorder.advance(5);
        check(rearm.saved.get(file("rearm")).equals("preserved"),"explicit re-enable can recover an exhausted report");
    }
    static void fairnessAndReporter() {
        List<String> attempts=new ArrayList<>();
        DiagnosticRecorder recorder=new DiagnosticRecorder((name,text)->{
            attempts.add(name);
            if(name.equals(file("bad"))||attempts.size()<=2)throw new IOException("injected");
        },(operation,name,error)->{throw new IllegalStateException("injected notification error");});
        recorder.archive("bad","a");recorder.archive("good","b");
        recorder.advance(5);recorder.advance(5);
        check(attempts.get(3).equals(file("good")),"bad file does not starve later archive");
        Fixture f=new Fixture();f.badReporter=true;f.recorder.archive("safe","safe");
        f.failing=false;f.recorder.advance(5);
        check(f.saved.get(file("safe")).equals("safe"),"reporter exception cannot prevent queued recovery");
        Fixture invalid=new Fixture();invalid.recorder.archive("x","x");
        invalid.recorder.advance(Float.NaN);invalid.recorder.advance(Float.POSITIVE_INFINITY);invalid.recorder.advance(-1);
        check(invalid.writes==1,"invalid time does not trigger retries");
    }
    public static void main(String[] args) {
        recovery();limits();exhaustion();fairnessAndReporter();
        System.out.println("PASS: "+assertions+" archive queue assertions");
    }
}
