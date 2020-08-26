package com.iridium.iridiumskyblock.listeners;

import com.iridium.iridiumskyblock.IridiumSkyblock;
import com.iridium.iridiumskyblock.Island;
import com.iridium.iridiumskyblock.User;
import com.iridium.iridiumskyblock.managers.IslandManager;
import com.iridium.iridiumskyblock.managers.UserManager;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Objects;
import java.util.function.Supplier;

public class EntityDamageByEntityListener implements Listener {

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        final Entity damagee = event.getEntity();
        if (!(damagee instanceof Player)) return;
        final Player player = (Player) damagee;
        final User user = UserManager.getUser(player.getUniqueId());
        final Location damageeLocation = damagee.getLocation();
        final IslandManager islandManager = IridiumSkyblock.getIslandManager();
        final Island island = islandManager.getIslandViaLocation(damageeLocation);
        if (island == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

        //The user is visiting this island, so disable damage
        if (user.islandID != island.getId() && IridiumSkyblock.getConfiguration().disablePvPOnIslands) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        try {
            final Entity damagee = event.getEntity();
            final Location damageeLocation = damagee.getLocation();
            final IslandManager islandManager = IridiumSkyblock.getIslandManager();
            final Island island = islandManager.getIslandViaLocation(damageeLocation);
            if (island == null) return;

            final Entity damager = event.getDamager();

            // Using suppliers to defer work if unnecessary
            // This includes seemingly innocuous downcast operations
            final Supplier<Player> damageePlayerSupplier = () -> (Player) damagee;
            final Supplier<User> damageeUserSupplier = () -> UserManager.getUser(damageePlayerSupplier.get().getUniqueId());
            final Supplier<Island> damageeIslandSupplier = () -> damageeUserSupplier.get().getIsland();
            final Supplier<Arrow> arrowSupplier = () -> (Arrow) damager;
            final Supplier<ProjectileSource> projectileSourceSupplier = () -> arrowSupplier.get().getShooter();
            final Supplier<Player> shooterSupplier = () -> (Player) projectileSourceSupplier.get();
            final Supplier<User> shootingUserSupplier = () -> UserManager.getUser(Objects.requireNonNull(shooterSupplier.get().getUniqueId()));
            final Supplier<Player> damagingPlayerSupplier = () -> (Player) damager;
            final Supplier<User> damagingUserSupplier = () -> UserManager.getUser(damagingPlayerSupplier.get().getUniqueId());

            // Deals with two players pvping in IridiumSkyblock world
            if (IridiumSkyblock.getConfiguration().disablePvPOnIslands
                    && damagee instanceof Player
                    && damager instanceof Player) {
                event.setCancelled(true);
                return;
            }

            // Deals with A player getting damaged by a bow fired from a player in IridiumSkyblock world
            if (IridiumSkyblock.getConfiguration().disablePvPOnIslands
                    && damagee instanceof Player
                    && damager instanceof Arrow
                    && projectileSourceSupplier.get() instanceof Player) {
                event.setCancelled(true);
                return;
            }

            // Deals with a player attacking animals with bows that are not from their island
            if (damager instanceof Arrow
                    && !(damagee instanceof Player)
                    && projectileSourceSupplier.get() instanceof Player
                    && !island.getPermissions(shootingUserSupplier.get()).killMobs) {
                event.setCancelled(true);
                return;
            }

            // Deals with a player attacking animals that are not from their island
            if (damager instanceof Player
                    && !(damagee instanceof Player)
                    && !island.getPermissions(damagingUserSupplier.get()).killMobs) {
                event.setCancelled(true);
                return;
            }

            //Deals with a mob attacking a player that doesn't belong to the island (/is home traps?)
            if (IridiumSkyblock.getConfiguration().disablePvPOnIslands
                    && damagee instanceof Player
                    && !(damager instanceof Player)) {
                if (damageeIslandSupplier.get() != null) {
                    if (!damageeIslandSupplier.get().isInIsland(damager.getLocation())) {
                        event.setCancelled(true);
                        return;
                    }
                } else {
                    event.setCancelled(true);
                    return;
                }
            }

            // Deals with two allies pvping
            if (IridiumSkyblock.getConfiguration().disablePvPBetweenIslandMembers
                    && damagee instanceof Player
                    && damager instanceof Player
                    && damageeIslandSupplier.get() != null
                    && damageeIslandSupplier.get().equals(damagingUserSupplier.get().getIsland())) {
                event.setCancelled(true);
                return;
            }

            // Deals with two allies pvping with bows
            if (IridiumSkyblock.getConfiguration().disablePvPBetweenIslandMembers
                    && damagee instanceof Player
                    && damager instanceof Arrow
                    && projectileSourceSupplier.get() instanceof Player
                    && damageeIslandSupplier.get() != null
                    && damageeIslandSupplier.get().equals(damagingUserSupplier.get().getIsland())) {
                event.setCancelled(true);
                return;
            }
        } catch (Exception ex) {
            IridiumSkyblock.getInstance().sendErrorMessage(ex);
        }
    }

    @EventHandler
    public void onVehicleDamage(VehicleDamageEvent event) {
        try {
            final Vehicle vehicle = event.getVehicle();
            final Location location = vehicle.getLocation();
            final IslandManager islandManager = IridiumSkyblock.getIslandManager();
            final Island island = islandManager.getIslandViaLocation(location);
            if (island == null) return;

            final Entity attacker = event.getAttacker();
            if (!(attacker instanceof Player)) return;

            final Player attackerPlayer = (Player) attacker;
            final User attackerUser = UserManager.getUser(attackerPlayer.getUniqueId());

            if (!island.getPermissions(attackerUser).killMobs)
                event.setCancelled(true);
        } catch (Exception ex) {
            IridiumSkyblock.getInstance().sendErrorMessage(ex);
        }
    }
}
