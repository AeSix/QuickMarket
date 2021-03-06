package me.mrCookieSlime.QuickMarket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.mrCookieSlime.QuickMarket.shop.MarketLink;
import me.mrCookieSlime.QuickMarket.shop.MarketStand;
import me.mrCookieSlime.QuickMarket.shop.PlayerMarket;
import me.mrCookieSlime.QuickMarket.shop.PlayerShop;
import me.mrCookieSlime.QuickMarket.shop.ShopProtectionLevel;
import me.mrCookieSlime.QuickMarket.shop.PlayerShop.ShopType;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class MarketListener implements Listener {
	
	public MarketListener(QuickMarket plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	private final BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	
	Map<UUID, MarketLink> link = new HashMap<UUID, MarketLink>();
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent e) {
		if (link.containsKey(e.getPlayer().getUniqueId()) && e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			link.get(e.getPlayer().getUniqueId()).link(e.getClickedBlock());
			link.remove(e.getPlayer().getUniqueId());
		}
		else if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
			if (e.getClickedBlock().getType() == Material.WALL_SIGN) {
				PlayerShop shop = PlayerShop.signs.get(e.getClickedBlock());
				if (shop != null) {
					if (shop.isOwner(e.getPlayer())) shop.openEditor(e.getPlayer());
					else shop.handleTransaction(e.getPlayer(), 0);
				}
				else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
					MarketStand market = MarketStand.map.get(MarketStand.location(e.getClickedBlock().getLocation()));
					if (market != null && market.isOwner(e.getPlayer())) {
						try {
							market.openGUI(e.getPlayer());
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
			}
			else if (e.getClickedBlock().getType().equals(Material.CHEST) || e.getClickedBlock().getType().equals(Material.TRAPPED_CHEST)) {
				ShopProtectionLevel level = isChestProtected(e.getPlayer(), e.getClickedBlock());
				if (level.equals(ShopProtectionLevel.NO_ACCESS)) {
					e.setCancelled(true);
					e.getPlayer().sendMessage("�4You are not permitted to modify this Shop");
				}
			}
		}
	}
	
	private ShopProtectionLevel isChestProtected(Player p, Block b) {
		PlayerShop shop = PlayerShop.chests.get(b);
		if (shop != null) return (p == null ||!shop.isOwner(p) || shop.getOwner() == null) ? ShopProtectionLevel.NO_ACCESS: ShopProtectionLevel.ACCESS;
		else {
			for (BlockFace face: faces) {
				Block block = b.getRelative(face);
				PlayerShop adjacentShop = PlayerShop.chests.get(block);
				if (block.getType().equals(b.getType()) && adjacentShop != null) return (p == null ||!adjacentShop.isOwner(p) || adjacentShop.getOwner() == null) ? ShopProtectionLevel.NO_ACCESS: ShopProtectionLevel.ACCESS;
			}
		}
		return ShopProtectionLevel.NO_SHOP;
	}
	
	@EventHandler
	public void onPickup(PlayerPickupItemEvent e) {
		if (e.getItem().hasMetadata("quickmarket_item")) {
			e.setCancelled(true);
		}
		else if (e.getItem().getItemStack().hasItemMeta() && e.getItem().getItemStack().getItemMeta().hasDisplayName()) {
			if (e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith("�6�lQuickMarket Display Item �e")) {
				e.setCancelled(true);
				e.getItem().remove();
			}
		}
	}
	
	@EventHandler
	public void onPickup(InventoryPickupItemEvent e) {
		if (e.getItem().hasMetadata("quickmarket_item")) {
			e.setCancelled(true);
		}
		else if (e.getItem().getItemStack().hasItemMeta() && e.getItem().getItemStack().getItemMeta().hasDisplayName()) {
			if (e.getItem().getItemStack().getItemMeta().getDisplayName().startsWith("�6�lQuickMarket Display Item �e")) {
				e.setCancelled(true);
				e.getItem().remove();
			}
		}
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent e) {
		String chunk = e.getChunk().getWorld().getName() + "_" + e.getChunk().getX() + "_" + e.getChunk().getZ();
		if (PlayerShop.chunk.containsKey(chunk)) {
			List<PlayerShop> shops = PlayerShop.chunk.get(chunk);
			if (QuickMarket.getInstance().cfg.getBoolean("options.chunk-notifications")) System.out.println("[QuickMarket] Chunk X:" + e.getChunk().getX() + " Z:" + e.getChunk().getZ() + " has been unloaded, this lead to " + shops.size() + " Shop(s) being temporarily unloaded.");
			for (PlayerShop shop: shops) {
				if (shop.getDisplayItem() != null) shop.getDisplayItem().remove();
				shop.setLoaded(false);
			}
		}
	}
	
	@EventHandler
	public void onChunkLoad(ChunkLoadEvent e) {
		String chunk = e.getChunk().getWorld().getName() + "_" + e.getChunk().getX() + "_" + e.getChunk().getZ();
		if (PlayerShop.chunk.containsKey(chunk)) {
			List<PlayerShop> shops = PlayerShop.chunk.get(chunk);
			if (QuickMarket.getInstance().cfg.getBoolean("options.chunk-notifications")) System.out.println("[QuickMarket] Chunk X:" + e.getChunk().getX() + " Z:" + e.getChunk().getZ() + " has been loaded, this lead to " + shops.size() + " Shop(s) being loaded.");
			for (PlayerShop shop: shops) {
				shop.setLoaded(true);
				shop.update(true);
			}
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent e) {
		PlayerShop shop = PlayerShop.chests.get(e.getBlock());
		if (shop != null) {
			e.setCancelled(true);
			if (shop.isOwner(e.getPlayer())) {
				if (shop.isMarket()) e.getPlayer().sendMessage("�4Delete the Shop by destroying the Marketstand Sign");
				else e.getPlayer().sendMessage("�4Delete the Shop using the Shopmenu instead of breaking the Block");
			}
			else e.getPlayer().sendMessage("�4You are not permitted to modify this Shop");
		}
		else {
			shop = PlayerShop.signs.get(e.getBlock());
			if (shop != null) {
				e.setCancelled(true);
				if (shop.isOwner(e.getPlayer())) {
					if (shop.isMarket()) e.getPlayer().sendMessage("�4Delete the Shop by destroying the Marketstand Sign");
					else e.getPlayer().sendMessage("�4Delete the Shop using the Shopmenu instead of breaking the Block");
				}
				else e.getPlayer().sendMessage("�4You are not permitted to modify this Shop");
			}
			else {
				MarketStand market = MarketStand.map.get(MarketStand.location(e.getBlock().getLocation()));
				if (market != null && e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					market.delete();
				}
			}
		}
	}
	
	@EventHandler
	public void onSignEdit(SignChangeEvent e) {
		if (!(e.getBlock().getState() instanceof Sign)) return;
		if (((org.bukkit.material.Sign)e.getBlock().getState().getData()).isWallSign()) {
			if (e.getLine(0).equalsIgnoreCase(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("shops.prefix"))))) {
				Block chest = e.getBlock().getRelative(((org.bukkit.material.Sign)e.getBlock().getState().getData()).getAttachedFace());
				if (chest.getType() != Material.CHEST && chest.getType() != Material.TRAPPED_CHEST) {
					QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-chest", true);
					e.setCancelled(true);
					return;
				}
				else {
					try {
						double price = Double.valueOf(e.getLine(2));
						ShopType type;
						if (e.getLine(3).equalsIgnoreCase("sell")) type = ShopType.SELL;
						else if (e.getLine(3).equalsIgnoreCase("buy")) type = ShopType.BUY;
						else if (e.getLine(3).equalsIgnoreCase("sellall")) type = ShopType.SELL_ALL;
						else {
							QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-valid-type", true);
							e.setCancelled(true);
							return;
						}
						try {
							int amount = Integer.parseInt(e.getLine(1));
							if (amount > 0) {
								PlayerShop shop = new PlayerShop(e.getBlock(), chest, e.getPlayer(), amount, price, type);
								e.setCancelled(true);
								shop.update(true);
							}
							else {
								QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-valid-amount", true);
								e.setCancelled(true);
								return;
							}
						} catch(NumberFormatException x) {
							QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-valid-amount", true);
							e.setCancelled(true);
							return;
						}
					} catch(NumberFormatException x) {
						QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-valid-price", true);
						e.setCancelled(true);
						return;
					}
				}
			}
			else if (e.getLine(0).equalsIgnoreCase(ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', QuickMarket.getInstance().cfg.getString("markets.prefix"))))) {
				if (e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					Block chest = e.getBlock().getRelative(((org.bukkit.material.Sign)e.getBlock().getState().getData()).getAttachedFace());
					if (chest.getType() != Material.CHEST && chest.getType() != Material.TRAPPED_CHEST) {
						QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "shops.not-a-chest", true);
						e.setCancelled(true);
						return;
					}
					else createMarket(e, chest, 1, 100.0, ShopType.BUY);
				}
				else e.setCancelled(true);
			}
			else if (e.getLine(0).equalsIgnoreCase("[MarketStand]")) {
				if (e.getPlayer().hasPermission("QuickMarket.market.admin")) {
					try {					
						double price = Double.valueOf(e.getLine(1));
						e.setCancelled(true);
						new MarketStand(e.getBlock(), price);
					} catch(NumberFormatException x) {
						QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "market.not-a-valid-price", true);
						e.setCancelled(true);
						return;
					}
				}
				else e.setCancelled(true);
			}
		}
	}

	private void createMarket(final SignChangeEvent e, final Block chest, final int amount, final double price, final ShopType type) {
		e.setCancelled(true);
		QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "market.link", true);
		link.put(e.getPlayer().getUniqueId(), new MarketLink() {

			@Override
			public void link(Block block) {
				if (!MarketStand.map.containsKey(MarketStand.location(block.getLocation()))) {
					QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "market.link-abort", true);
					return;
				}
				QuickMarket.getInstance().local.sendTranslation(e.getPlayer(), "market.link-success", true);
				PlayerMarket shop = new PlayerMarket(MarketStand.location(block.getLocation()), e.getBlock(), chest, e.getPlayer(), amount, price, type);
				shop.update(true);
		}});
	}

}
