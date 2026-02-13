package io.pancakesmp.pancakeabilities;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class PancakeAbilitiesPlugin extends JavaPlugin implements Listener {

    private NamespacedKey typeKey;
    private NamespacedKey levelKey;

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<Location, Material> temporaryBlocks = new HashMap<>();
    private final Map<UUID, Integer> coconutShieldCharges = new HashMap<>();
    private final Map<UUID, List<Display>> coconutDisplays = new HashMap<>();

    private static final long COOLDOWN_MS = 9000;

    @Override
    public void onEnable() {
        typeKey = new NamespacedKey(this, "pancake_type");
        levelKey = new NamespacedKey(this, "pancake_level");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PancakeAbilities enabled.");
    }

    @Override
    public void onDisable() {
        coconutDisplays.values().forEach(list -> list.forEach(Entity::remove));
        coconutDisplays.clear();
        coconutShieldCharges.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /pancake <type> [level]");
            return true;
        }

        PancakeType type = PancakeType.fromKey(args[0]);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "Unknown pancake type.");
            return true;
        }

        int level = 1;
        if (args.length > 1) {
            try {
                level = Math.max(1, Math.min(5, Integer.parseInt(args[1])));
            } catch (NumberFormatException ignored) {
                level = 1;
            }
        }

        player.getInventory().addItem(createPancakeItem(type, level));
        player.sendMessage(ChatColor.GOLD + "Given " + type.displayName + " (Level " + level + ").");
        return true;
    }

    private ItemStack createPancakeItem(PancakeType type, int level) {
        ItemStack stack = new ItemStack(type.icon);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(type.color + type.displayName + ChatColor.GRAY + " [L" + level + "]");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Right-click to activate ability");
        lore.add(ChatColor.GRAY + "Scales with level: range, duration,");
        lore.add(ChatColor.GRAY + "damage and projectile count");
        meta.setLore(lore);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(typeKey, PersistentDataType.STRING, type.key);
        pdc.set(levelKey, PersistentDataType.INTEGER, level);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isRightClick()) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(typeKey, PersistentDataType.STRING)) return;

        String key = pdc.get(typeKey, PersistentDataType.STRING);
        Integer levelRaw = pdc.get(levelKey, PersistentDataType.INTEGER);
        int level = levelRaw == null ? 1 : Math.max(1, Math.min(5, levelRaw));
        PancakeType type = PancakeType.fromKey(key);
        if (type == null) return;

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MS) {
            long seconds = (COOLDOWN_MS - (now - last) + 999) / 1000;
            player.sendActionBar(ChatColor.RED + "Ability cooldown: " + seconds + "s");
            return;
        }

        cooldowns.put(player.getUniqueId(), now);
        activateAbility(player, type, level);
        event.setCancelled(true);
    }

    private void activateAbility(Player player, PancakeType type, int level) {
        switch (type) {
            case CHOCOLATE -> cocoaCurse(player, level);
            case VANILLA -> balancedAura(player, level);
            case ICE -> frostZone(player, level);
            case LAVA -> moltenBurst(player, level);
            case HONEY -> syrupTrap(player, level);
            case STRAWBERRY -> berryDash(player, level);
            case BLUEBERRY -> berryBarrage(player, level);
            case COCONUT -> shellGuard(player, level);
            case MATCHA -> focusPulse(player, level);
        }
    }

    private List<Player> nearbyEnemies(Player source, double radius) {
        List<Player> result = new ArrayList<>();
        for (Entity e : source.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player p && !p.getUniqueId().equals(source.getUniqueId())) result.add(p);
        }
        return result;
    }

    private void cocoaCurse(Player player, int level) {
        double radius = 4 + level * 1.5;
        int duration = 50 + level * 20;
        for (Player p : nearbyEnemies(player, radius)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 1 + (level / 3), true, false, true));
            p.getWorld().spawnParticle(Particle.DRIPPING_HONEY, p.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.02);
        }

        World world = player.getWorld();
        Location center = player.getLocation();
        int stainRadius = 2 + level;
        for (int x = -stainRadius; x <= stainRadius; x++) {
            for (int z = -stainRadius; z <= stainRadius; z++) {
                Location floor = center.clone().add(x, -1, z);
                Location top = center.clone().add(x, 0, z);
                if (floor.getBlock().getType().isSolid() && top.getBlock().getType() == Material.AIR) {
                    setTemporaryBlock(top.getBlock(), Material.BROWN_CARPET, 80 + (level * 20));
                }
            }
        }
        world.playSound(center, Sound.ENTITY_SLIME_SQUISH, 1f, 0.7f);
    }

    private void balancedAura(Player player, int level) {
        World world = player.getWorld();
        double radius = 3 + (level * 1.2);
        int ticks = 80 + (level * 20);

        for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) {
            if (effect.getType().isBad()) player.removePotionEffect(effect.getType());
        }

        new BukkitRunnableLoop(this, ticks, 10, i -> {
            Location c = player.getLocation();
            for (double t = 0; t < Math.PI * 2; t += Math.PI / 12) {
                Location ring = c.clone().add(Math.cos(t) * radius, 0.2, Math.sin(t) * radius);
                world.spawnParticle(Particle.END_ROD, ring, 1, 0, 0, 0, 0);
            }
            if (i % 2 == 0) {
                for (Player enemy : nearbyEnemies(player, radius)) {
                    Vector push = enemy.getLocation().toVector().subtract(c.toVector()).normalize().multiply(0.2 + (level * 0.04));
                    enemy.setVelocity(enemy.getVelocity().add(push));
                }
            }
        });
        world.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.4f);
    }

    private void frostZone(Player player, int level) {
        World world = player.getWorld();
        Location center = player.getLocation();
        double radius = 4 + level;

        for (Player p : nearbyEnemies(player, radius)) {
            p.setFreezeTicks(40 + level * 10);
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60 + level * 20, 2, true, false, true));
            if (!p.isOnGround()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40 + level * 20, 0, true, false, true));
            }
        }

        int r = 2 + level;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                Location bLoc = center.clone().add(x, -1, z);
                Block block = bLoc.getBlock();
                if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                    setTemporaryBlock(block, Material.PACKED_ICE, 100 + level * 20);
                }
            }
        }

        world.spawnParticle(Particle.SNOWFLAKE, center, 80, radius / 2, 1, radius / 2, 0.02);
        world.playSound(center, Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
    }

    private void moltenBurst(Player player, int level) {
        World world = player.getWorld();
        Location center = player.getLocation();
        double radius = 3 + level * 1.2;
        double damage = 3.0 + level * 1.5;

        world.spawnParticle(Particle.LAVA, center, 100, radius / 2, 0.6, radius / 2, 0.03);
        world.spawnParticle(Particle.FLAME, center, 120, radius / 2, 1, radius / 2, 0.05);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.8f);

        for (Player p : nearbyEnemies(player, radius)) {
            p.damage(damage, player);
            p.setFireTicks(40 + level * 20);
        }

        int r = 1 + level;
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                Location floor = center.clone().add(x, -1, z);
                if (floor.distanceSquared(center.clone().add(0, -1, 0)) > (r * r)) continue;
                Block b = floor.getBlock();
                if (b.getType().isSolid() && b.getType() != Material.BEDROCK) {
                    setTemporaryBlock(b, Material.MAGMA_BLOCK, 80 + level * 20);
                }
            }
        }
    }

    private void syrupTrap(Player player, int level) {
        RayTraceResult result = player.rayTraceBlocks(20);
        Location target = result != null && result.getHitPosition() != null
                ? result.getHitPosition().toLocation(player.getWorld())
                : player.getLocation().add(player.getLocation().getDirection().multiply(6));

        World world = player.getWorld();
        int maxRadius = 2 + level;
        int durationTicks = 100;

        new BukkitRunnableLoop(this, durationTicks, 10, i -> {
            double progress = (double) i / (durationTicks / 10.0);
            double radius = Math.min(maxRadius, 1 + progress * maxRadius);
            world.spawnParticle(Particle.FALLING_HONEY, target.clone().add(0, 0.1, 0), 40, radius / 2, 0.05, radius / 2, 0.01);

            for (Entity e : world.getNearbyEntities(target, radius, 1.5, radius)) {
                if (e instanceof Player p && !p.getUniqueId().equals(player.getUniqueId())) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 6, true, false, true));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30, 128, true, false, true));
                }
            }
        });

        world.playSound(target, Sound.ITEM_HONEY_BOTTLE_DRINK, 1f, 0.8f);
    }

    private void berryDash(Player player, int level) {
        Vector dash = player.getLocation().getDirection().normalize().multiply(1.4 + level * 0.25);
        player.setVelocity(dash);
        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.9f, 1.5f);

        final UUID source = player.getUniqueId();
        final boolean[] hitDone = {false};

        new BukkitRunnableLoop(this, 30, 1, i -> {
            Player p = Bukkit.getPlayer(source);
            if (p == null || !p.isOnline()) return;
            world.spawnParticle(Particle.CHERRY_LEAVES, p.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.01);

            if (!hitDone[0]) {
                for (Entity e : p.getNearbyEntities(1.2, 1.2, 1.2)) {
                    if (e instanceof Player target && !target.getUniqueId().equals(source)) {
                        target.setVelocity(target.getVelocity().add(new Vector(0, 0.7 + level * 0.1, 0)));
                        target.damage(2.0 + level, p);
                        hitDone[0] = true;
                        break;
                    }
                }
            }

            if (p.isOnGround() && i > 2) {
                world.spawnParticle(Particle.CLOUD, p.getLocation(), 24, 1.2, 0.1, 1.2, 0.02);
                for (Player enemy : nearbyEnemies(p, 2 + level * 0.5)) {
                    Vector kb = enemy.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(0.4 + 0.08 * level);
                    enemy.setVelocity(enemy.getVelocity().add(kb));
                }
            }
        });
    }

    private void berryBarrage(Player player, int level) {
        int shots = 3 + Math.min(2, level / 2);
        World world = player.getWorld();

        for (int i = 0; i < shots; i++) {
            int delay = i * 4;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Snowball snowball = player.launchProjectile(Snowball.class);
                snowball.setItem(new ItemStack(Material.BLUE_CONCRETE));
                snowball.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "blueberry_proj:" + player.getUniqueId());
                snowball.setVelocity(player.getLocation().getDirection().normalize().multiply(1.3 + level * 0.12));
                world.playSound(player.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.8f, 1.3f);
            }, delay);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        String tag = snowball.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (tag == null || !tag.startsWith("blueberry_proj:")) return;

        World world = snowball.getWorld();
        Location hit = snowball.getLocation();
        world.spawnParticle(Particle.EXPLOSION, hit, 2, 0.1, 0.1, 0.1, 0.01);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, hit, 20, 0.25, 0.25, 0.25, 0.01);
        world.playSound(hit, Sound.ENTITY_SLIME_JUMP, 0.8f, 1.6f);

        String uuidText = tag.substring("blueberry_proj:".length());
        UUID shooterId;
        try {
            shooterId = UUID.fromString(uuidText);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Player shooter = Bukkit.getPlayer(shooterId);
        for (Entity e : world.getNearbyEntities(hit, 2.2, 2.2, 2.2)) {
            if (!(e instanceof Player p)) continue;
            if (p.getUniqueId().equals(shooterId)) continue;
            Vector kb = p.getLocation().toVector().subtract(hit.toVector()).normalize().multiply(0.5);
            p.setVelocity(p.getVelocity().add(kb));
            if (shooter != null) p.damage(2.5, shooter);
        }
    }

    private void shellGuard(Player player, int level) {
        UUID id = player.getUniqueId();
        coconutShieldCharges.put(id, 2 + level);

        List<Display> displays = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BlockDisplay d = player.getWorld().spawn(player.getLocation().add(0, 1, 0), BlockDisplay.class);
            d.setBlock(Bukkit.createBlockData(Material.JUNGLE_LOG));
            displays.add(d);
        }
        coconutDisplays.put(id, displays);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.1f);

        new BukkitRunnableLoop(this, 120 + level * 20, 1, i -> {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                clearShield(id, null);
                return;
            }
            List<Display> ds = coconutDisplays.get(id);
            if (ds == null) return;

            for (int idx = 0; idx < ds.size(); idx++) {
                double angle = (i * 0.2) + (idx * (Math.PI * 2 / ds.size()));
                Location base = p.getLocation().add(Math.cos(angle) * 1.2, 1.0, Math.sin(angle) * 1.2);
                ds.get(idx).teleport(base);
            }
        });
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID id = player.getUniqueId();
        Integer charges = coconutShieldCharges.get(id);
        if (charges == null || charges <= 0) return;

        event.setCancelled(true);
        int left = charges - 1;
        coconutShieldCharges.put(id, left);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.9f);

        if (left <= 0) {
            clearShield(id, player);
            for (Player enemy : nearbyEnemies(player, 3)) {
                Vector kb = enemy.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(0.6);
                enemy.setVelocity(enemy.getVelocity().add(kb));
            }
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 30, 0.7, 0.7, 0.7,
                    Bukkit.createBlockData(Material.JUNGLE_LOG));
        }
    }

    private void clearShield(UUID id, Player playerContext) {
        coconutShieldCharges.remove(id);
        List<Display> displays = coconutDisplays.remove(id);
        if (displays != null) displays.forEach(Entity::remove);
        if (playerContext != null) {
            playerContext.getWorld().playSound(playerContext.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.9f, 0.7f);
        }
    }

    private void focusPulse(Player player, int level) {
        World world = player.getWorld();
        double radius = 6 + level * 2;
        int pulses = 3 + level;

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60 + level * 20, 0, true, false, true));

        for (int i = 0; i < pulses; i++) {
            int delay = i * 12;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                Location c = player.getLocation();
                world.spawnParticle(Particle.VILLAGER_HAPPY, c, 40, radius / 2, 1, radius / 2, 0.02);
                for (Player enemy : nearbyEnemies(player, radius)) {
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 50, 0, true, false, true));
                }
                world.playSound(c, Sound.BLOCK_BEACON_POWER_SELECT, 0.8f, 1.4f);
            }, delay);
        }
    }

    private void setTemporaryBlock(Block block, Material temporary, int ticks) {
        Material original = block.getType();
        block.setType(temporary, false);
        temporaryBlocks.put(block.getLocation(), original);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Material restore = temporaryBlocks.remove(block.getLocation());
            if (restore != null && block.getType() == temporary) {
                if (temporary == Material.PACKED_ICE) {
                    block.getWorld().spawnParticle(Particle.BLOCK, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2,
                            Bukkit.createBlockData(Material.PACKED_ICE));
                }
                block.setType(restore, false);
            }
        }, ticks);
    }

    private enum PancakeType {
        CHOCOLATE("chocolate", "Chocolate Pancake", ChatColor.DARK_RED, Material.COCOA_BEANS),
        VANILLA("vanilla", "Vanilla Pancake", ChatColor.WHITE, Material.SUGAR),
        ICE("ice", "Ice Pancake", ChatColor.AQUA, Material.ICE),
        LAVA("lava", "Lava Pancake", ChatColor.RED, Material.MAGMA_CREAM),
        HONEY("honey", "Honey Pancake", ChatColor.GOLD, Material.HONEY_BOTTLE),
        STRAWBERRY("strawberry", "Strawberry Pancake", ChatColor.LIGHT_PURPLE, Material.SWEET_BERRIES),
        BLUEBERRY("blueberry", "Blueberry Pancake", ChatColor.BLUE, Material.LAPIS_LAZULI),
        COCONUT("coconut", "Coconut Pancake", ChatColor.GRAY, Material.NAUTILUS_SHELL),
        MATCHA("matcha", "Matcha Pancake", ChatColor.GREEN, Material.GREEN_DYE);

        private final String key;
        private final String displayName;
        private final ChatColor color;
        private final Material icon;

        PancakeType(String key, String displayName, ChatColor color, Material icon) {
            this.key = key;
            this.displayName = displayName;
            this.color = color;
            this.icon = icon;
        }

        static PancakeType fromKey(String key) {
            for (PancakeType type : values()) {
                if (type.key.equalsIgnoreCase(key)) return type;
            }
            return null;
        }
    }

    private static class BukkitRunnableLoop {
        BukkitRunnableLoop(JavaPlugin plugin, int totalTicks, int interval, java.util.function.IntConsumer eachRun) {
            final int[] t = {0};
            BukkitTask[] task = new BukkitTask[1];
            task[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                eachRun.accept(t[0] / interval);
                t[0] += interval;
                if (t[0] >= totalTicks) {
                    task[0].cancel();
                }
            }, 0L, interval);
        }
    }
}
