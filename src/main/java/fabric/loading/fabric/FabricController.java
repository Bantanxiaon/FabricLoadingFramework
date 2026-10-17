package fabric.loading.fabric;
import fabric.loading.core.*;import net.fabricmc.loader.api.FabricLoader;
import javax.swing.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class FabricController {
 private static final AtomicBoolean starting=new AtomicBoolean(),closed=new AtomicBoolean();
 private static volatile MediaSession media;private static volatile FabricSurface surface;private static volatile Object reload;
 private static volatile long pendingWindow;
 private static volatile Method reloadProgress,reloadDone,reloadFuture;
 private static volatile boolean fade;private static final CompletionGate gate=new CompletionGate();
 public static volatile Thread startupThread;
 public static void ensureStarted(){
  if(!FabricHooks.prepared||closed.get()||!starting.compareAndSet(false,true))return;
  try{
   var loader=FabricLoader.getInstance();Path game=loader.getGameDir(),dir=Settings.directory(game);Settings settings=Settings.load(dir);
   if(!settings.enabled){closed.set(true);return;}
   if(settings.title==null||settings.title.isBlank())settings.title=PackName.resolve(game,loader.getLaunchArguments(true));
   // Fail closed before creating a window when resources or the native bridge are absent.
   if(!Files.isRegularFile(settings.resource(dir,settings.ffmpeg))||!Files.isRegularFile(settings.resource(dir,settings.video)))throw new IllegalStateException("Video or FFmpeg missing in fabric_loading");
   Class.forName("com.sun.jna.platform.win32.User32",false,FabricController.class.getClassLoader());
   startupThread=Thread.currentThread();MediaSession session=new MediaSession(dir,settings);media=session;
   session.progress.mods(loader.getAllMods().size());session.start();
   Runnable create=()->{FabricSurface view=new FabricSurface(session);surface=view;if(closed.get())view.close();};
   if(SwingUtilities.isEventDispatchThread())create.run();else SwingUtilities.invokeAndWait(create);
   if(pendingWindow!=0&&!closed.get())surface.attach(pendingWindow);
   Runtime.getRuntime().addShutdownHook(new Thread(FabricController::close,"FabricLoadingFramework-shutdown"));
  }catch(Throwable e){FabricHooks.warn(e);close();}
 }
 public static void window(Object window)throws ReflectiveOperationException{
  ensureStarted();if(closed.get())return;long handle=NativeHandle.resolve(window);pendingWindow=handle;FabricSurface view=surface;if(view!=null&&!closed.get())view.attach(handle);
 }
 public static synchronized void reload(Object value)throws ReflectiveOperationException{
  if(closed.get()||reload!=null)return;
  Class<?> type=Class.forName(FabricHooks.reloadType,false,value.getClass().getClassLoader());
  reloadProgress=type.getMethod(FabricHooks.reloadProgressMethod);reloadDone=type.getMethod(FabricHooks.reloadDoneMethod);reloadFuture=type.getMethod(FabricHooks.reloadFutureMethod);reload=value;
 }
 public static synchronized void overlay(Object value){if(reload!=null&&!closed.get())gate.overlay(value!=null);}
 public static synchronized void tick()throws ReflectiveOperationException{
  MediaSession session=media;Object state=reload;
  if(session==null||state==null||closed.get())return;
  float progress=((Number)reloadProgress.invoke(state)).floatValue();session.progress.resource(progress);
  boolean done=(Boolean)reloadDone.invoke(state);
  if(done){
   Object result=reloadFuture.invoke(state);
   if(!(result instanceof CompletableFuture<?> future)||!future.isDone())return;
   var decision=gate.frame(true,!future.isCompletedExceptionally()&&!future.isCancelled());
   if(decision==CompletionGate.Decision.ABORT){close();return;}
   if(decision==CompletionGate.Decision.FADE){session.progress.complete();session.beginFade();fade=true;session.log("FADE_BEGIN resources=success handoff=overlay-cleared");}
  }else gate.frame(false,false);
 }
 public static boolean fading(){return fade;}
 public static void close(){
  if(!closed.compareAndSet(false,true))return;
  FabricSurface view=surface;surface=null;if(view!=null)view.close();
  MediaSession session=media;media=null;if(session!=null){session.close();session.finishLogging();}
 }
 private FabricController(){}
}
