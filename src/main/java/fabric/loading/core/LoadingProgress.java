package fabric.loading.core;
import fabric.loading.fabric.TaskRegistry;
public final class LoadingProgress implements ProgressProvider {
 public volatile double value;public volatile String stage="正在准备 Fabric";
 private double resource;private int mods;private boolean complete;
 public synchronized void mods(int n){mods=Math.max(0,n);}
 public synchronized void tick(){
  if(complete)return;
  int count=TaskRegistry.planned(),done=TaskRegistry.completed();
  double observed=(Math.min(count,done)+100*resource)/(count+100.0);
  value=Math.max(value,Math.min(.99,observed));
  stage=resource>0?"正在加载客户端资源":done<count?"正在初始化模组":"正在准备客户端资源";
 }
 public synchronized void resource(float f){if(Float.isFinite(f))resource=Math.max(resource,Math.max(0,Math.min(1,f)));tick();}
 public synchronized void complete(){complete=true;value=1;stage="初始化完成";}
 public synchronized Snapshot snapshot(){tick();if(complete)value=1;return new Snapshot(value,stage,complete?Accuracy.COMPLETE:Accuracy.ESTIMATE,resource,mods,mods,"Fabric 初始化回调与资源加载");}
}
