package fabric.loading.fabric;
import fabric.loading.core.ModTaskObserver.Task;
import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class TaskRegistry{
 public record Plan(String key,String id,String step){}
 private static final Map<String,Plan> plans=new ConcurrentHashMap<>();
 private static final Map<String,String> names=new ConcurrentHashMap<>();
 private static final Map<String,Task> active=new ConcurrentHashMap<>();
 private static final Set<String> finished=ConcurrentHashMap.newKeySet();
 private record Invocation(String plan,String token){}
 private static final ThreadLocal<Deque<Invocation>> stack=ThreadLocal.withInitial(ArrayDeque::new);
 public static final AtomicLong started=new AtomicLong(),ended=new AtomicLong();
 private static final AtomicLong sequence=new AtomicLong();
 public static void plan(Plan p){plans.putIfAbsent(p.key(),p);}
 public static int planned(){return plans.size();}public static int completed(){return finished.size();}
 public static void name(String id,String n){names.put(id,n);}
 public static String display(String id){return names.getOrDefault(id,id);}
 public static void enter(String key){
  Plan p=plans.get(key);if(p==null)return;
  String token=Thread.currentThread().getId()+":"+sequence.incrementAndGet();
  stack.get().push(new Invocation(key,token));active.put(token,new Task(token,p.id(),p.step(),System.nanoTime(),Thread.currentThread().getName()));started.incrementAndGet();
 }
 public static void exit(String key){
  Deque<Invocation> s=stack.get();if(s.isEmpty()||!s.peek().plan().equals(key))return;
  active.remove(s.pop().token());finished.add(key);ended.incrementAndGet();if(s.isEmpty())stack.remove();
 }
 public static List<Task> current(){return active.values().stream().sorted(Comparator.comparingLong(Task::start)).toList();}
 public static void clearActive(){active.clear();stack.remove();}
 private TaskRegistry(){}
}
