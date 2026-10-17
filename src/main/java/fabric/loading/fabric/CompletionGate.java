package fabric.loading.fabric;
/** Completion follows successful resource work plus a visible game-frame handoff, never a timeout. */
public final class CompletionGate{
 public enum Decision{WAIT,FADE,ABORT}
 private long frames,clearedAt;private boolean cleared,released;
 public synchronized void overlay(boolean present){cleared=!present;if(cleared)clearedAt=frames;}
 public synchronized Decision frame(boolean done,boolean success){
  frames++;if(released)return Decision.WAIT;
  if(done&&!success){released=true;return Decision.ABORT;}
  if(done&&success&&cleared&&frames>clearedAt+1){released=true;return Decision.FADE;}
  return Decision.WAIT;
 }
}