package fabric.loading.fabric;
/** Hook failures cannot escape into a game's constructor or another mod's initializer. */
public final class FabricHooks {
 public static volatile boolean prepared;public static volatile String backend="",reloadType="",windowHandleMethod="",reloadProgressMethod="",reloadDoneMethod="",reloadFutureMethod="";
 public static void enter(String key){try{FabricController.ensureStarted();TaskRegistry.enter(key);}catch(Throwable e){warn(e);}}
 public static void exit(String key){try{TaskRegistry.exit(key);}catch(Throwable e){warn(e);}}
 public static void failed(String key){try{TaskRegistry.exit(key);FabricController.close();}catch(Throwable e){warn(e);}}
 public static void window(Object window){try{FabricController.window(window);}catch(Throwable e){warn(e);FabricController.close();}}
 public static void reload(Object reload){try{FabricController.reload(reload);}catch(Throwable e){warn(e);FabricController.close();}}
 public static void overlay(Object overlay){try{FabricController.overlay(overlay);}catch(Throwable e){warn(e);FabricController.close();}}
 public static void tick(){try{FabricController.tick();}catch(Throwable e){warn(e);FabricController.close();}}
 public static void close(){try{FabricController.close();}catch(Throwable e){warn(e);}}
 public static void warn(Throwable e){System.err.println("[FabricLoadingFramework] Animation disabled: "+e.getClass().getSimpleName()+": "+e.getMessage());}
 private FabricHooks(){}
}
