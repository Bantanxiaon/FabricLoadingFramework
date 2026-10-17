import fabric.loading.fabric.*;import net.fabricmc.loader.api.*;import net.fabricmc.loader.impl.launch.*;
import java.lang.reflect.*;import java.nio.file.*;import java.util.*;import java.util.jar.*;
public final class GameAbiSmoke{
 public static void main(String[] args)throws Exception{
  Path game=Path.of(args[0]);
  FabricLauncher launcher=(FabricLauncher)Proxy.newProxyInstance(GameAbiSmoke.class.getClassLoader(),new Class[]{FabricLauncher.class},(p,m,a)->{
   if(m.getName().equals("getClassByteArray")){
    try(JarFile jar=new JarFile(game.toFile())){var entry=jar.getJarEntry(((String)a[0]).replace('.','/')+".class");return entry==null?null:jar.getInputStream(entry).readAllBytes();}
   }
   if(m.getName().equals("isClassLoaded")||m.getName().equals("isDevelopment"))return false;
   if(m.getName().equals("getTargetClassLoader"))return GameAbiSmoke.class.getClassLoader();
   return null;
  });
  Field installed=FabricLauncherBase.class.getDeclaredField("launcher");installed.setAccessible(true);installed.set(null,launcher);
  Object loader=FabricLoader.getInstance();Field mapping=loader.getClass().getDeclaredField("mappingResolver");mapping.setAccessible(true);
  Object resolver=Proxy.newProxyInstance(GameAbiSmoke.class.getClassLoader(),new Class[]{MappingResolver.class},(p,m,a)->m.getName().equals("getNamespaces")?List.of("named"):m.getName().equals("getCurrentRuntimeNamespace")?"named":null);
  mapping.set(loader,resolver);
  var plugin=new FabricMixinPlugin();Field pkg=FabricMixinPlugin.class.getDeclaredField("pkg");pkg.setAccessible(true);pkg.set(plugin,"fabric.loading.mixin");
  Method probe=FabricMixinPlugin.class.getDeclaredMethod("prepareGame");probe.setAccessible(true);probe.invoke(plugin);
  if(!FabricHooks.backend.equals("sdl")||!FabricHooks.windowHandleMethod.equals("handle")||!FabricHooks.reloadProgressMethod.equals("getActualProgress")||plugin.getMixins().size()<4)throw new AssertionError("Modern production ABI not selected");
  if(FabricHooks.prepared)throw new AssertionError("Headless ABI check enabled runtime UI");
  System.out.println("PASS_VANILLA_26_3_ABI production capability probe / SDL / reload / Gui handoff; no game classes initialized");
 }
}