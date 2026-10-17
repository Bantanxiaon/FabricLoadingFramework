package fabric.loading.fabric;
/** Reversible child-window attachment. The backend is injected in headless tests. */
public final class NativeAttachment implements AutoCloseable {
 public interface Ops{
  boolean valid(long h);long pid(long h);long parent(long h);int style(long h,int index);
  void style(long h,int index,int value);void parent(long child,long parent);void fit(long child,long parent);void alpha(long child,int value);
 }
 private final Ops ops;private final long child,process;private long parent;private int parentStyle,childStyle,childEx;private boolean attached;
 public NativeAttachment(Ops ops,long child,long process){this.ops=ops;this.child=child;this.process=process;if(child==0||!ops.valid(child)||ops.pid(child)!=process)throw new IllegalArgumentException("Invalid owned child");}
 public synchronized void attach(long target){
  if(attached){if(parent==target)return;throw new IllegalStateException("Already attached");}
  if(target==0||target==child||!ops.valid(target)||ops.pid(target)!=process)throw new IllegalArgumentException("Invalid owned game window");
  parent=target;parentStyle=ops.style(parent,-16);childStyle=ops.style(child,-16);childEx=ops.style(child,-20);
  try{
   ops.style(child,-16,(childStyle&~(0x80000000|0x00C00000|0x000F0000))|0x40000000|0x10000000);
   ops.style(parent,-16,parentStyle|0x02000000);ops.parent(child,parent);
   if(ops.parent(child)!=parent||(ops.style(child,-16)&0x40000000)==0)throw new IllegalStateException("Child attachment failed");
   attached=true;ops.fit(child,parent);
  }catch(RuntimeException e){rollback();throw e;}
 }
 private void rollback(){
  if(ops.valid(child)){ops.parent(child,0);ops.style(child,-16,childStyle);ops.style(child,-20,childEx);}
  restoreParent();attached=false;parent=0;
 }
 private void restoreParent(){if(parent!=0&&ops.valid(parent)&&ops.pid(parent)==process){int current=ops.style(parent,-16);ops.style(parent,-16,(current&~0x02000000)|(parentStyle&0x02000000));}}
 public synchronized void fit(){if(attached&&parentAlive())ops.fit(child,parent);}
 public synchronized void opacity(float value){if(attached){ops.style(child,-20,ops.style(child,-20)|0x00080000);ops.alpha(child,Math.round(255*Math.max(0,Math.min(1,value))));}}
 public synchronized boolean attached(){return attached;}
 public synchronized boolean parentAlive(){return !attached||(ops.valid(child)&&ops.pid(child)==process&&ops.valid(parent)&&ops.pid(parent)==process&&ops.parent(child)==parent);}
 public synchronized void close(){restoreParent();attached=false;parent=0;}
}
