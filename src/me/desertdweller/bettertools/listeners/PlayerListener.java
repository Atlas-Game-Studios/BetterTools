package me.desertdweller.bettertools.listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.nbtapi.NBTItem;
import me.desertdweller.bettertools.BetterTools;
import me.desertdweller.bettertools.math.BTBMeta;
import me.desertdweller.bettertools.math.BlockUtils;
import me.desertdweller.bettertools.math.StringUtils;
import me.desertdweller.bettertools.math.Noise;
import me.desertdweller.bettertools.undo.Alteration;
import me.desertdweller.bettertools.undo.ChangeTracker;
import net.md_5.bungee.api.ChatColor;

public class PlayerListener implements Listener{
	private static BetterTools plugin = BetterTools.getPlugin(BetterTools.class);

	@EventHandler
	public static void onPlayerInteract(PlayerInteractEvent e) {
		if(e.getItem() == null || e.getItem().getType() == Material.AIR)
			return;
		NBTItem nbti = new NBTItem(e.getItem());
		if(nbti.hasKey("Plugin") && nbti.getString("Plugin").equals("BetterTools") && nbti.getString("Item").equals("Paint Tool")) {
			e.setCancelled(true);
			if(!e.getPlayer().hasPermission("bt.use")) {
				e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to use that tool.");
				return;
			}
			usePaintBrush(e, nbti);
		}else if(nbti.hasKey("Plugin") && nbti.getString("Plugin").equals("BetterTools") && nbti.getString("Item").equals("Snow Tool")) {
			e.setCancelled(true);
			if(!e.getPlayer().hasPermission("bt.use")) {
				e.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to use that tool.");
				return; 
			}
			useSnowBrush(e, nbti);
		}
	}
	
	private static void useSnowBrush(PlayerInteractEvent e, NBTItem nbti) {
		if(nbti.getBoolean("Smooth") == false) {
			
		}else {
	        Alteration change = new Alteration();
			Set<Material> medium = new HashSet<Material>();
			medium.add(Material.AIR);
			Block centerBlock = e.getPlayer().getTargetBlock(medium, 200);
			List<Block> blocks;
			blocks = BlockUtils.getNearbyBlocksMasked(centerBlock.getLocation(), nbti.getInteger("Radius"),StringUtils.stringToHashMap("snow,snow_block", false), new Noise(), true);
			
			for(Block block : blocks) {
				change.addBlock(block);
				BlockUtils.smoothSnowBlock(block);
			}
			
			ChangeTracker tracker = ChangeTracker.getChangesForPlayer(e.getPlayer().getUniqueId());
	        if(tracker == null)
	        	tracker = new ChangeTracker(e.getPlayer().getUniqueId());
			
			if(change.getBlockList().keySet().size() > 0)
				tracker.addChange(change);
		}
	}
	
	private static void usePaintBrush(PlayerInteractEvent e, NBTItem nbti) {
		Block centerBlock = e.getPlayer().getTargetBlock(dataToMaterialSet(StringUtils.stringToHashMap(nbti.getString("Through"), false).keySet()), 200);
		List<Block> blocks;
		Noise noise = new Noise(nbti.getString("Noise"));
		if(nbti.hasKey("Mask") && !nbti.getString("Mask").equals("empty")) {
			blocks = BlockUtils.getNearbyBlocksMasked(centerBlock.getLocation(), nbti.getInteger("Radius"),StringUtils.stringToHashMap(StringUtils.replaceSpecialStrings(nbti.getString("Mask"), nbti.getString("Blocks")), false), noise, false);
		}else {
			blocks = BlockUtils.getNearbyBlocks(centerBlock.getLocation(), nbti.getInteger("Radius"), noise);
		}
		if(nbti.hasKey("Touching") && !nbti.getString("Touching").equals("")) {
			blocks = BlockUtils.getBlocksTouching(blocks, StringUtils.stringToHashMap(nbti.getString("Touching"), false));
		}

		Map<BlockData, BTBMeta> matList = StringUtils.stringToHashMap(nbti.getString("Blocks"), true);
		
		setBlocksInArea(blocks, getBlockList(matList), e.getPlayer(), matList, nbti.getBoolean("Updates"));
	}
	
	private static List<BlockData> getBlockList(Map<BlockData, BTBMeta> matList){
		List<BlockData> blockList = new ArrayList<BlockData>();
		for(BlockData block : matList.keySet()) {
			for(int i = 0; i < matList.get(block).amount; i++)
				blockList.add(block);
		}
		return blockList;
	}
	
	private static void setBlocksInArea(List<Block> area, List<BlockData> blockList, Player p, Map<BlockData, BTBMeta> matList, boolean blockUpdates) {
        Alteration change = new Alteration();
		for(int i = 0; i < area.size(); i++) {
			int id = (int) (Math.random()*blockList.size());
			BlockData targetData = (BlockData)  blockList.get(id);
			if(!area.get(i).getBlockData().equals(targetData)) {
				change.addBlock(area.get(i));
				plugin.getCoreProtect().logRemoval(p.getName(), area.get(i).getLocation(), area.get(i).getType(), area.get(i).getBlockData());
				if(!setBlockData(area.get(i), targetData.clone(), blockUpdates, matList.get(targetData).specified)) {
					p.sendMessage(ChatColor.RED + "The block type " + area.get(i).getBlockData().getClass().getSimpleName() + " was not able to correctly transfer data. This is an error. Please report it with this message and it will be fixed ASAP.");
				}
				plugin.getCoreProtect().logPlacement(p.getName(), area.get(i).getLocation(), area.get(i).getType(), area.get(i).getBlockData());
			}
		}
		
		ChangeTracker tracker = ChangeTracker.getChangesForPlayer(p.getUniqueId());
        if(tracker == null)
        	tracker = new ChangeTracker(p.getUniqueId());
		
		if(change.getBlockList().keySet().size() > 0)
			tracker.addChange(change);
	}
	
	private static boolean setBlockData(Block targetBlock, BlockData targetData, boolean updates, boolean customProps){
		//If the blocks are of the same type, (ie both walls), then transfer data from the previous block to the other.
		if(!customProps && (targetBlock.getBlockData().getClass().equals(targetData.getClass())) || checkSimilarClasses(targetBlock, targetData)) {
			targetData = BlockUtils.applyProperties(targetData, targetBlock.getBlockData());
		}
		//This would be null if there was an error in trying to transfer data from one block to another.
		if(targetData == null)
			return false;
		targetBlock.setBlockData(targetData, updates);
		return true;
	}
	
	private static boolean checkSimilarClasses(Block targetBlock, BlockData targetData) {
		String targetBlockClassName = targetBlock.getBlockData().getClass().getSimpleName();
		String targetPropertiesClassName = targetData.getClass().getSimpleName();
		if(targetBlockClassName.contains("Stair") && targetPropertiesClassName.contains("Stair"))
			return true;
		if(targetBlockClassName.contains("Slab") && targetPropertiesClassName.contains("Slab"))
			return true;
		if(targetBlockClassName.contains("Button") && targetPropertiesClassName.contains("Button"))
			return true;
		if(targetBlockClassName.contains("Pane") && targetPropertiesClassName.contains("Pane"))
			return true;
		if((targetBlockClassName.equals("CraftCrops") || targetBlockClassName.equals("CraftBeetroots") || targetBlockClassName.equals("CraftPotatoes") || targetBlockClassName.equals("CraftCarrots")) 
				&& (targetPropertiesClassName.equals("CraftCrops") || targetPropertiesClassName.equals("CraftBeetroots") || targetPropertiesClassName.equals("CraftPotatoes") || targetPropertiesClassName.equals("CraftCarrots")))
			return true;
		return false;
	}
	
	@EventHandler
	public static void onPlayerItemHoldEvent(PlayerItemHeldEvent e){
		ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
		if(item == null || item.getType() == Material.AIR) {
			if(e.getPlayer().getInventory().getItemInOffHand().getType() == Material.AIR)
				return;
			NBTItem offhand = new NBTItem(e.getPlayer().getInventory().getItemInOffHand());
			if(offhand.getItem().getType() == Material.FILLED_MAP && offhand.hasKey("Plugin") && offhand.getString("Plugin").equals("BetterTools")) {
				e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			}
			return;
		}
		NBTItem nbti = new NBTItem(item);
		if(!nbti.hasKey("Plugin") || !nbti.getString("Plugin").equals("BetterTools") || new Noise(nbti.getString("Noise")).method.equals("none")) {
			if(e.getPlayer().getInventory().getItemInOffHand().getType() == Material.AIR)
				return;
			NBTItem offhand = new NBTItem(e.getPlayer().getInventory().getItemInOffHand());
			if(offhand.getItem().getType() == Material.FILLED_MAP && offhand.hasKey("Plugin") && offhand.getString("Plugin").equals("BetterTools")) {
				e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR));
			}
			return;
		}
		if(item.getType() == Material.FILLED_MAP) {
			e.getPlayer().getInventory().setItem(e.getNewSlot(), null);
		}else if(!new Noise(nbti.getString("Noise")).method.equals("none")) {
			BlockUtils.givePlayerNoiseMap(e.getPlayer());
		}
	}

	@EventHandler
	public static void onPlayerDropItemEvent(PlayerDropItemEvent e) {
		if(e.getPlayer().getInventory().getItemInOffHand().getType() == Material.AIR)
			return;
		NBTItem offhand = new NBTItem(e.getPlayer().getInventory().getItemInOffHand());
		if(offhand.getItem().getType() == Material.FILLED_MAP && offhand.hasKey("Plugin") && offhand.getString("Plugin").equals("BetterTools")) {
			if(e.getPlayer().getInventory().getItemInMainHand().equals(null))
				e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.AIR));
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public static void onInventoryClickEvent(InventoryClickEvent e) {
		if(e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
			return;
		NBTItem nbti = new NBTItem(e.getCurrentItem());
		if(nbti.hasKey("Plugin") && nbti.getString("Plugin").equals("BetterTools") && e.getCurrentItem().getType() == Material.FILLED_MAP) {
			e.setCancelled(true);
			e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
			e.setCursor(new ItemStack(Material.AIR));
		}
	}
	
	@EventHandler
	public static void onPlayerSwapHandsEvent(PlayerSwapHandItemsEvent e) {
		if(e.getOffHandItem().getType() == Material.AIR)
			return;
		NBTItem nbti = new NBTItem(e.getOffHandItem());
		if(nbti.hasKey("Plugin") && nbti.getString("Plugin").equals("BetterTools") && e.getOffHandItem().getType() == Material.FILLED_MAP) {
			e.setOffHandItem(new ItemStack(Material.AIR));
		}
	}

	@EventHandler
	public static void onFarmlandTrampleEvent(PlayerInteractEvent e) {
		if(e.getAction().equals(Action.PHYSICAL) && e.hasBlock() && e.getPlayer().getGameMode().equals(GameMode.CREATIVE) && e.getClickedBlock().getType() == Material.FARMLAND) {
			e.setCancelled(true);
		}
	}

	private static Set<Material> dataToMaterialSet(Set<BlockData> list){
		Set<Material> output = new HashSet<Material>();
		for(BlockData data : list) {
			output.add(data.getMaterial());
		}
		return output;
	}
	
//	@SuppressWarnings("unused")
//	private static void setBlockInNativeWorld(Block block, int blockId, boolean applyPhysics) {
//	    net.minecraft.server.v1_13_R2.World nmsWorld = ((CraftWorld) block.getWorld()).getHandle();
//	    BlockPosition bp = new BlockPosition(block.getX(), block.getY(), block.getZ());
//	    @SuppressWarnings("deprecation")
//		IBlockData ibd = net.minecraft.server.v1_13_R2.Block.getByCombinedId(blockId + (block.getData() << 12));
//	    nmsWorld.setTypeAndData(bp, ibd, applyPhysics ? 3 : 2);
//	}
}