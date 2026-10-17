package fabric.loading.fabric;
import java.nio.file.*;import java.io.*;import java.util.*;import java.util.jar.*;import java.security.*;
/** Immutable generated resources. Existing cache bytes must match this launch's own generated classes. */
public final class MixinCache {
 public static Path store(Path directory,Map<String,byte[]> classes)throws IOException{
  if(classes.size()>4104)throw new IOException("Too many generated markers");
  ByteArrayOutputStream buffer=new ByteArrayOutputStream();
  try(JarOutputStream jar=new JarOutputStream(buffer)){
   for(var e:new TreeMap<>(classes).entrySet()){
    if(!e.getKey().matches("fabric\\.loading\\.mixin\\.Hook\\d+")||e.getValue().length>16000)throw new IOException("Unexpected generated marker");
    JarEntry entry=new JarEntry(e.getKey().replace('.','/')+".class");entry.setTime(0);jar.putNextEntry(entry);jar.write(e.getValue());jar.closeEntry();
   }
  }
  byte[] bytes=buffer.toByteArray();if(bytes.length>8_000_000)throw new IOException("Generated cache too large");
  String hash;try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}
  Path file=directory.resolve("hooks-"+hash+".jar");
  try{Files.write(file,bytes,StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException race){}
  if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)!=bytes.length||!Arrays.equals(Files.readAllBytes(file),bytes))throw new IOException("Generated cache validation failed");
  return file;
 }
 private MixinCache(){}
}