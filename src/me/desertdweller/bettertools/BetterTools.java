package me.desertdweller.bettertools;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.ags.simpleblocks.SimpleBlocks;

import me.desertdweller.bettertools.listeners.PlayerJoinListener;
import me.desertdweller.bettertools.listeners.PlayerListener;
import me.desertdweller.bettertools.math.BlockUtils;
import me.desertdweller.bettertools.math.PerlinNoiseGenerator;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;

public class BetterTools extends JavaPlugin{
	public PerlinNoiseGenerator noiseGen;

	@Override
	public void onEnable() {
		getCommand("bt").setExecutor(new Commands());
		getCommand("bt").setTabCompleter(new CommandCompleter());
		getServer().getPluginManager().registerEvents(new PlayerListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
		noiseGen = new PerlinNoiseGenerator();
		BlockUtils.initMaterialIds();
		
	}
	
	public CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (plugin == null || !(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (CoreProtect.isEnabled() == false) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 6) {
            return null;
        }

        return CoreProtect;
	}
	
	public static SimpleBlocks getSimpleBlocks() {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("SimpleBlocks");
 
        if (plugin instanceof SimpleBlocks) {
            return (SimpleBlocks) plugin;
        }
        return null;
    }
}
