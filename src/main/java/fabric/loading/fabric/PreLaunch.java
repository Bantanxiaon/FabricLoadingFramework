package fabric.loading.fabric;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
public final class PreLaunch implements PreLaunchEntrypoint {
 public void onPreLaunch(){try{FabricController.ensureStarted();}catch(Throwable e){FabricHooks.warn(e);FabricController.close();}}
}
