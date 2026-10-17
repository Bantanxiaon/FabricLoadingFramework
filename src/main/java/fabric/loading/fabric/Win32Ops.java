package fabric.loading.fabric;
import com.sun.jna.*;import com.sun.jna.platform.win32.*;import com.sun.jna.platform.win32.WinDef.*;import com.sun.jna.ptr.IntByReference;
public final class Win32Ops implements NativeAttachment.Ops{
 private final User32 user=User32.INSTANCE;private int width=-1,height=-1;
 private static HWND h(long value){return new HWND(Pointer.createConstant(value));}
 public boolean valid(long value){return user.IsWindow(h(value));}
 public long pid(long value){IntByReference out=new IntByReference();user.GetWindowThreadProcessId(h(value),out);return Integer.toUnsignedLong(out.getValue());}
 public long parent(long value){HWND p=user.GetParent(h(value));return p==null?0:Pointer.nativeValue(p.getPointer());}
 public int style(long value,int index){return user.GetWindowLong(h(value),index);}
 public void style(long value,int index,int flags){user.SetWindowLong(h(value),index,flags);}
 public void parent(long child,long parent){user.SetParent(h(child),parent==0?null:h(parent));}
 public void fit(long child,long parent){RECT r=new RECT();if(!user.GetClientRect(h(parent),r))throw new IllegalStateException("Cannot query game window");int w=Math.max(1,r.right-r.left),ht=Math.max(1,r.bottom-r.top);if(w==width&&ht==height)return;if(!user.SetWindowPos(h(child),null,0,0,w,ht,0x0034))throw new IllegalStateException("Cannot fit loading surface");width=w;height=ht;}
 public void alpha(long child,int value){if(!user.SetLayeredWindowAttributes(h(child),0,(byte)value,2))throw new IllegalStateException("Cannot fade loading surface");}
}
