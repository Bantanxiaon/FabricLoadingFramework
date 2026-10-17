package fabric.loading.fabric;
import net.fabricmc.api.EnvType;import net.fabricmc.loader.api.*;import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.spongepowered.asm.mixin.extensibility.*;import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
import fabric.loading.core.Settings;import com.google.gson.*;
import java.nio.file.*;import java.security.*;import java.util.*;import java.util.jar.*;
public final class FabricMixinPlugin implements IMixinConfigPlugin {
 private record Binding(String target,String kind,String method,String key){}
 private final Map<String,Binding> bindings=new LinkedHashMap<>();
 private String pkg;
 private void bind(String target,String kind,String method,String key){String name="Hook"+bindings.size();bindings.put(pkg+"."+name,new Binding(target,kind,method,key));}
 public void onLoad(String mixinPackage){
  pkg=mixinPackage;
  try{
   var loader=FabricLoader.getInstance();
   if(loader.getEnvironmentType()!=EnvType.CLIENT||!System.getProperty("os.name","").startsWith("Windows"))return;
   String game=loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
   String version=loader.getModContainer("fabricloader").orElseThrow().getMetadata().getVersion().getFriendlyString();
   if(!FabricAccess.versions(game,version)||!Settings.load(Settings.directory(loader.getGameDir())).enabled)return;
   prepareGame();
   prepareTasks();
   Path dir=Settings.directory(loader.getGameDir()).resolve("cache");Files.createDirectories(dir);
   if(!dir.toRealPath().startsWith(loader.getGameDir().toRealPath()))throw new IllegalStateException("Cache link escapes game directory");
   Map<String,byte[]> markerClasses=new LinkedHashMap<>();
   for(var e:bindings.entrySet())markerClasses.put(e.getKey(),HookWeaver.marker(e.getKey(),e.getValue().target()));
   Path cache=MixinCache.store(dir,markerClasses);
   FabricLauncherBase.getLauncher().addToClassPath(cache,pkg+".");
   FabricHooks.prepared=true;
  }catch(Throwable error){bindings.clear();FabricHooks.prepared=false;FabricHooks.warn(error);}
 }
 private void prepareGame(){
  String window=FabricAccess.mappedClass("net.minecraft.class_1041","com.mojang.blaze3d.platform.Window");
  String client=FabricAccess.mappedClass("net.minecraft.class_310","net.minecraft.client.Minecraft");
  String overlay=FabricAccess.mappedClass("net.minecraft.class_425","net.minecraft.client.gui.screens.LoadingOverlay");
  String reload=FabricAccess.mappedClass("net.minecraft.class_4011","net.minecraft.server.packs.resources.ReloadInstance");
  ClassNode wn=FabricAccess.node(window),cn=FabricAccess.node(client),on=FabricAccess.node(overlay),rn=FabricAccess.node(reload);
  if(FabricLauncherBase.getLauncher().isClassLoaded(window)||FabricLauncherBase.getLauncher().isClassLoaded(client)||FabricLauncherBase.getLauncher().isClassLoaded(overlay))throw new IllegalStateException("Game target already initialized");
  if(wn==null||cn==null||on==null||rn==null)throw new IllegalStateException("Unknown client class layout");
  String handle=choose(wn,"()J",FabricAccess.method("net.minecraft.class_1041","method_4490","()J","getWindow"),"handle","getWindow","getHandle");
  String render=choose(cn,"(Z)V",FabricAccess.method("net.minecraft.class_310","method_1523","(Z)V","runTick"),"runTick");
  String progress=choose(rn,"()F",FabricAccess.method("net.minecraft.class_4011","method_18229","()F","getActualProgress"),"getActualProgress");
  String done=choose(rn,"()Z",FabricAccess.method("net.minecraft.class_4011","method_18787","()Z","isDone"),"isDone");
  String future=choose(rn,"()Ljava/util/concurrent/CompletableFuture;",FabricAccess.method("net.minecraft.class_4011","method_18364","()Ljava/util/concurrent/CompletableFuture;","done"),"done");
  boolean reloadConstructor=on.methods.stream().anyMatch(m->m.name.equals("<init>")&&Arrays.stream(Type.getArgumentTypes(m.desc)).anyMatch(t->t.getClassName().equals(reload)));
  if(!reloadConstructor)throw new IllegalStateException("Unknown reload constructor");
  String baseOverlay=FabricAccess.mappedClass("net.minecraft.class_4071","net.minecraft.client.gui.screens.Overlay"),setterDesc="(L"+baseOverlay.replace('.','/')+";)V";
  String set=FabricAccess.method("net.minecraft.class_310","method_18502","(Lnet/minecraft/class_4071;)V","setOverlay");
  String setterOwner=client;ClassNode setter=cn;
  if(!FabricAccess.has(cn,set,setterDesc)){
   setterOwner="net.minecraft.client.gui.Gui";setter=FabricAccess.node(setterOwner);set="setOverlay";
   if(!FabricAccess.has(setter,set,setterDesc))throw new IllegalStateException("Unknown overlay handoff");
  }
  Set<String> nativeCalls=new HashSet<>();
  for(var method:wn.methods)for(var instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode call){
   if(call.owner.startsWith("org/lwjgl/sdl/"))nativeCalls.add("sdl");if(call.owner.startsWith("org/lwjgl/glfw/"))nativeCalls.add("glfw");
  }
  if(nativeCalls.size()!=1)throw new IllegalStateException("Unknown native window backend");
  FabricHooks.backend=nativeCalls.iterator().next();FabricHooks.reloadType=reload;
  FabricHooks.windowHandleMethod=handle;FabricHooks.reloadProgressMethod=progress;FabricHooks.reloadDoneMethod=done;FabricHooks.reloadFutureMethod=future;
  bind(window,"window","<init>","");
  bind(client,"tick",render,"");bind(overlay,"reload","<init>",reload);
  bind(setterOwner,"overlay",set,setterDesc);
  String close=FabricAccess.method("net.minecraft.class_310","method_1490","()V","close");
  if(FabricAccess.has(cn,close,"()V"))bind(client,"close",close,"");
 }
 private static String choose(ClassNode n,String desc,String... options){for(String name:options)if(FabricAccess.has(n,name,desc))return name;throw new IllegalStateException("Required method shape unavailable: "+desc);}
 private void prepareTasks(){
  var launcher=FabricLauncherBase.getLauncher();
  Map<String,TaskRegistry.Plan> unique=new LinkedHashMap<>();Set<String> ambiguous=new HashSet<>();
  for(var mod:FabricLoader.getInstance().getAllMods()){
   String id=mod.getMetadata().getId();TaskRegistry.name(id,mod.getMetadata().getName());
   if(id.equals("fabricloadingframework"))continue;
   try{
    var path=mod.findPath("fabric.mod.json");if(path.isEmpty()||Files.size(path.get())>1_000_000)continue;
    var json=JsonParser.parseString(Files.readString(path.get())).getAsJsonObject();if(!json.has("entrypoints"))continue;
    var entries=json.getAsJsonObject("entrypoints");
    for(String kind:List.of("preLaunch","main","client")){
     if(!entries.has(kind)||!entries.get(kind).isJsonArray())continue;
     for(var entry:entries.getAsJsonArray(kind)){
      String value;
      if(entry.isJsonPrimitive()&&entry.getAsJsonPrimitive().isString())value=entry.getAsString();
      else if(entry.isJsonObject()){var obj=entry.getAsJsonObject();if(obj.has("adapter")&&!obj.get("adapter").getAsString().equals("default"))continue;value=obj.get("value").getAsString();}
      else continue;
      String[] parts=value.split("::",-1);if(parts.length>2)continue;
      String target=parts[0],method=parts.length==2?parts[1]:switch(kind){case "main"->"onInitialize";case "client"->"onInitializeClient";default->"onPreLaunch";};
      if(!target.matches("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)+")||!method.matches("[A-Za-z_$][\\w$]*")||launcher.isClassLoaded(target))continue;
      ClassNode node=FabricAccess.node(target);if(node==null||(node.access&Opcodes.ACC_INTERFACE)!=0||node.methods.stream().noneMatch(m->m.name.equals(method)&&m.desc.equals("()V")&&(m.access&(Opcodes.ACC_ABSTRACT|Opcodes.ACC_NATIVE))==0))continue;
      String key=target+"#"+method;var plan=new TaskRegistry.Plan(key,id,switch(kind){case "main"->"公共初始化";case "client"->"客户端初始化";default->"启动前初始化";});
      if(unique.putIfAbsent(key,plan)!=null)ambiguous.add(key);
     }
    }
   }catch(Exception|LinkageError ignored){/* Optional task observation may not block a mod. */}
  }
  if(unique.size()>4096)throw new IllegalStateException("Too many initializer hooks");
  for(var p:unique.values())if(!ambiguous.contains(p.key())){TaskRegistry.plan(p);int split=p.key().lastIndexOf('#');bind(p.key().substring(0,split),"task",p.key().substring(split+1),p.key());}
 }
 public String getRefMapperConfig(){return null;}
 public boolean shouldApplyMixin(String target,String mixin){return bindings.containsKey(mixin);}
 public void acceptTargets(Set<String> own,Set<String> other){}
 public List<String> getMixins(){return bindings.keySet().stream().map(n->n.substring(pkg.length()+1)).toList();}
 public void preApply(String name,ClassNode target,String mixin,IMixinInfo info){}
 public void postApply(String name,ClassNode target,String mixin,IMixinInfo info){
  Binding b=bindings.get(mixin);if(b==null)return;
  for(MethodNode method:target.methods)if(method.name.equals(b.method())){
   switch(b.kind()){
    case "task"->{if(method.desc.equals("()V"))HookWeaver.callback(method,b.key());}
    case "window"->HookWeaver.onReturn(method,"window",true);
    case "tick"->{if(method.desc.equals("(Z)V"))HookWeaver.onReturn(method,"tick",false);}
    case "close"->{if(method.desc.equals("()V"))HookWeaver.onReturn(method,"close",false);}
    case "overlay"->{if(method.desc.equals(b.key()))HookWeaver.objectArgAtReturn(method,0,"overlay");}
    case "reload"->{var args=Type.getArgumentTypes(method.desc);for(int i=0;i<args.length;i++)if(args[i].getClassName().equals(b.key()))HookWeaver.objectArgAtReturn(method,i,"reload");}
   }
  }
 }
}
