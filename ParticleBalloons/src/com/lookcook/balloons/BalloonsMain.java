package com.lookcook.balloons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class BalloonsMain extends JavaPlugin implements Listener {

	public HashMap<Entity, Color> balloons = new HashMap<Entity, Color>();
	public int i = 0;
	public String name = ChatColor.RED + "[" + ChatColor.LIGHT_PURPLE + "Particle" + ChatColor.AQUA + "Balloons"
			+ ChatColor.RED + "]";

	public void onDisable() {
		for (Entity entity : balloons.keySet()) {
			entity.remove();
			balloons.remove(entity);
		}
	}

	public void onEnable() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
		if (!this.getConfig().contains("NoPermission")) {
			this.getConfig().addDefault("NoPermission", "&c[&dParticle&bBalloons&c] &cNo Permission!");
		}
		if (!this.getConfig().contains("RemoveBalloon")) {
			this.getConfig().addDefault("RemoveBalloon", "&c[&dParticle&bBalloons&c] &6Removed current Balloon.");
		}
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				i++;
				if (i == 16) {
					i = 0;
				}
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (player.getOpenInventory().getTopInventory().getName().equals(getMainMenu().getName())) {
						player.openInventory(getMainMenu());
					}
				}
			}
		}, 1L, 10L);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				for (Entity entity : balloons.keySet()) {
					if (!((LivingEntity) entity).isLeashed()) {
						entity.remove();
						balloons.remove(entity);
						return;
					}
					line(entity, ((LivingEntity) entity).getLeashHolder());
					double phi = 0;
					Location loc = entity.getLocation();
					for (double theta = 0; theta <= 2 * Math.PI; theta += Math.PI / 40) {
						phi += Math.PI / 10;
						double r = 0.8;
						double x = r * Math.cos(theta) * Math.sin(phi);
						double y = r * Math.cos(phi) + 2.5;
						double z = r * Math.sin(theta) * Math.sin(phi);

						loc.add(x, y - 1.5, z);
						Color color = balloons.get(entity);
						ParticleEffect.REDSTONE.display(
								new ParticleEffect.OrdinaryColor(color.getRed(), color.getGreen(), color.getBlue()),
								loc, 20);
						loc.subtract(x, y - 1.5, z);
					}
				}
			}
		}, 1L, 2L);
	}

	public void summonBalloon(final Player player, Color color) {
		final Entity entity = player.getWorld().spawnEntity(player.getLocation(), EntityType.BAT);
		((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
		balloons.put(entity, color);
		((LivingEntity) entity).setLeashHolder(player);
	}

	public List<Entity> getNearbyEntities(Location where, int range) {
		List<Entity> found = new ArrayList<Entity>();

		for (Entity entity : where.getWorld().getEntities()) {
			if (isInBorder(where, entity.getLocation(), range)) {
				found.add(entity);
			}
		}
		return found;
	}

	public boolean isInBorder(Location center, Location notCenter, int range) {
		int x = center.getBlockX(), z = center.getBlockZ();
		int x1 = notCenter.getBlockX(), z1 = notCenter.getBlockZ();

		if (x1 >= (x + range) || z1 >= (z + range) || x1 <= (x - range) || z1 <= (z - range)) {
			return false;
		}
		return true;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		for (Entity entity : balloons.keySet()) {
			LivingEntity lE = (LivingEntity) entity;
			if (lE.getLeashHolder().equals(player)) {
				if (block != null) {
					if (block.getType().equals(Material.FENCE) || block.getType().equals(Material.NETHER_FENCE)) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		Entity entity = event.getEntity();
		if (balloons.keySet().contains(entity)) {
			entity.setVelocity(damager.getLocation().getDirection().multiply(2.5));
			ParticleEffect.EXPLOSION_LARGE.display(0, 0, 0, 0, 1, entity.getLocation(), 20);
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onUnleash(PlayerUnleashEntityEvent event) {
		Player player = event.getPlayer();
		Entity entity = event.getEntity();
		if (balloons.keySet().contains(entity)) {
			removeBalloon(player);
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		final Player player = event.getPlayer();
		for (Entity entity : balloons.keySet()) {
			LivingEntity lE = (LivingEntity) entity;
			if (lE.getLeashHolder().equals(player)) {
				final Color color = balloons.get(entity);
				removeBalloon(player);
				Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					public void run() {
						summonBalloon(player, color);
					}
				}, 3L);
			}
		}
	}

	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		Inventory inv = event.getInventory();
		ItemStack item = event.getCurrentItem();
		Player player = (Player) event.getWhoClicked();

		if (inv == null) {
			return;
		}
		if (item == null) {
			return;
		}

		if (inv.getName().equals(getMainMenu().getName())) {
			event.setCancelled(true);
			if (item.getType().equals(Material.WOOL)) {
				if (!player.hasPermission("balloons.*") && !player.isOp() && !player.hasPermission(
						"balloons." + item.getItemMeta().getDisplayName().toLowerCase().replace("_", ""))) {
					player.sendMessage(
							ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("NoPermission")));
					player.closeInventory();
					return;
				}
				DyeColor dyeColor = DyeColor.getByData((byte) item.getDurability());
				Color color = dyeColor.getColor();
				removeBalloon(player);
				summonBalloon(player, color);
				player.closeInventory();
			}
			if (item.getType().equals(Material.MILK_BUCKET)) {
				removeBalloon(player);
				player.sendMessage(
						ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("RemoveBalloon")));
				player.closeInventory();
			}
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		removeBalloon(event.getPlayer());
	}

	public Inventory getMainMenu() {
		Inventory inv = Bukkit.createInventory(null, 36, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Balloons");

		ItemStack glass = rename(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) i), " ");

		for (int x = 0; x <= 8; x++) {
			inv.setItem(x, glass);
			inv.setItem(x + 27, glass);
		}

		for (DyeColor color : DyeColor.values()) {
			String itemName = color.toString().substring(0, 1).toUpperCase()
					+ color.toString().substring(1, color.toString().length()).toLowerCase();
			ItemStack wool = rename(new ItemStack(Material.WOOL, 1, color.getData()), itemName.replace("_", " "));
			inv.addItem(wool);
		}

		inv.setItem(18, glass);
		inv.setItem(17, glass);
		inv.setItem(26, glass);
		inv.setItem(9, glass);
		inv.setItem(35, rename(new ItemStack(Material.MILK_BUCKET), ChatColor.RED + "Remove Current Balloon"));

		return inv;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("balloons")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "No Console");
				return false;
			}
			Player player = (Player) sender;
			player.openInventory(getMainMenu());
		}
		return false;
	}

	public static ItemStack rename(ItemStack item, String name) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		item.setItemMeta(meta);
		return item;
	}

	public void line(Entity entity, Entity player) {
		if (entity.getWorld().equals(player.getWorld())) {
			if (entity.getLocation().distance(player.getLocation()) > 3.8) {
				Vector direction = player.getLocation().toVector().subtract(entity.getLocation().toVector())
						.normalize();
				entity.setVelocity(entity.getVelocity().add(direction.multiply(0.4)));
			} else {
				entity.setVelocity(entity.getVelocity().add(new Vector(0, 0.3, 0)));
			}
		} else {
			entity.teleport(player);
		}
		if (entity.getLocation().distance(player.getLocation()) > 10) {
			entity.teleport(player);
		}
	}

	public void removeBalloon(Player player) {
		for (Entity entity : balloons.keySet()) {
			if (((LivingEntity) entity).getLeashHolder().equals(player)) {
				entity.remove();
				balloons.remove(entity);
			}
		}
	}
}
