package fabric.loading.fabric;
import java.lang.reflect.*;
public final class NativeHandle{
 public static long resolve(Object window)throws ReflectiveOperationException{
  long handle=((Number)window.getClass().getMethod(FabricHooks.windowHandleMethod).invoke(window)).longValue();
  if(handle==0)throw new IllegalArgumentException("Null game handle");
  ClassLoader loader=window.getClass().getClassLoader();
  // Backend selection follows the actual SDL capability, never a numeric handle guess.
  if(FabricHooks.backend.equals("sdl")){
   Class<?> sdl=Class.forName("org.lwjgl.sdl.SDLVideo",false,loader),props=Class.forName("org.lwjgl.sdl.SDLProperties",false,loader);
   int properties=((Number)sdl.getMethod("SDL_GetWindowProperties",long.class).invoke(null,handle)).intValue();
   if(properties==0)throw new IllegalStateException("SDL window properties unavailable");
   return ((Number)props.getMethod("SDL_GetPointerProperty",int.class,CharSequence.class,long.class).invoke(null,properties,"SDL.window.win32.hwnd",0L)).longValue();
  }else if(FabricHooks.backend.equals("glfw")){
   Class<?> glfw=Class.forName("org.lwjgl.glfw.GLFWNativeWin32",false,loader);
   return ((Number)glfw.getMethod("glfwGetWin32Window",long.class).invoke(null,handle)).longValue();
  }
  throw new IllegalStateException("Unselected window backend");
 }
 private NativeHandle(){}
}
