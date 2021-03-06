package swordman.minigame.quake.gun;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitTask;

import swordman.minigame.quake.Main;
import swordman.minigame.quake.effects.QuakeEffects;

public class GunHandler {

	public static List<String> reloaded = new ArrayList<String>();

	public static void giveGun(final Player p, final QuakeGun gun) {
		p.getInventory().addItem(gun.getItemStack());
		if (!reloaded.contains(p.getName())) {
			reload(p, gun.reloadTime);
		}
	}

	public static QuakeGun gunInHand(final Player p) {
		for (final QuakeGun gun : QuakeGun.values())
			if (p.getItemInHand().isSimilar(gun.getItemStack())) {
				return gun;
			}

		return null;
	}

	public static void reload(final Player p, final float time) {
		final ReloadTimer timer = new ReloadTimer(p, time);
		final BukkitTask task = Bukkit.getScheduler().runTaskTimer(Main.plugin,
				timer, 0L, 1L);
		timer.setTask(task);
	}

	public static void shoot(final Player p, final QuakeGun gun) {
		if (reloaded.contains(p.getName()) && gun != null) {
			reloaded.remove(p.getName());
			reload(p, gun.reloadTime);

			final Set<Material> transparent = new HashSet<Material>();
			transparent.add(Material.AIR); // Materials that are considered
											// transparent and you can shoot
											// through
			transparent.add(Material.WATER);
			transparent.add(Material.SIGN);
			transparent.add(Material.WALL_SIGN);
			transparent.add(Material.SIGN_POST);
			transparent.add(Material.DEAD_BUSH);
			transparent.add(Material.STATIONARY_WATER);
			transparent.add(Material.GLASS);

			final int distance = 100;

			final List<Block> blocks = p.getLineOfSight(transparent, distance);

			final List<Entity> entities = p.getNearbyEntities(distance,
					distance, distance);

			for (int i = 0; i < entities.size(); i++) {
				Entity en = entities.get(i);
				if (!(en instanceof LivingEntity)) {
					entities.remove(i);
				}
			}

			boolean swap = false;

			for (final Block b : blocks) {
				if (!swap) {
					QuakeEffects.gunShot(b.getLocation());
				}
				swap = !swap;

				if (b.getType() == Material.GLASS) {
					breakBlocks(b);
				}

				for (final Entity en : entities) {
					if (en.getLocation().distance(b.getLocation()) < 1
							|| (en instanceof Player && ((Player) en)
									.getEyeLocation().distance(b.getLocation()) < 1)) {
						((LivingEntity) en).setHealth(0);

						QuakeEffects.gunDeath(en.getLocation());
						return;
					}
				}
			}
		}
	}

	private static class ReloadTimer implements Runnable {
		final Player p;
		BukkitTask task;

		final float time;

		private ReloadTimer(final Player p, final float time) {
			this.p = p;
			this.time = time;
			p.setExp(0);
		}

		@Override
		public void run() {
			if (this.p.getExp() < 1)
				this.p.setExp((float) (this.p.getExp() + 1f / (this.time * 20)));
			else {
				reloaded.add(this.p.getName());
				this.task.cancel();
			}
		}

		public void setTask(final BukkitTask task) {
			this.task = task;
		}
	}

	public static void breakBlocks(Block b) {
		Material m = b.getType();

		List<BlockFace> faces = new ArrayList<BlockFace>();
		faces.add(BlockFace.UP);
		faces.add(BlockFace.DOWN);
		faces.add(BlockFace.EAST);
		faces.add(BlockFace.WEST);

		for (BlockFace face : faces) {
			Block b1 = b.getRelative(face);
			if (b1.getType() == m) {
				b1.breakNaturally();
				breakBlocks(b);
			}
		}
	}

}
