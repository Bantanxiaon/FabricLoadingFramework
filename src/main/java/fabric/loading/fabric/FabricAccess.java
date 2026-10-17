package fabric.loading.fabric;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
import java.util.*;
public final class FabricAccess {
 public static String mappedClass(String intermediary,String modern){
  try{var map=FabricLoader.getInstance().getMappingResolver();if(map.getNamespaces().contains("intermediary"))return map.mapClassName("intermediary",intermediary);}catch(RuntimeException ignored){}
  return modern;
 }
 public static String method(String owner,String name,String desc,String modern){
  try{var map=FabricLoader.getInstance().getMappingResolver();if(map.getNamespaces().contains("intermediary"))return map.mapMethodName("intermediary",owner,name,desc);}catch(RuntimeException ignored){}
  return modern;
 }
 public static ClassNode node(String name){
  try{byte[] b=FabricLauncherBase.getLauncher().getClassByteArray(name,false);if(b==null)return null;ClassNode n=new ClassNode();new ClassReader(b).accept(n,ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);return n;}catch(Exception|LinkageError e){return null;}
 }
 public static boolean has(ClassNode n,String method,String desc){return n!=null&&n.methods.stream().anyMatch(m->m.name.equals(method)&&m.desc.equals(desc));}
 public static boolean versions(String minecraft,String loader){
  if(!loader.matches("0\\.(14|15|16|17|18|19)\\.\\d+"))return false;
  try{
   String[] lv=loader.split("\\.");if(lv[1].equals("14")&&Integer.parseInt(lv[2])<21)return false;
   String[] v=minecraft.split("\\.");for(String part:v)if(!part.matches("\\d+"))return false;
   int a=Integer.parseInt(v[0]),b=v.length>1?Integer.parseInt(v[1]):-1,c=v.length>2?Integer.parseInt(v[2]):0;
   return v.length<=3&&((a==1&&((b==20&&c>=1&&c<=6)||(b==21&&c<=11)))||(a==26&&b>=1&&b<=3&&(b==1?c<=2:c==0)));
  }catch(RuntimeException e){return false;}
 }
 private FabricAccess(){}
}
