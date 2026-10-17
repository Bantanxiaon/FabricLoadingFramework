package fabric.loading.fabric;
import org.objectweb.asm.*;import org.objectweb.asm.tree.*;
import java.util.*;
/** Only called by this mod's Mixin postApply. No on-disk game or loader patching. */
public final class HookWeaver implements Opcodes {
 private static final String HOOK="fabric/loading/fabric/FabricHooks";
 private static MethodInsnNode call(String name,String desc){return new MethodInsnNode(INVOKESTATIC,HOOK,name,desc,false);}
 public static void callback(MethodNode method,String key){
  if((method.access&(ACC_ABSTRACT|ACC_NATIVE))!=0||!method.desc.equals("()V"))throw new IllegalArgumentException("Unsupported initializer shape");
  InsnList head=new InsnList();head.add(new LdcInsnNode(key));head.add(call("enter","(Ljava/lang/String;)V"));
  LabelNode start=new LabelNode();head.add(start);method.instructions.insert(head);
  // Capture the original return instructions before inserting the handler.
  for(AbstractInsnNode n:method.instructions.toArray())if(n.getOpcode()==RETURN){InsnList end=new InsnList();end.add(new LdcInsnNode(key));end.add(call("exit","(Ljava/lang/String;)V"));method.instructions.insertBefore(n,end);}
  LabelNode end=new LabelNode(),handler=new LabelNode();method.instructions.add(end);method.instructions.add(handler);
  int local=method.maxLocals++;method.instructions.add(new VarInsnNode(ASTORE,local));method.instructions.add(new LdcInsnNode(key));method.instructions.add(call("failed","(Ljava/lang/String;)V"));method.instructions.add(new VarInsnNode(ALOAD,local));method.instructions.add(new InsnNode(ATHROW));
  method.tryCatchBlocks.add(new TryCatchBlockNode(start,end,handler,"java/lang/Throwable"));method.maxStack+=2;
 }
 public static void onReturn(MethodNode method,String hook,boolean self){
  for(AbstractInsnNode n:method.instructions.toArray())if(n.getOpcode()==RETURN){
   InsnList list=new InsnList();if(self)list.add(new VarInsnNode(ALOAD,0));list.add(call(hook,self?"(Ljava/lang/Object;)V":"()V"));method.instructions.insertBefore(n,list);
  }method.maxStack+=2;
 }
 public static void objectArgAtReturn(MethodNode method,int arg,String hook){
  Type[] types=Type.getArgumentTypes(method.desc);int slot=(method.access&ACC_STATIC)==0?1:0;
  for(int i=0;i<arg;i++)slot+=types[i].getSize();
  for(AbstractInsnNode n:method.instructions.toArray())if(n.getOpcode()==RETURN){InsnList list=new InsnList();list.add(new VarInsnNode(ALOAD,slot));list.add(call(hook,"(Ljava/lang/Object;)V"));method.instructions.insertBefore(n,list);}method.maxStack+=2;
 }
 public static byte[] marker(String name,String target){
  ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC|ACC_ABSTRACT|ACC_SUPER,name.replace('.','/'),null,"java/lang/Object",null);
  AnnotationVisitor a=writer.visitAnnotation("Lorg/spongepowered/asm/mixin/Mixin;",false),targets=a.visitArray("targets");targets.visit(null,target);targets.visitEnd();a.visit("remap",false);a.visitEnd();writer.visitEnd();return writer.toByteArray();
 }
 private HookWeaver(){}
}
