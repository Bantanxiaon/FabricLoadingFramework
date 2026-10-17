package fixtures;
public class Initializer {
 public static Runnable action=()->{};
 public static int finalized;
 public void onInitialize(){try{action.run();}finally{finalized++;}}
}
