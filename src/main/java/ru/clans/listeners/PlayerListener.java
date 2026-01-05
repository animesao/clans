package ru.clans.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.CraftingInventory;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.models.QuestType;
import ru.clans.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final ClanPlugin plugin;
    private final Map<UUID, Location> lastLocations;
    private final Map<UUID, Double> travelAccumulator;

    public PlayerListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.lastLocations = new HashMap<>();
        this.travelAccumulator = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.setName(player.getName());
                member.setLastOnline(System.currentTimeMillis());
                plugin.getDataManager().saveClan(clan);
            }
            
            MessageUtil.sendClanMessage(clan, "&aУчастник &f" + player.getName() + " &aвошёл в игру!");
        }
        
        lastLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.setLastOnline(System.currentTimeMillis());
                plugin.getDataManager().saveClan(clan);
            }
            
            MessageUtil.sendClanMessage(clan, "&cУчастник &f" + player.getName() + " &cвышел из игры");
        }
        
        lastLocations.remove(player.getUniqueId());
        travelAccumulator.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
        Clan attackerClan = plugin.getClanManager().getPlayerClan(attacker.getUniqueId());
        
        if (victimClan != null && attackerClan != null && victimClan.getId().equals(attackerClan.getId())) {
            if (!victimClan.isPvpEnabled()) {
                event.setCancelled(true);
                MessageUtil.send(attacker, "&cPVP внутри клана выключен!");
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        Player killer = event.getEntity().getKiller();
        
        if (killer != null) {
            Clan clan = plugin.getClanManager().getPlayerClan(killer.getUniqueId());
            if (clan != null) {
                ClanMember member = clan.getMember(killer.getUniqueId());
                
                if (entity instanceof Player) {
                    Player victim = (Player) entity;
                    Clan victimClan = plugin.getClanManager().getPlayerClan(victim.getUniqueId());
                    
                    if (victimClan == null || !victimClan.getId().equals(clan.getId())) {
                        clan.addKill();
                        if (member != null) {
                            member.addKill();
                            member.addContribution(10);
                        }
                        
                        plugin.getQuestManager().progressQuest(clan, QuestType.KILL_PLAYERS, 1);
                        plugin.getLevelManager().addExperience(clan, 50, "Player kill");
                    }
                    
                    if (victimClan != null) {
                        victimClan.addDeath();
                        ClanMember victimMember = victimClan.getMember(victim.getUniqueId());
                        if (victimMember != null) {
                            victimMember.addDeath();
                        }
                        plugin.getDataManager().saveClan(victimClan);
                    }
                } else {
                    if (member != null) {
                        member.addContribution(1);
                    }
                    plugin.getQuestManager().progressQuest(clan, QuestType.KILL_MOBS, 1);
                    plugin.getLevelManager().addExperience(clan, 5, "Mob kill");
                }
                
                plugin.getDataManager().saveClan(clan);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            String blockType = event.getBlock().getType().name();
            
            if (blockType.contains("ORE")) {
                plugin.getQuestManager().progressQuest(clan, QuestType.MINE_ORES, 1);
                plugin.getLevelManager().addExperience(clan, 3, "Ore mining");
                
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null) {
                    member.addContribution(1);
                }
            }
            
            plugin.getQuestManager().progressQuest(clan, QuestType.MINE_BLOCKS, 1);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            plugin.getQuestManager().progressQuest(clan, QuestType.PLACE_BLOCKS, 1);
            plugin.getLevelManager().addExperience(clan, 1, "Block placing");
        }
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            
            if (clan != null) {
                plugin.getQuestManager().progressQuest(clan, QuestType.FISH, 1);
                plugin.getLevelManager().addExperience(clan, 5, "Fishing");
                
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null) {
                    member.addContribution(2);
                }
                
                plugin.getDataManager().saveClan(clan);
            }
        }
    }

    @EventHandler
    public void onCraftItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory() instanceof CraftingInventory)) return;
        
        if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null) {
            Player player = (Player) event.getWhoClicked();
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            
            if (clan != null) {
                int amount = event.getCurrentItem().getAmount();
                plugin.getQuestManager().progressQuest(clan, QuestType.CRAFT_ITEMS, amount);
                plugin.getLevelManager().addExperience(clan, 2, "Crafting");
            }
        }
    }

    @EventHandler
    public void onEntityBreed(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player)) return;
        
        Player player = (Player) event.getBreeder();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            plugin.getQuestManager().progressQuest(clan, QuestType.BREED_ANIMALS, 1);
            plugin.getLevelManager().addExperience(clan, 10, "Breeding");
            
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.addContribution(3);
            }
            
            plugin.getDataManager().saveClan(clan);
        }
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            plugin.getQuestManager().progressQuest(clan, QuestType.ENCHANT_ITEMS, 1);
            plugin.getLevelManager().addExperience(clan, 15, "Enchanting");
            
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.addContribution(5);
            }
            
            plugin.getDataManager().saveClan(clan);
        }
    }

    @EventHandler
    public void onVillagerTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory().getType() != InventoryType.MERCHANT) return;
        
        if (event.getSlotType() == InventoryType.SlotType.RESULT && event.getCurrentItem() != null) {
            Player player = (Player) event.getWhoClicked();
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            
            if (clan != null) {
                plugin.getQuestManager().progressQuest(clan, QuestType.TRADE_VILLAGERS, 1);
                plugin.getLevelManager().addExperience(clan, 8, "Trading");
                
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null) {
                    member.addContribution(2);
                }
                
                plugin.getDataManager().saveClan(clan);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            Location lastLoc = lastLocations.get(player.getUniqueId());
            if (lastLoc != null && lastLoc.getWorld().equals(event.getTo().getWorld())) {
                double distance = lastLoc.distance(event.getTo());
                
                if (distance > 0 && distance < 100) {
                    double accumulated = travelAccumulator.getOrDefault(player.getUniqueId(), 0.0) + distance;
                    int blocks = (int) accumulated;
                    
                    if (blocks > 0) {
                        plugin.getQuestManager().progressQuest(clan, QuestType.TRAVEL_DISTANCE, blocks);
                        accumulated -= blocks;
                    }
                    
                    travelAccumulator.put(player.getUniqueId(), accumulated);
                }
            }
            
            lastLocations.put(player.getUniqueId(), event.getTo().clone());
        }
    }

    @EventHandler
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        
        if (amount > 0) {
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            
            if (clan != null) {
                plugin.getQuestManager().progressQuest(clan, QuestType.COLLECT_EXPERIENCE, amount);
            }
        }
    }
}
