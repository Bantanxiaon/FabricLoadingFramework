import fabric.loading.fabric.*;
import net.fabricmc.api.EnvType;import net.fabricmc.loader.impl.launch.*;
import org.spongepowered.asm.launch.MixinBootstrap;import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.extensibility.*;import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.objectweb.asm.tree.*;import java.util.*;import java.nio.file.*;import java.net.*;import java.io.*;import java.lang.reflect.*;
public final class MixinEngineSmoke {
 public static final class Plugin implements IMixinConfigPlugin{
  public void onLoad(String p){}public String getRefMapperConfig(){return null;}public boolean shouldApplyMixin(String t,String m){return true;}public void acceptTargets(Set<String> a,Set<String>b){}public List<String> getMixins(){return List.of("Marker");}
  public void preApply(String t,ClassNode n,String m,IMixinInfo i){}
  public void postApply(String t,ClassNode n,String m,IMixinInfo i){for(MethodNode method:n.methods)if(method.name.equals("onInitialize"))HookWeaver.callback(method,"engine");}
 }
 public static void main(String[] args)throws Exception{
  Path classes=Path.of(args[0]),work=Path.of(args[1]);Files.createDirectories(work.resolve("engine/marker"));
  Files.write(work.resolve("engine/marker/Marker.class"),HookWeaver.marker("engine.marker.Marker","fixtures.Initializer"));
  Files.writeString(work.resolve("engine.mixins.json"),"{\"required\":true,\"minVersion\":\"0.8\",\"package\":\"engine.marker\",\"plugin\":\"MixinEngineSmoke$Plugin\",\"compatibilityLevel\":\"JAVA_17\",\"mixins\":[]}");
  URLClassLoader resources=new URLClassLoader(new URL[]{work.toUri().toURL(),classes.toUri().toURL()},MixinEngineSmoke.class.getClassLoader());
  FabricLauncher proxy=(FabricLauncher)java.lang.reflect.Proxy.newProxyInstance(MixinEngineSmoke.class.getClassLoader(),new Class[]{FabricLauncher.class},(p,m,a)->switch(m.getName()){
   case "getEnvironmentType"->EnvType.CLIENT;
   case "getTargetClassLoader"->resources;
   case "getResourceAsStream"->resources.getResourceAsStream((String)a[0]);
   case "getClassByteArray"->{try(var in=resources.getResourceAsStream(((String)a[0]).replace('.','/')+".class")){yield in==null?null:in.readAllBytes();}}
   case "getClassPath"->List.of(classes,work);
   case "isClassLoaded","isDevelopment"->false;
   case "getTargetNamespace"->"intermediary";
   case "getEntrypoint"->"fixtures.Initializer";
   case "toString"->"Headless SDK test launcher";
   default->null;
  });
  Field launcher=FabricLauncherBase.class.getDeclaredField("launcher");launcher.setAccessible(true);launcher.set(null,proxy);
  Field properties=FabricLauncherBase.class.getDeclaredField("properties");properties.setAccessible(true);properties.set(null,new HashMap<String,Object>());
  System.setProperty("mixin.bootstrapService","net.fabricmc.loader.impl.launch.knot.MixinServiceKnotBootstrap");
  System.setProperty("mixin.service","net.fabricmc.loader.impl.launch.knot.MixinServiceKnot");
  MixinBootstrap.init();MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
  Mixins.addConfiguration("engine.mixins.json");
  Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
  Object active=MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
  if(!(active instanceof IMixinTransformer transformer))throw new AssertionError("Mixin transformer unavailable");
  byte[] original=Files.readAllBytes(classes.resolve("fixtures/Initializer.class"));
  byte[] output=transformer.transformClassBytes("fixtures.Initializer","fixtures.Initializer",original);
  if(Arrays.equals(original,output))throw new AssertionError("Marker postApply was not applied");
  TaskRegistry.plan(new TaskRegistry.Plan("engine","actual_mixin_fixture","客户端初始化"));
  Class<?> target=new ClassLoader(MixinEngineSmoke.class.getClassLoader()){Class<?> define(){return defineClass("fixtures.Initializer",output,0,output.length);}}.define();
  target.getField("action").set(null,(Runnable)()->{if(TaskRegistry.current().size()!=1)throw new AssertionError("No active Mixin callback");});
  target.getMethod("onInitialize").invoke(target.getConstructor().newInstance());
  if(!TaskRegistry.current().isEmpty()||TaskRegistry.completed()!=1)throw new AssertionError("Mixin callback was not cleared");
  System.out.println("PASS_REAL_MIXIN_ENGINE marker postApply / frame recomputation / exact callback");
 }
}