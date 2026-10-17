package fabric.loading.core;
import fabric.loading.fabric.TaskRegistry;
import java.util.*;import java.util.concurrent.atomic.AtomicLong;
public final class ModTaskObserver implements AutoCloseable{
 public record Task(String key,String id,String step,long start,String thread){}
 public final AtomicLong started=TaskRegistry.started,ended=TaskRegistry.ended;
 public void name(String id,String name){TaskRegistry.name(id,name);}
 public List<Task> current(){return TaskRegistry.current();}
 public String label(Task t){return TaskRegistry.display(t.id())+" ["+t.id()+"] · "+t.step();}
 public List<String> samples(){return List.of();}
 public void close(){TaskRegistry.clearActive();}
}
