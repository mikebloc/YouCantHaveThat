package me.cutzuu.YouCantHaveThat;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/*
    Cutzuu
    Version: 1.0.5
    Date: August 3, 2026

    @cutzuuu - Discord
    Github.com/cutzuu
*/

public final class main extends JavaPlugin implements Listener
{
    private Set<Material> blockedItems;

    @Override
    public void onEnable()
    {
        saveDefaultConfig();
        loadConfiguration();
        getLogger().info("YouCantHaveThat Enabled");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("reloadconfig")).setExecutor(this);


        Global.configToggleAntiBlockBreak = this.getConfig().getBoolean("AntiBreak");
        Global.configToggleBreakIntoAir = this.getConfig().getBoolean("BreakIntoAir");
        Global.configToggleStoragePurge = this.getConfig().getBoolean("StoragePurge");
    }


    public static class Global
    {
        public static boolean configToggleAntiBlockBreak;
        public static boolean configToggleBreakIntoAir;
        public static boolean configToggleStoragePurge;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (args.length == 0)
        {
            sender.sendMessage("§eUsage: /youcanthavethat reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload"))
        {
            if (sender.hasPermission("youcanthavethat.reload"))
            {
                reloadConfig();
                loadConfiguration();
                sender.sendMessage("§7[§6YouCantHaveThat§7] §aConfig reloaded.");
            }
            else sender.sendMessage("§cYou don't have permission to do that.");
            return true;
        }

        sender.sendMessage("§cUnknown subcommand.");
        return true;
    }

    ////////////////////////////////////////////////////////////
    // Establishing banned items via Config.
    ////////////////////////////////////////////////////////////
    private void loadConfiguration()
    {
        Global.configToggleAntiBlockBreak = this.getConfig().getBoolean("AntiBreak");
        Global.configToggleBreakIntoAir = this.getConfig().getBoolean("BreakIntoAir");
        Global.configToggleStoragePurge = this.getConfig().getBoolean("StoragePurge");

        blockedItems = new HashSet<>();
        FileConfiguration config = getConfig();
        List<String> blockNames = config.getStringList("blocked-items");

        for (String name : blockNames)
        {
            try
            {
                Material material = Material.valueOf(name.toUpperCase());
                blockedItems.add(material);
            } catch (IllegalArgumentException e)
            {
                getLogger().warning("Invalid block type in config: " + name);
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Interaction Check
    // Checks for any placements of items/materials
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void interactionCheck (PlayerInteractEvent e)
    {
        Player player = e.getPlayer();
        Material material = e.getMaterial();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();

        if (!player.hasPermission("youcanthavethat.bypass"))
        {
            if (blockedItems.contains(material))
            {
                e.setCancelled(true);
                checkInventoryProcess(inventory, secondHand);
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Block Break Check
    // Runs off the Anti-Inventory List
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void blockBreakChecker (BlockBreakEvent e)
    {
        Block block = e.getBlock();
        Player player = e.getPlayer();
        Material material = block.getType();

        if (!player.hasPermission("youcanthavethat.bypass"))
        {
            if (Global.configToggleAntiBlockBreak)
            {
                if (blockedItems.contains(material))
                {
                    if (Global.configToggleBreakIntoAir) block.setType(Material.AIR);
                    else e.setCancelled(true);
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////
    // Opening any container will trigger the check.
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck1 (InventoryOpenEvent e)
    {
        Player player = (Player) e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack secondHand = inventory.getItemInOffHand();

        if (!player.hasPermission("youcanthavethat.bypass")) checkInventoryProcess(inventory, secondHand);
    }

    ////////////////////////////////////////////////////////////
    // PlayerPickUpItem, Check Inventory for bad items,
    // Cant place in /listeners for some reason.
    ////////////////////////////////////////////////////////////
    @EventHandler
    public void inventoryCheck2 (EntityPickupItemEvent e)
    {
        Entity entity = e.getEntity();
        if (entity instanceof Player)
        {
            Player player = (Player) e.getEntity();
            if (!player.hasPermission("youcanthavethat.bypass"))
            {
                Item item = e.getItem();
                PlayerInventory inventory = player.getInventory();
                ItemStack secondHand = inventory.getItemInOffHand();
                Material material = e.getItem().getItemStack().getType();

                if (blockedItems.contains(material)) e.setCancelled(true); item.remove(); checkInventoryProcess(inventory, secondHand);
            }
        }
    }

    private void checkInventoryProcess(PlayerInventory inventory, ItemStack secondHand)
    {
        if (!Global.configToggleStoragePurge) return;
        // Loop through contents and remove blocked items
        for (ItemStack item : inventory.getContents())
        {
            if (item != null && blockedItems.contains(item.getType()))
            {
                inventory.remove(item.getType()); // removes ALL stacks of that type

                // PatchFix - Item Removal happens so quickly that it forgets to remove the remaining stack.
                // This runs it again 2 seconds after the first time.
                getServer().getScheduler().runTaskLater(this, () -> inventory.remove(item.getType()), 40L); // 20 ticks = 1 second
            }
        }
        // Also checks offhand in case the fucker tried to be smart.
        if (blockedItems.contains(secondHand.getType())) inventory.setItemInOffHand(null);
    }

    @Override
    public void onDisable()
    {
        // Plugin shutdown logic
    }
}
