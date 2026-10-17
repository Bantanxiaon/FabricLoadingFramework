import fabric.loading.core.*;
import fabric.loading.fabric.YuvFrame;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

/** Exercises the real FFmpeg pipe without native windows, audio devices or Minecraft. */
public final class DecoderSmoke {
 public static void main(String[] args) throws Exception {
  Path dir=Path.of(args[0]);Settings settings=new Settings();
  settings.width=320;settings.height=180;settings.audio=false;
  MediaSession session=new MediaSession(dir,settings);
  BufferedImage image=new BufferedImage(320,180,BufferedImage.TYPE_INT_RGB);
  Process decoder=null;int consumed=0;long last=-1;
  try {
   session.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
   while(consumed<40&&System.nanoTime()<deadline){
    MediaSession.Frame frame=session.latest();
    if(frame==null){Thread.sleep(5);continue;}
    try {
     if(frame.index()<=last)throw new AssertionError("Frame order");
     YuvFrame.convert(frame.rgba(),image);last=frame.index();consumed++;
    } finally {session.release(frame);}
   }
   Field process=MediaSession.class.getDeclaredField("videoProcess");process.setAccessible(true);decoder=(Process)process.get(session);
   if(consumed<40||session.videoFailed||session.frames.size()>settings.queueFrames)throw new AssertionError("Decoder or queue failure");
   // The clip is one second: consuming forty frames proves streaming loops across EOF.
   if(last<39)throw new AssertionError("Loop did not advance");
  } finally {session.close();session.finishLogging();}
  if(decoder==null||!decoder.waitFor(3,TimeUnit.SECONDS))throw new AssertionError("Decoder survived close");
  // A session closed before it starts must never create a decoder process.
  MediaSession stopped=new MediaSession(dir,settings);stopped.close();stopped.start();Thread.sleep(150);stopped.finishLogging();
  Field process=MediaSession.class.getDeclaredField("videoProcess");process.setAccessible(true);
  if(process.get(stopped)!=null)throw new AssertionError("Late decoder created after close");
  System.out.println("PASS_REAL_DECODER H264 loop / bounded queue / YUV frames / process shutdown");
 }
}
