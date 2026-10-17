package fabric.loading.fabric;
import fabric.loading.core.*;
import com.sun.jna.Native;import com.sun.jna.Pointer;
import javax.swing.*;import java.awt.*;import java.awt.event.*;import java.awt.image.*;import java.util.concurrent.*;
/** One CPU video presenter. It never acquires or changes the game's OpenGL/SDL context. */
public final class FabricSurface implements AutoCloseable{
 private final MediaSession media;private final Object pixels=new Object();
 private final JFrame frame=new JFrame();private final JPanel panel;
 private final BufferedImage ui=new BufferedImage(1280,720,BufferedImage.TYPE_INT_ARGB);
 private BufferedImage front,back;private final Theme theme;
 private ScheduledExecutorService executor;
 private volatile NativeAttachment attachment;private volatile boolean closed;
 private boolean hasVideo;private String lastUi="";private long lastUiCheck;private final Thread startupThread;
 public FabricSurface(MediaSession media){
  if(!SwingUtilities.isEventDispatchThread())throw new IllegalStateException("Create surface on EDT");
  this.media=media;startupThread=FabricController.startupThread;theme=new Theme(media);
  front=new BufferedImage(media.settings.width,media.settings.height,BufferedImage.TYPE_INT_RGB);back=new BufferedImage(front.getWidth(),front.getHeight(),front.getType());
  panel=new JPanel(){protected void paintComponent(Graphics raw){
   super.paintComponent(raw);Graphics2D g=(Graphics2D)raw.create();
   try{synchronized(pixels){
    g.setColor(theme.background());g.fillRect(0,0,getWidth(),getHeight());
    if(hasVideo){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(front,0,0,getWidth(),getHeight(),null);}
    g.drawImage(FabricSurface.this.ui,0,0,getWidth(),getHeight(),null);
   }}finally{g.dispose();}
  }};
  panel.setOpaque(true);panel.setBackground(theme.background());frame.setUndecorated(true);frame.setBackground(theme.background());frame.setTitle(media.settings.title);
  frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){FabricController.close();}});
  frame.setContentPane(panel);frame.setSize(960,540);frame.setLocationRelativeTo(null);
  if(media.settings.maximized)frame.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
  try{
  frame.setVisible(true);
  long child=Pointer.nativeValue(Native.getComponentPointer(frame));
  attachment=new NativeAttachment(new Win32Ops(),child,ProcessHandle.current().pid());
  executor=Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"FabricLoadingFramework-presenter");t.setDaemon(true);return t;});
  executor.scheduleAtFixedRate(this::present,0,1_000_000_000L/media.settings.fps,TimeUnit.NANOSECONDS);
  }catch(RuntimeException|LinkageError error){if(executor!=null)executor.shutdownNow();frame.dispose();throw error;}
 }
 public void attach(long parent){NativeAttachment a=attachment;if(a!=null&&!closed)a.attach(parent);}
 private void present(){
  if(closed)return;
  try{
   if(!startupThread.isAlive()||!attachment.parentAlive()){FabricController.close();return;}
   MediaSession.Frame next=media.latest();
   if(next!=null){try{YuvFrame.convert(next.rgba(),back);synchronized(pixels){BufferedImage old=front;front=back;back=old;hasVideo=true;}}finally{media.release(next);}}
   long now=System.nanoTime();
   if(now-lastUiCheck>=100_000_000L){
    String content=media.progressState().toString()+media.tasks.current().toString();
    if(!content.equals(lastUi)){synchronized(pixels){Graphics2D g=ui.createGraphics();try{g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,1280,720);g.setComposite(AlphaComposite.SrcOver);theme.draw(g,media.progressState(),media.tasks.current());}finally{g.dispose();}}lastUi=content;}
    lastUiCheck=now;
   }
   attachment.fit();
   // Whole-window alpha is only allowed after embedding, so the desktop never fades through.
   if(FabricController.fading()){if(!attachment.attached()){FabricController.close();return;}float alpha=media.opacity();attachment.opacity(alpha);media.fade(alpha);if(alpha<=0){FabricController.close();return;}}
   panel.repaint();
  }catch(Throwable e){FabricHooks.warn(e);FabricController.close();}
 }
 public void close(){
  if(closed)return;closed=true;if(executor!=null)executor.shutdownNow();
  SwingUtilities.invokeLater(()->{try{frame.dispose();}finally{NativeAttachment a=attachment;if(a!=null)a.close();}});
 }
}
