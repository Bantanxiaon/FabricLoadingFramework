package fabric.loading.fabric;
import java.awt.image.*;
public final class YuvFrame {
 public static void convert(byte[] yuv,BufferedImage target){
  int w=target.getWidth(),h=target.getHeight(),size=Math.multiplyExact(w,h);
  if((w&1)!=0||(h&1)!=0||yuv.length!=size*3/2||target.getType()!=BufferedImage.TYPE_INT_RGB)throw new IllegalArgumentException("YUV420 frame size/type");
  int[] rgb=((DataBufferInt)target.getRaster().getDataBuffer()).getData();int chroma=size/4;
  for(int y=0;y<h;y++)for(int x=0;x<w;x++){
   int p=y*w+x,c=(y/2)*(w/2)+x/2,yy=298*((yuv[p]&255)-16),u=(yuv[size+c]&255)-128,v=(yuv[size+chroma+c]&255)-128;
   int r=clip((yy+459*v+128)>>8),g=clip((yy-55*u-136*v+128)>>8),b=clip((yy+541*u+128)>>8);
   rgb[p]=(r<<16)|(g<<8)|b;
  }
 }
 private static int clip(int v){return Math.max(0,Math.min(255,v));}
 private YuvFrame(){}
}
