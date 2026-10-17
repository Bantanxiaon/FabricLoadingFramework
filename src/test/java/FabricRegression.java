import fabric.loading.fabric.*;import fabric.loading.core.*;
import java.nio.file.*;import java.awt.image.*;import java.lang.reflect.*;import java.util.*;import java.util.jar.*;import java.io.*;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
public final class FabricRegression {
 static int checks;
 static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static void rejected(Runnable action,String name){try{action.run();throw new AssertionError("accepted "+name);}catch(IllegalArgumentException expected){check(true,name);}}
 static final class NativeOps implements NativeAttachment.Ops{
  Map<Long,Integer> styles=new HashMap<>();Map<Long,Long> parents=new HashMap<>(),pids=new HashMap<>();int alpha,alphaCalls;boolean reject;
  NativeOps(){pids.put(7L,123L);pids.put(42L,123L);pids.put(99L,999L);styles.put(42L,0x400);}
  public boolean valid(long h){return pids.containsKey(h);}public long pid(long h){return pids.getOrDefault(h,0L);}
  public long parent(long h){return parents.getOrDefault(h,0L);}public int style(long h,int i){return styles.getOrDefault(h*100+i,styles.getOrDefault(h,0));}
  public void style(long h,int i,int v){styles.put(h*100+i,v);}public void parent(long c,long p){if(!reject||p==0)parents.put(c,p);}
  public void fit(long c,long p){}public void alpha(long c,int a){alpha=a;alphaCalls++;}
 }
 static byte[] bytes(Path jar,String entry)throws Exception{try(JarFile z=new JarFile(jar.toFile())){return z.getInputStream(z.getJarEntry(entry)).readAllBytes();}}
 static ClassNode node(byte[] bytes){ClassNode n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
 public static void main(String[] args)throws Exception{
  Path work=Path.of(args[0]),fixture=Path.of(args[1]),project=Path.of(args[2]);Files.createDirectories(work);
  for(String game:new String[]{"1.20.1","1.20.5","1.20.6","1.21","1.21.11","26.1","26.1.2","26.2","26.3"})
   check(FabricAccess.versions(game,"0.19.5"),"route "+game);
  for(String game:new String[]{"1.19.4","1.20","1.20.7","1.21.12","26.4","26.3.1","26.3-pre1","junk"})
   check(!FabricAccess.versions(game,"0.19.5"),"reject unknown game "+game);
  check(!FabricAccess.versions("1.20.1","0.20.0"),"unknown loader keeps stock path");
  check(!FabricAccess.versions("1.20.1","0.19.5-beta"),"preview loader not claimed");
  check(!FabricAccess.versions("1.20.1","0.14.20"),"minimum loader matches metadata");
  for(String loader:new String[]{"0.14.21","0.15.11","0.16.14","0.17.3","0.18.4","0.19.5"}){
   Path sdk=project.resolve("dependencies/fabric-loader-"+loader+".jar");
   ClassNode api=node(bytes(sdk,"net/fabricmc/loader/api/FabricLoader.class"));
   check(FabricAccess.has(api,"getLaunchArguments","(Z)[Ljava/lang/String;"),"launch API "+loader);
   ClassNode launch=node(bytes(sdk,"net/fabricmc/loader/impl/launch/FabricLauncher.class"));
   check(FabricAccess.has(launch,"addToClassPath","(Ljava/nio/file/Path;[Ljava/lang/String;)V")&&FabricAccess.has(launch,"getClassByteArray","(Ljava/lang/String;Z)[B"),"hook cache API "+loader);
  }
  NativeOps nativeOps=new NativeOps();NativeAttachment attachment=new NativeAttachment(nativeOps,7,123);
  attachment.opacity(.5f);check(nativeOps.alphaCalls==0,"standalone window never fades through desktop");
  rejected(()->attachment.attach(99),"reject foreign process parent");
  rejected(()->attachment.attach(0),"reject null parent");
  attachment.attach(42);check(attachment.attached()&&nativeOps.parent(7)==42,"attach own child");
  attachment.opacity(.5f);check(nativeOps.alpha==128,"embedded fade alpha");
  nativeOps.style(42,-16,nativeOps.style(42,-16)|0x800);
  attachment.close();check((nativeOps.style(42,-16)&0x02000000)==0&&(nativeOps.style(42,-16)&0x800)!=0,"restore only owned parent style bit");
  NativeOps failed=new NativeOps();failed.reject=true;
  NativeAttachment broken=new NativeAttachment(failed,7,123);
  try{broken.attach(42);throw new AssertionError("attachment accepted");}catch(IllegalStateException expected){}
  check(!broken.attached()&&failed.parent(7)==0&&(failed.style(42,-16)&0x02000000)==0,"rollback failed native attachment");
  NativeOps reused=new NativeOps();NativeAttachment originalParent=new NativeAttachment(reused,7,123);originalParent.attach(42);reused.pids.put(42L,999L);
  check(!originalParent.parentAlive(),"reject reused foreign parent handle");
  CompletionGate gate=new CompletionGate();gate.overlay(false);
  check(gate.frame(false,false)==CompletionGate.Decision.WAIT,"no time based completion");
  check(gate.frame(true,true)==CompletionGate.Decision.FADE,"success and game frame handoff");
  check(gate.frame(true,true)==CompletionGate.Decision.WAIT,"fade starts only once");
  CompletionGate failedGate=new CompletionGate();check(failedGate.frame(true,false)==CompletionGate.Decision.ABORT,"failed reload aborts video");
  CompletionGate replaced=new CompletionGate();replaced.overlay(false);replaced.overlay(true);
  check(replaced.frame(true,true)==CompletionGate.Decision.WAIT,"replacement overlay cancels early completion");
  BufferedImage image=new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB);
  byte[] yuv={(byte)16,(byte)16,(byte)16,(byte)16,(byte)128,(byte)128};
  YuvFrame.convert(yuv,image);check((image.getRGB(0,0)&0xffffff)==0,"BT709 limited black");
  Arrays.fill(yuv,0,4,(byte)235);YuvFrame.convert(yuv,image);check((image.getRGB(0,0)&0xffffff)==0xffffff,"BT709 limited white");
  Arrays.fill(yuv,0,4,(byte)63);yuv[4]=(byte)102;yuv[5]=(byte)240;YuvFrame.convert(yuv,image);int red=image.getRGB(0,0);check(((red>>16)&255)>240&&((red>>8)&255)<4&&(red&255)<4,"BT709 red channels");
  rejected(()->YuvFrame.convert(new byte[1],image),"reject malformed decoded frame");
  TaskRegistry.plan(new TaskRegistry.Plan("fixture","fixture_mod","客户端初始化"));TaskRegistry.name("fixture_mod","Fixture Mod");
  ClassNode original=node(Files.readAllBytes(fixture));MethodNode method=original.methods.stream().filter(m->m.name.equals("onInitialize")).findFirst().orElseThrow();
  HookWeaver.callback(method,"fixture");ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);original.accept(writer);
  byte[] instrumented=writer.toByteArray();
  Class<?> type=new ClassLoader(FabricRegression.class.getClassLoader()){Class<?> make(){return defineClass("fixtures.Initializer",instrumented,0,instrumented.length);}}.make();
  Object object=type.getConstructor().newInstance();
  type.getField("action").set(null,(Runnable)()->check(TaskRegistry.current().size()==1&&TaskRegistry.current().get(0).id().equals("fixture_mod"),"exact active mod inside callback"));
  type.getMethod("onInitialize").invoke(object);
  check(TaskRegistry.current().isEmpty()&&TaskRegistry.completed()==1,"normal callback clears current task");
  RuntimeException failure=new RuntimeException("fixture exception");
  type.getField("action").set(null,(Runnable)()->{throw failure;});
  try{type.getMethod("onInitialize").invoke(object);throw new AssertionError("exception swallowed");}catch(InvocationTargetException e){check(e.getCause()==failure,"original mod exception identity preserved");}
  check(TaskRegistry.current().isEmpty(),"failed callback clears current task");
  check(type.getField("finalized").getInt(null)==2,"original finally runs on success and failure");
  check(TaskRegistry.completed()==1,"repeated callback not double counted");
  LoadingProgress progress=new LoadingProgress();progress.resource(.7f);double first=progress.snapshot().overall();progress.resource(.2f);
  check(progress.snapshot().overall()>=first,"total progress does not move backwards");
  progress.resource(1);check(progress.snapshot().overall()<1,"resource completion alone not 100 percent");
  progress.complete();check(progress.snapshot().overall()==1&&progress.snapshot().stage().equals("初始化完成"),"100 percent requires explicit handoff completion");
  Path pack=work.resolve("Folder Title");Files.createDirectories(pack);
  check(PackName.resolve(pack,new String[0]).equals("Folder Title"),"folder title fallback");
  check(PackName.resolve(pack,new String[]{"--version","Launcher Name"}).equals("Launcher Name"),"launcher title priority");
  Files.writeString(pack.resolve("manifest.json"),"{\"name\":\"Metadata Name\"}");
  check(PackName.resolve(pack,new String[]{"--version","Launcher Name"}).equals("Metadata Name"),"metadata title priority");
  Settings settings=Settings.load(pack.resolve("fabric_loading"));
  check(Settings.directory(pack).equals(pack.resolve("fabric_loading")),"independent Fabric config directory");
  for(String unsafe:new String[]{"../outside.mp4","D:/outside.mp4"})rejected(()->settings.resource(pack,unsafe),"reject resource escape "+unsafe);
  check(settings.width==640&&settings.height==360&&settings.fps==30&&settings.queueFrames<=3,"bounded low-resolution defaults");
  byte[] marker=HookWeaver.marker("fabric.loading.mixin.Test","fixtures.Initializer");ClassNode markerNode=node(marker);
  check(markerNode.version==61&&markerNode.methods.isEmpty()&&markerNode.invisibleAnnotations.stream().anyMatch(a->a.desc.endsWith("/Mixin;")),"generated marker is a Java17 Mixin class");
  Path cacheDir=work.resolve("cache");Files.createDirectories(cacheDir);
  Map<String,byte[]> ownMarker=Map.of("fabric.loading.mixin.Hook0",HookWeaver.marker("fabric.loading.mixin.Hook0","fixtures.Initializer"));
  Path generated=MixinCache.store(cacheDir,ownMarker);check(MixinCache.store(cacheDir,ownMarker).equals(generated),"generated cache is immutable and reused");
  Files.writeString(generated,"corrupt");try{MixinCache.store(cacheDir,ownMarker);throw new AssertionError("corrupt cache accepted");}catch(IOException expected){check(true,"corrupt cache rejected before classpath registration");}
  System.out.println("FABRIC_HEADLESS_CHECKS_OK "+checks);
 }
}